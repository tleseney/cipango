// ========================================================================
// Copyright 2012 NEXCOM Systems
// ------------------------------------------------------------------------
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at 
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ========================================================================
package org.cipango.server.session;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.servlet.http.HttpSession;
import javax.servlet.sip.Address;
import javax.servlet.sip.ServletTimer;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipApplicationSessionBindingEvent;
import javax.servlet.sip.SipApplicationSessionBindingListener;
import javax.servlet.sip.SipApplicationSessionEvent;
import javax.servlet.sip.SipApplicationSessionListener;
import javax.servlet.sip.SipErrorEvent;
import javax.servlet.sip.SipErrorListener;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipSessionEvent;
import javax.servlet.sip.SipSessionListener;
import javax.servlet.sip.TimerListener;
import javax.servlet.sip.UAMode;
import javax.servlet.sip.URI;

import org.cipango.server.SipMessage;
import org.cipango.server.SipRequest;
import org.cipango.server.session.SessionManager.AppSessionIf;
import org.cipango.server.sipapp.SipAppContext;
import org.cipango.util.TimerTask;
import org.eclipse.jetty.util.component.ContainerLifeCycle;
import org.eclipse.jetty.util.component.Dumpable;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class ApplicationSession implements SipApplicationSession, AppSessionIf, Dumpable
{
	private static final Logger LOG = Log.getLogger(ApplicationSession.class);
	
	private final String _id;
	
	// Use CopyOnWriteArrayList to ensure no issue on invalidateIfReady
	protected final List<Session> _sessions = new CopyOnWriteArrayList<Session>();
	protected List<?> _otherSessions;
	
	private final long _created;
	private long _accessed;
	
	private int _timeoutMs = 30000;
	
	private boolean _valid = true;
	
	protected Map<String, Object> _attributes;
	
	private final SessionManager _sessionManager;
	protected List<ServletTimer> _timers;
    
    protected boolean _invalidateWhenReady = true;
	
	protected static Method __noAck;
    protected static Method __noPrack;
    protected static Method __appSessionReadyToInvalidate;
    protected static Method __sessionCreated;
    protected static Method __sessionReadyToInvalidate;
    protected static Method __sessionDestroyed;
    
    static 
    {
        try 
        {
            __noAck = SipErrorListener.class.getMethod("noAckReceived", SipErrorEvent.class);
            __noPrack = SipErrorListener.class.getMethod("noPrackReceived", SipErrorEvent.class);
             __appSessionReadyToInvalidate = 
            	SipApplicationSessionListener.class.getMethod("sessionReadyToInvalidate", SipApplicationSessionEvent.class);
            __sessionCreated = SipSessionListener.class.getMethod("sessionCreated", SipSessionEvent.class);
            __sessionReadyToInvalidate = SipSessionListener.class.getMethod("sessionReadyToInvalidate", SipSessionEvent.class);
            __sessionDestroyed = SipSessionListener.class.getMethod("sessionDestroyed", SipSessionEvent.class);
        } 
        catch (NoSuchMethodException e)
        {
            throw new ExceptionInInitializerError(e);
        }
    }
	
	public ApplicationSession(SessionManager sessionManager, String id)
	{
		_sessionManager = sessionManager;
		
		_created = System.currentTimeMillis();
		_accessed = 0;
		
		_id = id;
		
		if (getContext().getSpecVersion() == SipAppContext.VERSION_10)
			_invalidateWhenReady = false;
	}
	
	public SessionManager getSessionManager()
	{
		return _sessionManager;
	}
	
	public Session createSession(SipRequest initial)
	{
		Session session = new Session(this, _sessionManager.newSessionId(), initial);
		addSession(session);
		return session;
	}
	
	public Session createUacSession(String callId, Address from, Address to)
	{
		Session session = new Session(this, _sessionManager.newSessionId(), callId, from, to);
	
		addSession(session);
		session.createUA(UAMode.UAC);
		return session;
	}
	
	public Session createDerivedSession(Session session)
	{
		if (session.appSession() != this)
			throw new IllegalArgumentException("SipSession " + session.getId()
					+ " does not belong to SipApplicationSession " + getId());

		Session derived = new Session(_sessionManager.newSessionId(), session);
		derived.setInvalidateWhenReady(_invalidateWhenReady);
		addSession(derived);
		return derived;
	}

	protected void addSession(Session session)
	{
		_sessions.add(session);
		List<SipSessionListener> listeners = getSessionManager().getSessionListeners();
		if (!listeners.isEmpty())
		{
			SipSessionEvent event = new SipSessionEvent((SipSession) session);
			getContext().fire(listeners, __sessionCreated, event);
		}
	}
	
	public String newCallId()
	{
		return _sessionManager.newCallId();
	}
	
	protected String newSessionId()
	{
		return _sessionManager.newSessionId();
	}
	
	public String newUASTag()
	{
		return _sessionManager.newUASTag(this);
	}
	
	public String newBranch()
	{
		return _sessionManager.newBranch();
	}
		
	protected void putAttribute(String name, Object value)
	{
		Object old = null;
		synchronized (this)
		{
			checkValid();
			old = doPutOrRemove(name, value);
		}
		if (value == null || !value.equals(old))
		{
			if (old != null)
				unbindValue(name, old);
			if (value != null)
				bindValue(name, value);
			
			_sessionManager.doApplicationSessionAttributeListeners(this, name, old, value);
		}
	}
	
	public void unbindValue(String name, Object value)
	{
		if (value != null && value instanceof SipApplicationSessionBindingListener)
			((SipApplicationSessionBindingListener) value).valueUnbound(new SipApplicationSessionBindingEvent(this, name));
	}
	
	public void bindValue(String name, Object value)
	{
		if (value != null && value instanceof SipApplicationSessionBindingListener)
			((SipApplicationSessionBindingListener) value).valueBound(new SipApplicationSessionBindingEvent(this, name));
	}
	
	protected Object doPutOrRemove(String name, Object value)
    {
		if (value == null)
		{	
			return _attributes != null ? _attributes.remove(name) : null;
		}
		else
		{
			if (_attributes == null)
				_attributes = new HashMap<String, Object>();
			return _attributes.put(name, value);
		}
    }
	
	public void removeSession(Object session)
	{
		if (session instanceof SipSession)
		{
			_sessions.remove((SipSession) session);
			
			List<SipSessionListener> listeners = getSessionManager().getSessionListeners();
			if (!listeners.isEmpty())
			{
				SipSessionEvent event = new SipSessionEvent((SipSession) session);
				getContext().fire(listeners, __sessionDestroyed, event);
			}
		}
		else if (_otherSessions != null)
		{
			_otherSessions.remove(session);
		}
	}
	
	public int getTimeoutMs()
	{
		return _timeoutMs;
	}
	
	public SipAppContext getContext()
	{
		return _sessionManager.getSipAppContext();
	}
	
	/**
	 * @see SipApplicationSession#getCreationTime()
	 */
	public long getCreationTime() 
	{
		checkValid();
		return _created;
	}
	
	protected void accessed()
	{
		_accessed = System.currentTimeMillis();
	}

	@Override
	public long getExpirationTime() 
	{
		checkValid();
		
		if (_timeoutMs == 0)
			return 0;
		
		long expirationTime = _accessed + _timeoutMs;
				
		if (expirationTime < System.currentTimeMillis())
			return Long.MIN_VALUE;
		
		return expirationTime;
		/*
		if (_expiryTimer == null)
			return 0;
		long expirationTime = _expiryTimer.getExecutionTime();
		if (expirationTime <= System.currentTimeMillis())
			return Long.MIN_VALUE;
		else
			return expirationTime;
			*/
	}

	/**
	 * @see SipApplicationSession#getLastAccessedTime()
	 */
	public long getLastAccessedTime() 
	{
		return _accessed;
	}

	/**
	 * @see SipApplicationSession#setExpires(int)
	 */
	public int setExpires(int deltaMinutes) 
	{
		checkValid();
		
		if (deltaMinutes < 0)
			deltaMinutes = 0;
		
		_timeoutMs = deltaMinutes * 60 * 1000;
		
		if (deltaMinutes == 0)
			return Integer.MAX_VALUE;
		
		return deltaMinutes;
	}
	
	/**
	 * @see SipApplicationSession#invalidate()
	 */
	public void invalidate()
	{
		checkValid();

		if (LOG.isDebugEnabled())
			LOG.debug("invalidating SipApplicationSession: " + this);

		synchronized (this)
		{
			_sessionManager.removeApplicationSession(this);
			try
			{
				while (_attributes != null && _attributes.size() > 0)
				{
					ArrayList<String> keys = new ArrayList<String>(_attributes.keySet());
					for (String key : keys)
						removeAttribute(key);
				}
			}
			finally
			{
				_valid = false;
			}
		}
	}

	private void checkValid()
	{
		if (!_valid)
			throw new IllegalStateException("SipApplicationSession has been invalidated");
	}
		
	@Override
	public void encodeURI(URI uri) 
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public URL encodeURL(URL url) 
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getApplicationName()
	{
		if (getContext() == null)
			return null;
		return getContext().getName();
	}

	public synchronized Object getAttribute(String name) 
	{
		checkValid();
		return _attributes != null ? _attributes.get(name) : null;
	}

	public Iterator<String> getAttributeNames() 
	{
		checkValid();
		
		if (_attributes == null)
			return Collections.emptyIterator();
		
		return new ArrayList<String>(_attributes.keySet()).iterator();
	}


	@Override
	public String getId() 
	{
		return _id;
	}

	@Override
	public boolean getInvalidateWhenReady() 
	{
		checkValid();
		return _invalidateWhenReady;
	}

	

	@Override
	public Object getSession(String id, Protocol protocol) 
	{
		checkValid();
		
		if (id == null || protocol == null)
			throw new NullPointerException((id == null) ? "null id" : "null protocol");
		
		if (protocol == Protocol.SIP)
		{
			for (Session session : _sessions)
			{
				if (session.getId().equals(id))
					return session;
			}
		}
		else if (protocol == Protocol.HTTP)
		{
			if (_otherSessions != null)
			{
				for (Object session : _otherSessions)
				{
					if (session instanceof HttpSession && ((HttpSession) session).getId().equals(id))
						return session;
				}
			}
		}
		return null;
	}

	@Override
	public Iterator<?> getSessions() 
	{
		checkValid();
		
		List<Object> list = new ArrayList<Object>(_sessions);
		if (_otherSessions != null)
			list.addAll(_otherSessions);
		
		return list.iterator();
	}

	@Override
	public Iterator<?> getSessions(String protocol) 
	{
		checkValid();
		
		if (protocol == null)
			throw new NullPointerException("null protocol");
		
		if ("sip".equalsIgnoreCase(protocol))
			return new ArrayList<SipSession>(_sessions).iterator();
		
		if ("http".equalsIgnoreCase(protocol))
		{
			if (_otherSessions == null)
				return Collections.emptyIterator();
			List<HttpSession> sessions = new ArrayList<HttpSession>();
			for (Object session : _otherSessions)
			{
				if (session instanceof HttpSession)
					sessions.add((HttpSession) session);
			}
			return sessions.iterator();
		}
		throw new IllegalArgumentException("Unknown protocol " + protocol);
	}

	@Override
	public SipSession getSipSession(String id)
	{
		checkValid();
		
		for (Session session : _sessions)
		{
			if (session.getId().equals(id))
				return session;
		}
		return null;
	}

	@Override
	public ServletTimer getTimer(String id)
	{
		checkValid();
		
		if (_timers != null)
		{
			for (ServletTimer timer : _timers)
				if (timer.getId().equals(id))
					return timer;
		}
		return null;
	}

	@Override
	public Collection<ServletTimer> getTimers() 
	{
		checkValid();
		if (_timers == null)
			return Collections.emptyList();
		
		return new ArrayList<ServletTimer>(_timers);
	}

	
	@Override
	public boolean isReadyToInvalidate()
	{
		checkValid();
		
		if (_accessed == 0)
			return false;
		
		for (int i = 0; i < _sessions.size(); i++)
		{
			Session session = _sessions.get(i);
			if (!session.isReadyToInvalidate())
				return false;
		}
		
		if (_otherSessions != null && !_otherSessions.isEmpty())
			return false;
		
		return (_timers == null || _timers.isEmpty());
	}

	/**
	 * @see SipApplicationSession#isValid()
	 */
	public boolean isValid() 
	{
		return _valid;
	}

	/**
	 * @see SipApplicationSession#removeAttribute(String)
	 */
	public void removeAttribute(String name) 
	{
		putAttribute(name, null);
	}

	/**
	 * @see SipApplicationSession#setAttribute(String, Object)
	 */
	public void setAttribute(String name, Object value) 
	{
		if (name == null || value == null)
			throw new NullPointerException("Name or attribute is null");
		
		putAttribute(name, value);
	}

	@Override
	public void setInvalidateWhenReady(boolean invalidateWhenReady) 
	{
		checkValid();
		_invalidateWhenReady = invalidateWhenReady;
		
	}
	
	@Override
	public String toString()
    {
    	return _id + "/" + getApplicationName() + "(" + _sessions.size() + ")";
    }
	
	@Override
	public ApplicationSession getAppSession()
	{
		return this;
	}
	
    private void addTimer(Timer timer)
    {
    	if (_timers == null)
    		_timers = new ArrayList<ServletTimer>(1);
    	
    	_timers.add(timer);
    }
    
    private void removeTimer(Timer timer)
    {
    	if (_timers != null)
    		_timers.remove(timer);
    }
    
	public synchronized Session getSession(SipMessage message)
	{
		String ftag = message.from().getTag();
		String ttag = message.to().getTag();
		String callId = message.getCallId();
		
		for (int i = 0; i < _sessions.size(); i++)
		{
			Session session = _sessions.get(i);
			if (session.isDialog(callId, ftag, ttag))
				return session;
		}
		return null;
	}
	
	public void invalidateIfReady()
	{
		boolean invalidateSessionsWhenReady = true;
		
		// Could use iterator as list is CopyOnWriteArrayList
		for (Session session : _sessions)
		{
			if (session.getInvalidateWhenReady())
				session.invalidateIfReady();
			else
				invalidateSessionsWhenReady = false;
		}
		
		if (isValid() && getInvalidateWhenReady() && invalidateSessionsWhenReady && isReadyToInvalidate())
		{
			List<SipApplicationSessionListener> listeners = getSessionManager().getApplicationSessionListeners();
			if (!listeners.isEmpty())
				getContext().fire(listeners, __appSessionReadyToInvalidate, new SipApplicationSessionEvent(this));
			
			if (getInvalidateWhenReady() && isValid())
				invalidate();
		}
	}
	
	/**
	 * Returns the derived sessions for session 
	 * The session session is included in the list.
	 */
	public List<Session> getDerivedSessions(Session session) 
	{
		String tag = session.getLocalTag();
		String callID = session.getCallId();

		List<Session> list = new ArrayList<Session>(1);
		for (Session sipSession : _sessions)
		{
			if (tag.equals(sipSession.getLocalTag())
					&& callID.equals(sipSession.getCallId()))
				list.add(sipSession);
		}
		return list;
	}
	

	@Override
	public String dump()
	{
		return ContainerLifeCycle.dump(this);
	}

	@Override
	public void dump(Appendable out, String indent) throws IOException
	{
		out.append(indent).append("+ ").append(getId()).append('\n');
		indent += "  ";
		printAttr(out, "created", new Date(getCreationTime()), indent);
		printAttr(out, "accessed", new Date(getLastAccessedTime()), indent);
		printAttr(out, "expirationTime", new Date(getExpirationTime()), indent);
		printAttr(out, "context", getContext().getName(), indent);
		printAttr(out, "invalidateWhenReady", getInvalidateWhenReady(), indent);
		printAttr(out, "attributes", _attributes, indent);
		
		if (_timers != null && !_timers.isEmpty())
		{
			Iterator<ServletTimer> it4 = _timers.iterator();
			out.append(indent).append("+ [Timers]\n");
			while (it4.hasNext())
				out.append(indent).append("  + ").append(it4.next().toString()).append('\n');
		}
		
		Iterator<Session> it = _sessions.iterator();
		if (it.hasNext())
			out.append(indent).append("+ [sipSessions]\n");
		while (it.hasNext())
			it.next().dump(out, indent + "  ");
	}
	
	private void printAttr(Appendable out, String name, Object value, String indent) throws IOException
	{
		out.append(indent).append("- ").append(name).append(": ").append(String.valueOf(value)).append('\n');
	}
	
    public class Timer implements ServletTimer, Runnable
    {
		private Serializable _info;
        private long _period;
        private TimerTask _timerTask;
        private boolean _persistent;
        private final String _id;
        
        public Timer(long delay, boolean persistent, Serializable info)
        {
        	this(delay, -1, false, persistent, info);
        }
        
        public Timer(long delay, long period, boolean fixedDelay, boolean isPersistent, Serializable info)
        {
        	this(delay, period, fixedDelay, isPersistent, info, getSessionManager().newTimerId());
        }
        
        public Timer(long delay, long period, boolean fixedDelay, boolean isPersistent, Serializable info, String id)
        {
        	checkValid();
            addTimer(this);
            _info = info;
            _period = period;
            _timerTask = getSessionManager().schedule(this, delay);
            _persistent = isPersistent;
            _id = id;
        }
        
        public SipApplicationSession getApplicationSession()
        {
            return ApplicationSession.this;
        }

        public Serializable getInfo()
        {
            return _info;
        }

        public long scheduledExecutionTime()
        {
            return _timerTask.getExecutionTime();
        }

        public String getId()
		{
			return _id;
		}

		public long getTimeRemaining()
		{
			return _timerTask.getExecutionTime() - System.currentTimeMillis();
		}
		
		public long getPeriod()
		{
			return _period;
		}
		
		public boolean isPersistent()
		{
			return _persistent;
		}
		
        public void cancel()
        {
        	_timerTask.cancel();
        	removeTimer(this);
           _period = -1;
        }
        
        @Override
        public String toString()
        {
        	long remaining = getTimeRemaining();
        	
        	return "ServletTimer {" + _info + "}@" + ((Math.abs(remaining) > 2000) ? remaining/1000 + "s" : remaining + "ms"); 
        }
        
        public void run()
        {
        	// Do not change the class loader as it has been already done in the timer thread start (See SessionManager.Timer).
        	List<TimerListener> listeners = getContext().getTimerListeners();
        	
        	if (!listeners.isEmpty())
        	{
        		for (TimerListener l :listeners)
        		{
        			try
        			{
        				l.timeout(this);
        			}
        			catch (Throwable t)
        			{
        				LOG.debug("Failed to invoke listener " + l, t);
        			}
        		}
        	}
        	else
        		LOG.warn("The timer {} has been created by application {} but no timer listeners defined",
        				toString(), getApplicationName());

        	if (_period != -1)
            {
                _timerTask = getSessionManager().schedule(this, _period);
            }
            else
            {
               removeTimer(this);
            }
        }
    }

}
