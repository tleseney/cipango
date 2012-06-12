package org.cipango.server.session;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.servlet.sip.ServletTimer;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.URI;

import org.cipango.server.SipRequest;
import org.cipango.util.TimerTask;

public class ApplicationSession implements SipApplicationSession
{
	private String _id;
	
	private List<Session> _sessions = new ArrayList<Session>(1);
	
	private long _created = System.currentTimeMillis();
	private long _accessed;
	private int _expiryDelay;
	
	private boolean _valid = true;
	private TimerTask _expiryTimer;
	
	private SessionManager _sessionManager;
	
	public ApplicationSession(String id)
	{
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
		if (_expiryTimer == null)
			return 0;
		long expirationTime = _expiryTimer.getExecutionTime();
		if (expirationTime <= System.currentTimeMillis())
			return Long.MIN_VALUE;
		else
			return expirationTime;
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
	}
	
	/**
	 * @see SipApplicationSession#invalidate()
	 */
	public void invalidate() 
	{
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

	@Override
	public Object getAttribute(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterator<String> getAttributeNames() {
		// TODO Auto-generated method stub
		return null;
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

	@Override
	public boolean isValid() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void removeAttribute(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setAttribute(String arg0, Object arg1) {
		// TODO Auto-generated method stub
		
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
