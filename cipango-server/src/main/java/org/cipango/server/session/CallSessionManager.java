package org.cipango.server.session;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.cipango.server.SipRequest;
import org.cipango.server.SipServer;
import org.cipango.server.transaction.ServerTransaction;
import org.cipango.server.transaction.Transaction;
import org.cipango.util.TimerQueue;
import org.cipango.util.TimerTask;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class CallSessionManager extends AbstractLifeCycle
{
	private final Logger LOG = Log.getLogger(CallSessionManager.class);
	
	private HashMap<String, CSession> _sessions = new HashMap<String, CallSessionManager.CSession>();
	private TimerQueue<CSession> _schedulerQueue = new TimerQueue<CSession>(1024);
	
	private Thread _scheduler;
	private SipServer _server;
	
	@Override
	public void doStart() throws Exception
	{
		new Thread(new Scheduler()).start();
		new Thread(new Watcher()).start();
		super.doStart();
	}
	
	public void setServer(SipServer server)
	{
		_server = server;
	}
	
	public SipServer getServer()
	{
		return _server;
	}
	
	public SessionScope openScope(String id)
	{
		synchronized (_sessions)
		{
			CSession session = _sessions.get(id);
			if (session == null)
			{
				session = newSession(id);
				_sessions.put(id, session);
				/*CSession newSession = newSession(id);
				session = _sessions.putIfAbsent(id, newSession);
				if (session == null)
					session = newSession;*/
			}
			return new SessionScope(session._lock.tryLock() ? session : null);
		}
	}
	
	protected CSession newSession(String id)
	{
		return new CSession(id);
	}
	
	protected void close(CSession session)
	{
		try
		{
			int holds = session._lock.getHoldCount();
			if (holds == 1)
			{
				long time = session.nextExecutionTime();
				
				if (time > 0)
				{
					while (time < System.currentTimeMillis())
					{
						session.runTimer();
						time = session.nextExecutionTime();
						
						if (time < 0)
							break;
					}
					
					if (time > 0)
					{
						synchronized(_schedulerQueue)
						{
							_schedulerQueue.offer(session, time);
							_schedulerQueue.notifyAll();
						}
					}
				}
				if (session.isDone())
				{
					synchronized (_sessions)
					{
						_sessions.remove(session._id);
					}
				}
				
				if (LOG.isDebugEnabled())
					LOG.debug("closed scope for {}", session);
				
				LOG.info("closed scope for {}", session);
			}
		}
		finally
		{
			session._lock.unlock();
		}
	}
	
	private void runTimers(CSession session)
	{
		boolean locked = session._lock.tryLock();
		
		if (locked)
		{
			try
			{
				session.runTimer();
			}
			finally
			{
				close(session);
			}
		}
		else
		{
			synchronized(_schedulerQueue)
			{
				_schedulerQueue.offer(session, System.currentTimeMillis() + 100);
				_schedulerQueue.notifyAll();
			}
		}
	}
	
	class CSession extends TimerQueue.Node implements CallSession
	{
		protected final String _id;
		protected final long _created;
		
		protected final List<Transaction> _transactions = new ArrayList<Transaction>(2);
		protected final List<ApplicationSession> _applicationSessions = new ArrayList<ApplicationSession>(1);
		protected final PriorityQueue<TimerTask> _timers = new PriorityQueue<TimerTask>();
		
		private ReentrantLock _lock = new ReentrantLock();
		
		public CSession(String id)
		{
			_id = id;
			_created = System.currentTimeMillis();
		}
		
		public SipServer getServer()
		{
			return CallSessionManager.this.getServer();
		}
		
		public String getID()
		{
			return _id;
		}
		
		public long getCreationTime()
		{
			return _created;
		}
		
		public TimerTask schedule(Runnable runnable, long delay)
		{
			TimerTask timer = new TimerTask(runnable, System.currentTimeMillis() + delay);
			_timers.offer(timer);
			
			if (LOG.isDebugEnabled())
				LOG.debug("scheduled timer {}", timer);
			
			return timer;
		}
		
		public void cancel(TimerTask task)
		{
			if (task != null)
			{
				task.cancel();
				_timers.remove(task);
			}
		}
		
		protected long nextExecutionTime()
		{
			return _timers.isEmpty() ? -1 : _timers.peek().getExecutionTime();
		}
		
		protected TimerTask pollTimerIfExpired(long time)
		{
			if (!_timers.isEmpty())
			{
				if (_timers.peek().getExecutionTime() <= time)
					return _timers.poll();
			}
			return null;
		}
		
		protected void runTimer()
		{
			long now = System.currentTimeMillis();
			
			TimerTask timer = null;
			
			while ((timer = pollTimerIfExpired(now)) != null)
			{
				if (!timer.isCancelled())
				{
					try
					{
						timer.getRunnable().run();
					}
					catch (Throwable t)
					{
						LOG.debug(t);
					}
				}
			}
		}
		
		public Transaction createTransaction(SipRequest request)
		{
			ServerTransaction transaction = new ServerTransaction(request);
			_transactions.add(transaction);
			return transaction;
		}
		
		public Transaction getTransaction(String branch, boolean cancel, boolean server)
		{
			for (int i = 0; i < _transactions.size(); i++)
			{
				Transaction transaction = _transactions.get(i);
				if ((transaction.getBranch().equals(branch)) 
						&& transaction.isServer() == server
						&& transaction.isCancel() == cancel)
					return transaction;
			}
			return null;
		}
		
		public void removeTransaction(Transaction transaction)
		{
			_transactions.remove(transaction);
		}
		
		public ApplicationSession createApplicationSession()
		{
			ApplicationSession appSession = new ApplicationSession(); // TODO this
 			_applicationSessions.add(appSession);
			return appSession;
		}
		
		public void removeApplicationSession(ApplicationSession applicationSession)
		{
			
		}
		
		public boolean isDone()
		{
			return _transactions.isEmpty() && _timers.isEmpty();
		}
		
		@Override
		public String toString()
		{
			return String.format("%s{%s,txs=%s}", getClass().getSimpleName(), _id, _transactions);
		}
	}
	
	public class SessionScope
	{
		private CSession _session;
		
		public SessionScope(CSession session)
		{
			_session = session;
		}
		
		public CallSession getCallSession()
		{
			return _session;
		}
		
		public void close()
		{
			if (_session != null)
				CallSessionManager.this.close(_session);
		}
	}
	
	class Scheduler implements Runnable
	{
		public void run()
		{
			_scheduler = Thread.currentThread();
			String name = _scheduler.getName();
			_scheduler.setName("session-scheduler");
			
			try
			{
				do
				{
					try
					{
						CSession session;
						long timeout;
						
						synchronized (_schedulerQueue) 
						{
							session = _schedulerQueue.peek();
							timeout = session != null ? session.getValue() - System.currentTimeMillis() : Long.MAX_VALUE;
							
							if (timeout > 0)
							{
								_schedulerQueue.wait(timeout);
							}
							else
							{
								_schedulerQueue.poll();
							}
						}
						if (timeout <= 0)
						{
							runTimers(session);
						}
					}
					catch (InterruptedException e) { continue; }
					catch (Throwable t) { LOG.warn(t); }
				}
				while (isRunning());
			}
			finally
			{
				_scheduler.setName(name);
				_scheduler = null;
			}
		}
	}
	
	class Watcher implements Runnable
	{
		public void run()
		{
			while(isRunning())
			{
				try { Thread.sleep(5000); } catch (Exception e) {}
				LOG.info("session size: " + _sessions.size());
			}
		}
	}
}
