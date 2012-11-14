package org.cipango.server.session;

import static java.lang.Math.round;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.EventListener;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.servlet.ServletContext;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipApplicationSessionAttributeListener;
import javax.servlet.sip.SipApplicationSessionBindingEvent;
import javax.servlet.sip.SipApplicationSessionEvent;
import javax.servlet.sip.SipApplicationSessionListener;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipSessionAttributeListener;
import javax.servlet.sip.SipSessionBindingEvent;
import javax.servlet.sip.SipSessionListener;

import org.cipango.server.sipapp.SipAppContext;
import org.cipango.sip.SipGrammar;
import org.cipango.util.StringUtil;
import org.cipango.util.TimerTask;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.annotation.ManagedOperation;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.statistic.CounterStatistic;
import org.eclipse.jetty.util.statistic.SampleStatistic;

@ManagedObject("Session manager")
public class SessionManager extends AbstractLifeCycle
{
	private static final Logger LOG = Log.getLogger(SessionManager.class);
	public static final char CONTEXT_ID_SEPARATOR = '.';
	
	protected static final Method __appSessionCreated;
    protected static final Method __appSessionDestroyed;
	
	private Random _random = new Random();
	private ConcurrentHashMap<String, ApplicationSession> _appSessions = new ConcurrentHashMap<String, ApplicationSession>();
	
	private final List<SipSessionAttributeListener> _sessionAttributeListeners = new CopyOnWriteArrayList<SipSessionAttributeListener>();
	private final List<SipApplicationSessionAttributeListener> _applicationSessionAttributeListeners = new CopyOnWriteArrayList<SipApplicationSessionAttributeListener>();	
	private final List<SipApplicationSessionListener> _applicationSessionListeners = new CopyOnWriteArrayList<SipApplicationSessionListener>();
	private final List<SipSessionListener> _sessionListeners = new CopyOnWriteArrayList<SipSessionListener>();
	
	private Timer _timer;
	private long _scavengePeriodMs = 30000;
	private TimerTask _task;
	
	protected ClassLoader _loader;
	private final SipAppContext _sipAppContext;
	private final String _localhost;
	private Queue<TimerTask> _timerQueue = new PriorityQueue<TimerTask>();

	private int _sessionTimeout = -1;
	
    private final CounterStatistic _sessionsStats = new CounterStatistic();
    private final SampleStatistic _sessionTimeStats = new SampleStatistic();
    	
	static
	{
		try
		{
			__appSessionCreated = SipApplicationSessionListener.class.getMethod("sessionCreated",
					SipApplicationSessionEvent.class);
			__appSessionDestroyed = SipApplicationSessionListener.class.getMethod("sessionDestroyed",
					SipApplicationSessionEvent.class);
		}
		catch (NoSuchMethodException e)
		{
			throw new ExceptionInInitializerError(e);
		}
	}
	
	public SessionManager(SipAppContext sipAppContext)
	{
		_sipAppContext = sipAppContext;
		String localhost;
		try
		{
			localhost = InetAddress.getLocalHost().getHostName();
		}
		catch (Exception e)
		{
			localhost = "localhost";
		}
		_localhost = localhost;
	}
	
	@Override
	protected void doStart() throws Exception
	{
		super.doStart();

		// Web app context could be null in some tests.
		if (_sipAppContext.getWebAppContext() != null)
			_loader = _sipAppContext.getWebAppContext().getClassLoader();
			
		_timer = new Timer();
		new Thread(_timer, "Timer-" + _sipAppContext.getName()).start();
		setScavengePeriod(getScavengePeriod());
	}
		
	public ServletContext getContext()
	{
		return _sipAppContext.getServletContext();
	}
	
	public ApplicationSession createApplicationSession()
	{
		ApplicationSession appSession;
		synchronized (this)
		{
			String id = null;
			while (id == null || appIdInUse(id))
			{
				id = newApplicationSessionId();
			}
			appSession = new ApplicationSession(this, id);
			_appSessions.put(id, appSession);
		}
		
		_sessionsStats.increment();
		appSession.setExpires(_sessionTimeout);
		if (!_applicationSessionListeners.isEmpty())
			getSipAppContext().fire(_applicationSessionListeners, __appSessionCreated,  new SipApplicationSessionEvent(appSession));

		return appSession;
	}
	
	public ApplicationSession getApplicationSession(String id)
	{
		return _appSessions.get(id);
	}
	
	protected String newApplicationSessionId()
	{
		long r = _random.nextInt();
		if (r<0)
			r = -r;
		return _sipAppContext.getContextId() + CONTEXT_ID_SEPARATOR + StringUtil.toBase62String2(r);
	}
	
	public boolean appIdInUse(String id)
	{
		synchronized (this)
		{
			return _appSessions.containsKey(id);
		}
	}
	
	public String newSessionId()
	{
		long r = _random.nextInt();
		if (r<0)
			r = -r;
		return StringUtil.toBase62String2(r);
	}
	
	public String newTimerId()
	{
		long r = _random.nextInt();
		if (r<0)
			r = -r;
		return StringUtil.toBase62String2(r);
	}
	
	public String newCallId()
	{
		long r = _random.nextInt();
		if (r<0)
			r = -r;
		return StringUtil.toBase62String2(r) + '@' + _localhost;
	}
	
	public String newUASTag(ApplicationSession session)
	{
		long r = _random.nextInt();
		if (r<0)
			r = -r;
		return session.getId() + "-" + StringUtil.toBase62String2(r);
	}
	
	public String newBranch()
	{
		long r = _random.nextInt();
		if (r<0)
			r = -r;
		return SipGrammar.MAGIC_COOKIE + StringUtil.toBase62String2(r);
	}
	
	public void removeApplicationSession(ApplicationSession session)
	{
		_appSessions.remove(session.getId());
		
		_sessionsStats.decrement();
		_sessionTimeStats.set(round((System.currentTimeMillis() - session.getCreationTime())/1000.0));
		
		if (!_applicationSessionListeners.isEmpty())
			getSipAppContext().fire(_applicationSessionListeners, __appSessionDestroyed,  new SipApplicationSessionEvent(session));
	}
	
	protected void scavenge()
	{
		if (!isRunning())
			return;
			
		try
		{
			for (ApplicationSession session : _appSessions.values())
			{
				if (session.isValid() && session.getExpirationTime() == Long.MIN_VALUE)
					doSessionExpired(session);
			}
		}
		catch (Exception e)
		{
			LOG.warn("Failed to scavenge application sessions", e);
		}
		//LOG.info("#applications: " + _appSessions.size());
	}
	
	protected void doSessionExpired(ApplicationSession applicationSession)
	{	
		// Do not change the class loader as it has been already done in the timer thread start.
		for (SipApplicationSessionListener l : _applicationSessionListeners)
		{
			try
			{
				l.sessionExpired(new SipApplicationSessionEvent(applicationSession));
			}
			catch (Throwable e)
			{
				LOG.debug("Got exception while invoking session SipApplicationSessionListener " + l, e);
			}
		}
		
		if (applicationSession.getExpirationTime() < 0)
			applicationSession.invalidate();
	}
	
	public void doSessionAttributeListeners(Session session, String name, Object old, Object value)
	{
		if (!_sessionAttributeListeners.isEmpty())
		{
			SipSessionBindingEvent event = new SipSessionBindingEvent(session, name);
			
			for (SipSessionAttributeListener l : _sessionAttributeListeners)
			{
				if (value == null)
					l.attributeRemoved(event);
				else if (old == null)
					l.attributeAdded(event);
				else
					l.attributeReplaced(event);
			}
		}
	}
	
	public void doApplicationSessionAttributeListeners(ApplicationSession applicationSession, String name, Object old, Object value)
	{
		if (!_applicationSessionAttributeListeners.isEmpty())
		{
			SipApplicationSessionBindingEvent event = new SipApplicationSessionBindingEvent(applicationSession, name);
			
			for (SipApplicationSessionAttributeListener l : _applicationSessionAttributeListeners)
			{
				if (value == null)
					l.attributeRemoved(event);
				else if (old == null)
					l.attributeAdded(event);
				else
					l.attributeReplaced(event);
			}
		}
	}
	
	public int getScavengePeriod()
	{
		return (int) (_scavengePeriodMs / 1000);
	}
	
	public void setScavengePeriod(int seconds)
	{
		if (seconds == 0)
			seconds = 60;
		
		long oldPeriods = _scavengePeriodMs;
		long period = seconds * 1000l;
		
		if (period > 60000)
			period = 60000;
		if (period < 1000)
			period = 1000;
		
		_scavengePeriodMs = period;
		
		if (_timer != null && (period != oldPeriods || _task == null))
		{
			synchronized (this)
			{
				if (_task != null)
					_task.cancel();
				
				Runnable runnable = new Runnable()
				{
					@Override
					public void run() 
					{
						scavenge();
						if (isRunning())
							_task = schedule(this, _scavengePeriodMs);
					}
				};
				
				_task = schedule(runnable, _scavengePeriodMs);
			}
		}
	}
	
	public TimerTask schedule(Runnable runnable, long delay)
	{
		TimerTask task = new TimerTask(runnable, System.currentTimeMillis() + delay);
		synchronized (_timerQueue)
		{
			_timerQueue.offer(task);
			_timerQueue.notifyAll();
		}
		return task;
	}

	public SipAppContext getSipAppContext()
	{
		return _sipAppContext;
	}

	public List<SipApplicationSessionListener> getApplicationSessionListeners()
	{
		return _applicationSessionListeners;
	}
	
    public void addEventListener(EventListener listener)
    {
        if (listener instanceof SipApplicationSessionAttributeListener)
        	_applicationSessionAttributeListeners.add((SipApplicationSessionAttributeListener) listener);
        if (listener instanceof SipApplicationSessionListener)
        	_applicationSessionListeners.add((SipApplicationSessionListener) listener);
        if (listener instanceof SipSessionAttributeListener)
        	_sessionAttributeListeners.add((SipSessionAttributeListener) listener);
        if (listener instanceof SipSessionListener)
        	_sessionListeners.add((SipSessionListener) listener);
    }
    
    public void removeEventListener(EventListener listener)
    {
    	if (listener instanceof SipApplicationSessionAttributeListener)
        	_applicationSessionAttributeListeners.remove((SipApplicationSessionAttributeListener) listener);
        if (listener instanceof SipApplicationSessionListener)
        	_applicationSessionListeners.remove((SipApplicationSessionListener) listener);
        if (listener instanceof SipSessionAttributeListener)
        	_sessionAttributeListeners.remove((SipSessionAttributeListener) listener);
        if (listener instanceof SipSessionListener)
        	_sessionListeners.remove((SipSessionListener) listener);
    }

    public void clearEventListeners()
    {
       _applicationSessionAttributeListeners.clear();
       _applicationSessionListeners.clear();
       _sessionAttributeListeners.clear();
       _sessionListeners.clear();
    }

	public List<SipSessionAttributeListener> getSessionAttributeListeners()
	{
		return _sessionAttributeListeners;
	}

	public List<SipApplicationSessionAttributeListener> getApplicationSessionAttributeListeners()
	{
		return _applicationSessionAttributeListeners;
	}

	public List<SipSessionListener> getSessionListeners()
	{
		return _sessionListeners;
	}
	
	@ManagedAttribute("Session timeout")
	public int getSessionTimeout()
	{
		return _sessionTimeout;
	}

	public void setSessionTimeout(int sessionTimeout)
	{
		_sessionTimeout = sessionTimeout;
	}
	
	@ManagedAttribute("Active application sessions")
	public long getSessions()
	{
		return _sessionsStats.getCurrent();
	}
	
	@ManagedAttribute("Total application sessions")
	public long getSessionsTotal()
	{
		return _sessionsStats.getTotal();
	}
	
	@ManagedAttribute("Max active application sessions")
	public long getSessionsMax()
	{
		return _sessionsStats.getMax();
	}
	
    /**
     * @return maximum amount of time session remained valid
     */
	@ManagedAttribute("Maximum amount of time session remained valid")
    public long getSessionTimeMax()
    {
        return _sessionTimeStats.getMax();
    }
    
    /**
     * Reset statistics values
     */
	@ManagedOperation("Stats reset")
    public void statsReset()
    {
    	_sessionsStats.reset(getSessions());
    	_sessionTimeStats.reset();
    }

    /* ------------------------------------------------------------ */
    /**
     * @return maximum amount of time session remained valid
     */
	@ManagedAttribute("maximum amount of time session remained valid")
    public long getSessionTimeTotal()
    {
        return _sessionTimeStats.getTotal();
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @return mean amount of time session remained valid
     */
	@ManagedAttribute("mean amount of time session remained valid")
    public double getSessionTimeMean()
    {
        return _sessionTimeStats.getMean();
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @return standard deviation of amount of time session remained valid
     */
	@ManagedAttribute("standard deviation of amount of time session remained valid")
    public double getSessionTimeStdDev()
    {
        return _sessionTimeStats.getStdDev();
    }
	
	class Timer implements Runnable
	{
		public void run()
		{
			ClassLoader oldClassLoader = null;
			Thread currentThread = null;
			if (_loader != null)
			{
				currentThread = Thread.currentThread();
				oldClassLoader = currentThread.getContextClassLoader();
				currentThread.setContextClassLoader(_loader);
			}
			else
				LOG.warn("Could not set right class loader for timer of context {}", getSipAppContext());
			
			TimerTask task;
			long delay;
			do
			{
				try
				{	
					synchronized (_timerQueue)
					{
						do
						{
							task = _timerQueue.peek();
						}
						while (task != null && task.isCancelled());
						
						delay = task != null ? task.getExecutionTime() - System.currentTimeMillis() : Long.MAX_VALUE;
						
						if (delay > 0)
							_timerQueue.wait(delay); 
						else
							_timerQueue.poll();
					}
					if (delay <= 0)
					{
						try
						{
							if (!task.isCancelled())
								task.getRunnable().run();
						}
						catch (Throwable e)
						{
							LOG.debug("Failed to execute timer " + task, e);
						}
					}
				}
				catch (InterruptedException e) { continue; }
			}
			while (isRunning());
			
			if (_loader != null)
				currentThread.setContextClassLoader(oldClassLoader);
			
		}
	}
		
	public static interface AppSessionIf extends SipApplicationSession
	{
		ApplicationSession getAppSession();
	}
	
	public interface SipSessionIf extends SipSession
	{
		Session getSession();
	}

}
