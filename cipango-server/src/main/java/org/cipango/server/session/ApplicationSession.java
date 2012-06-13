package org.cipango.server.session;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.sip.ServletTimer;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipApplicationSessionBindingEvent;
import javax.servlet.sip.SipApplicationSessionBindingListener;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.URI;

import org.cipango.server.SipRequest;
import org.cipango.util.TimerTask;

public class ApplicationSession implements SipApplicationSession
{
	private String _id;
	
	private List<Session> _sessions = new ArrayList<Session>(1);
	
	private final long _created;
	private long _accessed;
	
	private int _timeoutMs = 30000;
	
	private boolean _valid = true;
	private TimerTask _expiryTimer;
	
	private Map<String, Object> _attributes;
	
	private SessionManager _sessionManager;
	
	public ApplicationSession(SessionManager sessionManager, String id)
	{
		_sessionManager = sessionManager;
		
		_created = System.currentTimeMillis();
		_accessed = _created;
		
		_id = id;
	}
	
	public SessionManager getSessionManager()
	{
		return _sessionManager;
	}
	
	public Session createSession(SipRequest initial)
	{
		Session session = new Session(this, "sipsessionid", initial);
		_sessions.add(session);
		return session;
	}
	
	public Session getSession(SipRequest request)
	{
		return _sessions.get(0);
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
				unbindValue(name, value);
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
	
	public int getTimeoutMs()
	{
		return _timeoutMs;
		
	}
	/**
	 * @see SipApplicationSession#getCreationTime()
	 */
	public long getCreationTime() 
	{
		checkValid();
		return _created;
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
		if (deltaMinutes < 0)
			deltaMinutes = 0;
		
		_timeoutMs = deltaMinutes * 60 * 1000;
		
		if (deltaMinutes == 0)
			return Integer.MAX_VALUE;
		
		return deltaMinutes;
		
		/*
		if (_expiryTimer != null)
		{
			//_callSession.cancel(_expiryTimer);
			_expiryTimer = null;
		}
		
		_expiryDelay = deltaMinutes;
		
		if (_expiryDelay > 0)
		{
			//_expiryTimer = _callSession.schedule(new ExpiryTimeout(), _expiryDelay * 60000l);
			return _expiryDelay;
		}
		return Integer.MAX_VALUE;
		*/
	}
	
	/**
	 * @see SipApplicationSession#invalidate()
	 */
	public void invalidate() 
	{
		_sessionManager.removeApplicationSession(this);
		try
		{
			if (_expiryTimer != null)
			{
				//_callSession.cancel(_expiryTimer);
				_expiryTimer = null;
			}
			//_callSession.removeApplicationSession(this);
		}
		finally
		{
			_valid = false;
		}
		
	}

	private void checkValid()
	{
		if (!_valid)
			throw new IllegalStateException("SipApplicationSession has been invalidated");
	}
	
	private void expired()
	{
		if (_valid)
		{
			// TODO call listeners
			if (_valid)
			{
				if (getExpirationTime() == Long.MIN_VALUE)
					invalidate();
			}
		}
	}
	
	@Override
	public void encodeURI(URI arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public URL encodeURL(URL arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getApplicationName() {
		// TODO Auto-generated method stub
		return null;
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
	public boolean getInvalidateWhenReady() {
		// TODO Auto-generated method stub
		return false;
	}

	

	@Override
	public Object getSession(String arg0, Protocol arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterator<?> getSessions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterator<?> getSessions(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SipSession getSipSession(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ServletTimer getTimer(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<ServletTimer> getTimers() {
		// TODO Auto-generated method stub
		return null;
	}

	
	@Override
	public boolean isReadyToInvalidate() {
		// TODO Auto-generated method stub
		return false;
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
			throw new IllegalArgumentException("Name or attribute is null");
		
		putAttribute(name, value);
	}

	@Override
	public void setInvalidateWhenReady(boolean arg0) {
		// TODO Auto-generated method stub
		
	}

	class ExpiryTimeout implements Runnable
	{
		public void run() { expired(); }
	}
	
}
