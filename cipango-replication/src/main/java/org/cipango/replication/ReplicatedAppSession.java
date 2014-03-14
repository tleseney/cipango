// ========================================================================
// Copyright 2006-2013 NEXCOM Systems
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
package org.cipango.replication;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.sip.Address;
import javax.servlet.sip.ServletTimer;
import javax.servlet.sip.SipApplicationSessionActivationListener;
import javax.servlet.sip.SipApplicationSessionEvent;
import javax.servlet.sip.UAMode;

import org.cipango.server.SipRequest;
import org.cipango.server.session.ApplicationSession;
import org.cipango.server.session.Session;
import org.cipango.server.session.SessionManager;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.infinispan.commons.marshall.jboss.GenericJBossMarshaller;
import org.infinispan.marshall.core.MarshalledValue;

public class ReplicatedAppSession extends ApplicationSession implements Serializable
{
	public static final String CREATED = "created";
	public static final String ATTRIBUTES = "attributes";
	public static final String ACCESSED = "accessed";
	public static final String TIMEOUT = "timeout";
	public static final String INVALIDATE_WHEN_READY = "invalidateWhenReady";
	
	// Timer constant
	public static final String INFO = "info";
	public static final String PERIOD = "period";
	public static final String EXCUTION_TIME = "executionTime";
	public static final String FIXED_DELAY = "fixedDelay";
	
	private final static Logger __log = Log.getLogger(ReplicatedAppSession.class);
	
	private byte[] _serializeAttributes;	

	public ReplicatedAppSession(SessionManager sessionManager, String id)
	{
		super(sessionManager, id);
	}
	
	public ReplicatedAppSession(SessionManager sessionManager, String id, Map<String, Object> data)
	{
		super(sessionManager, id, (Long) data.get(CREATED),  (Long) data.get(ACCESSED));
		_serializeAttributes = (byte[]) data.get(ATTRIBUTES);
		_invalidateWhenReady = getBoolean(data, INVALIDATE_WHEN_READY, false);
		setExpires((int) (((Long) data.get(TIMEOUT))/60000));

//		long delayMs = getExpirationTime() - System.currentTimeMillis();
//		if (delayMs < 0)
//			throw new SessionExpiredException(id, -delayMs);
		
	}
	
	@Override
	public Session createSession(SipRequest initial)
	{
		Session session = new ReplicatedSession(this, getSessionManager().newSessionId(), initial);
		addSession(session);
		return session;
	}
	
	@Override
	public Session createUacSession(String callId, Address from, Address to)
	{
		Session session = new ReplicatedSession(this, getSessionManager().newSessionId(), callId, from, to);
	
		addSession(session);
		session.createUA(UAMode.UAC);
		return session;
	}
	
	@Override
	public Session createDerivedSession(Session session)
	{
		if (session.appSession() != this)
			throw new IllegalArgumentException("SipSession " + session.getId()
					+ " does not belong to SipApplicationSession " + getId());

		Session derived = new ReplicatedSession(getSessionManager().newSessionId(), session);
		derived.setInvalidateWhenReady(_invalidateWhenReady);
		addSession(derived);
		return derived;
	}
	
	public Map<String, Object> getData() throws IOException
	{
		Map<String, Object> data = new HashMap<String, Object>();
		data.put(CREATED, getCreationTime());
		data.put(ACCESSED, getLastAccessedTime());
		if( _attributes != null && !_attributes.isEmpty())
			data.put(ATTRIBUTES, Serializer.serialize(_attributes));
		data.put(TIMEOUT, getTimeoutMs());
		
		if (_invalidateWhenReady)
			data.put(INVALIDATE_WHEN_READY, _invalidateWhenReady);

		return data;
	}
	
	private boolean getBoolean(Map<String, Object> data, String property, boolean defaultValue)
	{
		Boolean value = (Boolean) data.get(property);
		if (value == null)
			return defaultValue;
		return value;
	}
	
	@SuppressWarnings("unchecked")
	public void deserializeIfNeeded()
	{
		SessionManager old = ProxyAppSession.getSessionManager();
		ClassLoader ocl = Thread.currentThread().getContextClassLoader();
		try
		{
			Thread.currentThread().setContextClassLoader(getContext().getClassLoader());
			ProxyAppSession.setSessionManager(getSessionManager());
			if (_serializeAttributes != null) 
			{
				byte[] serializeAttributes = _serializeAttributes;
				_serializeAttributes = null;
				_attributes = (Map<String, Object>) Serializer.deserialize(serializeAttributes);
				notifyActivationListener(true);
			}
			
			for (Session session : _sessions)
				((ReplicatedSession) session).deserializeIfNeeded();
			
		}
		catch (Exception e)
		{
			__log.warn("Failed to deserialize attributes", e);
		}
		finally
		{
			Thread.currentThread().setContextClassLoader(ocl);
			ProxyAppSession.setSessionManager(old);
		}
	}
	
	private Object writeReplace() throws ObjectStreamException
	{
		return new ProxyAppSession(getId());
	}
	
	public void notifyActivationListener(boolean activate)
	{
		if (_attributes == null)
			return;
		
		Iterator<Object> it = _attributes.values().iterator();
		while (it.hasNext())
		{
			Object value = (Object) it.next();
			if (value instanceof SipApplicationSessionActivationListener)
			{
				if (activate)
					((SipApplicationSessionActivationListener) value).sessionDidActivate(new SipApplicationSessionEvent(this));	
				else
					((SipApplicationSessionActivationListener) value).sessionWillPassivate(new SipApplicationSessionEvent(this));	
			}
		}
	}
	
	@Override
	public Timer newTimer(long delay, boolean isPersistent, Serializable info) {
		return new ReplicatedTimer(this, delay, -1, false, isPersistent, info);
	}

	@Override
	public Timer newTimer(long delay, long period, boolean fixedDelay, boolean isPersistent,
			Serializable info) {
		return new ReplicatedTimer(this, delay, period, fixedDelay, isPersistent, info);
	}
	
	public void newTimer(Map<String, Object> timerData, String timerId) throws IOException, ClassNotFoundException
	{
		long executionTime = (Long) timerData.get(EXCUTION_TIME);
		long period = (Long) timerData.get(PERIOD);
		boolean fixedDelay = getBoolean(timerData, FIXED_DELAY, false);
		byte[] bInfo = (byte[]) timerData.get(INFO);
		Serializable info = null;
		if (bInfo != null)
			info = (Serializable) Serializer.deserialize(bInfo);
		
		long delay = executionTime - System.currentTimeMillis();
		if (delay < 0)
			delay = 100l;
		new Timer(this, delay, period, fixedDelay, true, info, timerId);
	}
	
	public Map<String, Object> getTimerData(Timer timer) throws IOException
	{
		Map<String, Object> timerData = new HashMap<String, Object>();
		timerData.put(EXCUTION_TIME, timer.getTimeRemaining() + System.currentTimeMillis());
		timerData.put(PERIOD, timer.getPeriod());
		timerData.put(FIXED_DELAY, false);
		if (timer.getInfo() != null)
			timerData.put(INFO, Serializer.serialize(timer.getInfo()));
		
		return timerData;
	}
	
	
	public static class ReplicatedTimer extends ApplicationSession.Timer implements Serializable
	{
		public ReplicatedTimer(ApplicationSession session, long delay, long period,
				boolean fixedDelay, boolean isPersistent, Serializable info) {
			super(session, delay, period, fixedDelay, isPersistent, info);
		}
		
		public ReplicatedTimer(ApplicationSession session, long delay, long period, boolean fixedDelay, boolean isPersistent, Serializable info, String id)
		{
			super(session, delay, period, fixedDelay, isPersistent, info, id);
		}
		
		private Object writeReplace() throws ObjectStreamException
		{
			return new ProxyTimer(getApplicationSession().getId(), getId());
		}
	}
	
	public static class ProxyAppSession implements Serializable
	{
		private static final long serialVersionUID = 1L;
		
		private static ThreadLocal<SessionManager> __sessionManager = new ThreadLocal<>();
		private String _id; 
		
		public ProxyAppSession()
		{
		}
		
		public ProxyAppSession(String id)
		{
			_id = id;
		}
		
		private Object readResolve() throws ObjectStreamException 
		{
			SessionManager sessionManager = __sessionManager.get();
			if (sessionManager == null)
			{
				__log.warn("Could not get session manager in local thread");
				return null;
			}
			
			ApplicationSession applicationSession = sessionManager.getApplicationSession(_id);
			if (applicationSession == null)
				__log.warn("Could not load session with ID " + _id);
			return applicationSession;
		}

		public static void setSessionManager(SessionManager sessionManager)
		{
			__sessionManager.set(sessionManager);
		}

		public static SessionManager getSessionManager()
		{
			return __sessionManager.get();
		}
	}
	
	public static class ProxyTimer implements Serializable
	{
		private static final long serialVersionUID = 1L;
		private String _appSessionId; 
		private String _id; 
		
		public ProxyTimer()
		{
		}
		
		public ProxyTimer(String appSessionId, String id)
		{
			_appSessionId = appSessionId;
			_id = id;
		}
		
		private Object readResolve() throws ObjectStreamException 
		{
			SessionManager sessionManager = ProxyAppSession.getSessionManager();
			if (sessionManager == null)
			{
				__log.warn("Could not get session manager in local thread");
				return null;
			}
			
			ApplicationSession applicationSession = sessionManager.getApplicationSession(_appSessionId);
			if (applicationSession == null)
			{
				__log.warn("Could not find application session with ID " + _appSessionId);
				return null;
			}
			ServletTimer timer = applicationSession.getTimer(_id);
			if (timer == null)
				__log.warn("Could not find servlet timer with ID " + _id);
			return timer;
		}
	}

}
