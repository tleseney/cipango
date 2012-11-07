package org.cipango.server.session;

import java.net.InetAddress;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.servlet.ServletContext;
import javax.servlet.sip.SipApplicationSessionAttributeListener;
import javax.servlet.sip.SipApplicationSessionBindingEvent;
import javax.servlet.sip.SipApplicationSessionEvent;
import javax.servlet.sip.SipApplicationSessionListener;
import javax.servlet.sip.SipSessionAttributeListener;
import javax.servlet.sip.SipSessionBindingEvent;

import org.cipango.server.sipapp.SipAppContext;
import org.cipango.util.StringUtil;
import org.eclipse.jetty.util.component.AbstractLifeCycle;

public class SessionManager extends AbstractLifeCycle
{
	private Random _random = new Random();
	private ConcurrentHashMap<String, ApplicationSession> _appSessions = new ConcurrentHashMap<String, ApplicationSession>();
	
	protected final List<SipSessionAttributeListener> _sessionAttributeListeners = new CopyOnWriteArrayList<SipSessionAttributeListener>();
	protected final List<SipApplicationSessionAttributeListener> _applicationSessionAttributeListeners = new CopyOnWriteArrayList<SipApplicationSessionAttributeListener>();
	
	protected final List<SipApplicationSessionListener> _applicationSessionListeners = new CopyOnWriteArrayList<SipApplicationSessionListener>();
	
	private Timer _timer;
	private long _scavengePeriodMs = 30000;
	private TimerTask _task;
	
	protected ClassLoader _loader;
	private final SipAppContext _sipAppContext;
	private final String _localhost;
	
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
		if (_sipAppContext != null && _sipAppContext.getWebAppContext() != null)
			_loader = _sipAppContext.getWebAppContext().getClassLoader();
		if (_timer == null)
		{
			_timer = new Timer("session-scavenger", true);
		}
		setScavengePeriod(getScavengePeriod());
	}
	
	public ServletContext getContext()
	{
		return _sipAppContext.getServletContext();
	}
	
	public ApplicationSession createApplicationSession()
	{
		ApplicationSession appSession = new ApplicationSession(this, newApplicationSessionId());
		_appSessions.put(appSession.getId(), appSession);
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
		return StringUtil.toBase62String2(r);
	}
	
	public String newSessionId()
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
		return "z9hG4bK" + StringUtil.toBase62String2(r);
	}
	
	public void removeApplicationSession(ApplicationSession session)
	{
		_appSessions.remove(session.getId());
	}
	
	protected void scavenge()
	{
		if (!isRunning())
			return;
				
		try
		{
			long now = System.currentTimeMillis();
			for (ApplicationSession session : _appSessions.values())
			{
				long expirationTime = session.getExpirationTime();
				
				if (expirationTime != 0 && expirationTime < now)
				{
					doSessionExpired(session);
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		//System.out.println("#applications: " + _appSessions.size());
	}
	
	protected void doSessionExpired(ApplicationSession applicationSession)
	{
		ClassLoader oldClassLoader = null;
		Thread currentThread = null;
		
		if (_loader != null)
		{
			currentThread = Thread.currentThread();
			oldClassLoader = currentThread.getContextClassLoader();
			currentThread.setContextClassLoader(_loader);
		}
			
		for (SipApplicationSessionListener l : _applicationSessionListeners)
		{
			l.sessionExpired(new SipApplicationSessionEvent(applicationSession));
		}
		
		if (applicationSession.getExpirationTime() < 0)
			applicationSession.invalidate();
		
		if (_loader != null)
			currentThread.setContextClassLoader(oldClassLoader);
		
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
				_task = new TimerTask() 
				{
					@Override
					public void run() 
					{
						scavenge();
					}
				};
				_timer.schedule(_task, _scavengePeriodMs, _scavengePeriodMs);
			}
		}
	}

	public SipAppContext getSipAppContext()
	{
		return _sipAppContext;
	}
}
