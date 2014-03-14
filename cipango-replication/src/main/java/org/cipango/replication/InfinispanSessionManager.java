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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.ServletTimer;
import javax.servlet.sip.SipSession.State;

import org.cipango.replication.ReplicatedAppSession.ProxyAppSession;
import org.cipango.server.session.ApplicationSession;
import org.cipango.server.session.ApplicationSession.Timer;
import org.cipango.server.session.SessionManager;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.annotation.ManagedOperation;
import org.eclipse.jetty.util.annotation.Name;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.infinispan.Cache;


@ManagedObject
public class InfinispanSessionManager extends SessionManager
{
	public static final String SESSIONS = "sipSessions";
	public static final String TIMERS = "timers";
	public static final String DATA = "data";
	
	private final static Logger __log = Log.getLogger(InfinispanSessionManager.class);
	private CacheWrapper _cache;
	
	@SuppressWarnings("unchecked")
	@Override
	protected void doStart() throws Exception
	{
		super.doStart();
		
		_cache = new CacheWrapper(getSipAppContext().getServer().getBean(
				Cache.class), getSipAppContext().getContextId());
		_cache.start();
		
		List<ReplicatedAppSession> calls = loadSessions();
		synchronized (this)
		{
			ConcurrentHashMap<String, ApplicationSession> sessions = getAppSessions();
			for (ReplicatedAppSession session : calls)
				sessions.put(session.getId(), session);
			
		}
	}

	@Override
	protected void doStop() throws Exception
	{
		super.doStop();
		_cache.stop();
	}

	protected List<ReplicatedAppSession> loadSessions()
	{
	
		List<ReplicatedAppSession> calls = new ArrayList<ReplicatedAppSession>();
		List<String> sessions = _cache.getAppSessions();
		
		__log.info("Got {} calls to load from cache for application {}", sessions.size(), getSipAppContext().getName());
		ReplicatedAppSession session;
		if (sessions != null)
		{
			ClassLoader old = Thread.currentThread().getContextClassLoader();
			try
			{
				Thread.currentThread().setContextClassLoader(getSipAppContext().getClassLoader());
    			for (String key : new ArrayList<String>(sessions))
    			{
    				session = loadSession(key);
    				if (session != null)
    					calls.add(session);
    				else
    					sessions.remove(key);
    			}
    
    			__log.warn("**** \t Storing " + sessions.size() + " calls");
    			_cache.setAppSessions(sessions);
			}
			finally
			{
				Thread.currentThread().setContextClassLoader(old);
			}
		}
		return calls;
	}
	
	protected ReplicatedAppSession loadSession(String key)
	{
		try
		{
			// TODO: take care of return value.
			_cache.startBatch();
			AppSessionNode node = new AppSessionNode(key);
			ReplicatedAppSession appSession = node.load();
			_cache.endBatch(appSession != null);
			return appSession;
		}
		catch (Throwable t)
		{
			__log.warn("Failed to load application session with key " + key, t);
			try
			{
				_cache.endBatch(false);
				_cache.remove(key);
			}
			catch (Exception e)
			{
				__log.ignore(e);
			}
			return null;
		}
	}
	
	@Override
	protected void saveSession(ApplicationSession applicationSession)
	{
		// TODO: use mortal data with the session expiration time?
		// See https://docs.jboss.org/author/display/ISPN/Using+the+Cache+API
		super.saveSession(applicationSession);
		try
		{
			// TODO: take care of return value.
			_cache.startBatch();
			AppSessionNode node = new AppSessionNode();
			node.store((ReplicatedAppSession) applicationSession);
			_cache.endBatch(true);
			
			if (__log.isDebugEnabled())
				__log.debug("Successfully store call {} in replicated cache", applicationSession.getId());
		}
		catch (Exception e)
		{
			_cache.endBatch(false);
			__log.warn("Failed to store call " + applicationSession.getId() + " in replicated cache: " + e, e);
		}
	}
	
	private void putSipSessions(Iterator<?> sessions, AppSessionNode node) throws Exception
	{
		List<String> sessionIds = node.getSessions();
		List<String> toRemove = new ArrayList<String>(sessionIds);
		
		while (sessions.hasNext())
		{
			ReplicatedSession session = (ReplicatedSession) sessions.next();
			if (session.getState() == State.CONFIRMED)
			{
				String key = node.getKeyFromId(session.getId());
				session.notifyActivationListener(false);
				_cache.put(key, session.getData());
				if (!sessionIds.contains(key))
					sessionIds.add(key);
				toRemove.remove(key);
			}
		}
		
		Iterator<String> iter = toRemove.iterator();
        while (iter.hasNext())
        {
        	String id = iter.next();
        	sessionIds.remove(id);
        	_cache.remove(id);
        }
	}
	
	private void putTimers(ReplicatedAppSession appSession, AppSessionNode node) throws Exception
	{
		List<String> timerIds = node.getTimers();
		if (timerIds.isEmpty() && appSession.getTimers().isEmpty())
			return;
		
		List<String> toRemove = new ArrayList<String>(timerIds);
		
		Iterator<ServletTimer> it = appSession.getTimers().iterator();
		while (it.hasNext())
		{
			Timer timer = (Timer) it.next();
			if (timer.isPersistent())
			{
				String key = node.getKeyFromId(timer.getId()); // FIXME should prefix with timer to prevent collisions with sessions
				_cache.put(key, appSession.getTimerData(timer));
				if (!timerIds.contains(key))
					timerIds.add(key);
				toRemove.remove(key);
			}
		}

		Iterator<String> iter = toRemove.iterator();
        while (iter.hasNext())
        {
        	String id = iter.next();
        	timerIds.remove(id);
        	_cache.remove(id);
        }
        node.setTimers(timerIds);
	}
	
	@ManagedAttribute(value = "Returns a list containing the IDs of replicated application sessions")
	public List<String> getReplicatedAppSessionIds() throws Exception
	{
		return new ArrayList<String>(_cache.getAppSessions());
	}
	
	@ManagedOperation(value = "View data save in cache for application session with id sessionId", impact = "INFO")
	public String viewReplicatedSession(@Name("sessionId") String sessionId) throws Exception
	{
		SessionManager old = ProxyAppSession.getSessionManager();
		ClassLoader ocl = Thread.currentThread().getContextClassLoader();
		try
		{
			ProxyAppSession.setSessionManager(this);
    		Thread.currentThread().setContextClassLoader(getSipAppContext().getClassLoader());
    		
    		StringBuilder sb = new StringBuilder();
    		sb.append(sessionId).append('\n');
    		
    		AppSessionNode node = new AppSessionNode(sessionId);
    		if (!node.exists())
    		{
    			sb.append("No app session found");
    			return sb.toString();
    		}
    		
    		print(sb, node);

    		return sb.toString();
		}
		finally
		{
			ProxyAppSession.setSessionManager(old);
			Thread.currentThread().setContextClassLoader(ocl);
		}
	}
	
	@SuppressWarnings("unchecked")
	private void print(StringBuilder sb, AppSessionNode node) throws Exception
	{
		print(sb, node.getData(), 1);
		
		sb.append("\t+ [sipSessions]\n");
		for (String key : node.getSessions())
		{
			sb.append("\t\t+ " + node.getIdFromKey(key) + "\n");
			print(sb, (Map<String, Object>) _cache.get(key), 3);
		}

		sb.append("\t+ [timers]\n");
		for (String key : node.getTimers())
		{
			sb.append("\t\t+ " + node.getIdFromKey(key) + "\n");
			print(sb, (Map<String, Object>) _cache.get(key), 3);
		}
	}
	
	private void print(StringBuilder sb, Map<String, Object> data, int depth) throws Exception
	{
		for (Entry<String, Object> entry : data.entrySet())
		{
			String key = entry.getKey();
			Object value = entry.getValue();
			
			for (int i = 0; i < depth; i++)
				sb.append('\t');
			sb.append("- ").append(key).append(": ");
			if (value instanceof byte[])
			{
				byte[] b = (byte[]) value;
				try
				{
					sb.append(Serializer.deserialize(b));
				}
				catch (Throwable e)
				{
					sb.append("FAILED TO DESERIALIZE NODE with key ").append(key);
					sb.append(": ").append(e.getMessage());
					__log.warn("FAILED TO DESERIALIZE NODE with key " + key, e);
				}
				sb.append(" (size: ").append(b.length).append(" bytes)");
			}
			else if (value instanceof String[])
				sb.append(Arrays.asList((String[]) value));
			else
			{
				sb.append(value);
				if ("created".equals(key) || "accessed".equals(key) || "expirationTime".equals(key))
					sb.append(" (").append(new Date((Long) value)).append(")");
			}
			sb.append('\n');
		}
				}
	
	@Override
	public void removeApplicationSession(ApplicationSession session)
	{
		super.removeApplicationSession(session);
		try
		{
			_cache.startBatch();
			
			AppSessionNode node = new AppSessionNode(session.getId());
			if (node != null)
			{
				for (String key : node.getSessions())
					_cache.remove(key);
				
				for (String key : node.getTimers())
					_cache.remove(key);
			}
			_cache.remove(session.getId());
			List<String> appSessions = _cache.getAppSessions();
			appSessions.remove(session.getId());
			_cache.setAppSessions(appSessions);
			
			_cache.endBatch(true);
		} 
		catch (Exception e)
		{
			_cache.endBatch(false);
		}
	}
	
	@Override
	public ApplicationSession createApplicationSession(String id)
	{
		ApplicationSession appSession = new ReplicatedAppSession(this, id);
		appSession = addApplicationSession(appSession);
		return appSession;
	}

	@Override
	public ApplicationSession getApplicationSession(String id)
	{
		ApplicationSession session = super.getApplicationSession(id);
		if (session != null && session instanceof ReplicatedAppSession)
			((ReplicatedAppSession) session).deserializeIfNeeded();
		return session;
	}
	
	private class CacheWrapper extends AbstractLifeCycle
	{
		private Cache<String, Object> _cache;
		private String _base;
		
		protected CacheWrapper(Cache<String, Object> cache, String contextId)
		{
			if (cache == null)
				throw new NullPointerException("Cache is null");
			
			_cache = cache;
			if (contextId != null)
				_base = contextId;
			else
				_base = "context";
			__log.debug("Use context ID: {} for base", _base);
		}

		@Override
		protected void doStart() throws Exception
		{
			super.doStop();
			_cache.start();
		}

		@Override
		protected void doStop() throws Exception
		{
			super.doStop();
			_cache.stop();
		}
		
		public Object get(String key)
		{
			return _cache.get(key);
		}
		
		public Object put(String key, Object o)
		{
			return _cache.put(key,  o);
		}
		
		public Object remove(String key)
		{
			return _cache.remove(key);
		}
		
		public boolean startBatch()
		{
			return _cache.startBatch();
		}
		
		public void endBatch(boolean successful)
		{
			_cache.endBatch(successful);
		}
		
		@SuppressWarnings("unchecked")
		public List<String> getAppSessions()
		{
			List<String> root = (List<String>) _cache.get(_base);
			if (root == null)
				root = new ArrayList<String>();

			return root;
		}

		public void addAppSession(String id)
		{
			List<String> sessions = getAppSessions();
			if (!sessions.contains(id))
			{
				sessions.add(id);
				setAppSessions(sessions);
			}
		}
			
		public void setAppSessions(List<String> sessions)
		{
			_cache.put(_base, sessions);
		}
	}
	
	private class AppSessionNode
	{
		private Map<String, Object> _node;
		private String _id;
		
		AppSessionNode()
		{
			_node = new HashMap<String, Object>();
			_node.put(SESSIONS, new ArrayList<>(2));
		}
		
		@SuppressWarnings("unchecked")
		AppSessionNode(String id)
		{
			_id = id;
			_node = (Map<String, Object>) _cache.get(id);
		}

		boolean exists()
		{
			return _node != null;
		}

		ReplicatedAppSession load() throws ClassNotFoundException, IOException, ServletParseException, ParseException
		{
			
			ReplicatedAppSession appSession = new ReplicatedAppSession(InfinispanSessionManager.this, _id, getData());
			if (appSession.getExpirationTime() == Long.MIN_VALUE)
			{
				__log.warn("Application session " + appSession + " is not added as it is expired");
				_cache.remove(_id);
				return null;
			}
			
			List<String> keys = getSessions();
			if (keys != null)
			{
				for (String key : keys)
					appSession.addSession(new ReplicatedSession(appSession, getIdFromKey(key), getItemData(key)));
			}
			
			List<String> timersKeys = getTimers();
			if (timersKeys != null)
			{
				for (String key : timersKeys)
					appSession.newTimer(getItemData(key), getIdFromKey(key));
			}
			return appSession;
		}
		
		void store(ReplicatedAppSession appSession) throws Exception
		{
			_id = appSession.getId();
			putSipSessions(appSession.getSessions("sip"), this);
			if (!getSessions().isEmpty())
			{
				appSession.notifyActivationListener(false);
				putTimers(appSession, this);
				_node.put(DATA, appSession.getData());
				_cache.put(_id, _node);
				_cache.addAppSession(_id);
			}
		}

		@SuppressWarnings("unchecked")
		Map<String, Object> getData()
		{
			return (Map<String, Object>) _node.get(DATA);
		}
		
		@SuppressWarnings("unchecked")
		List<String> getSessions()
		{
			return (List<String>) _node.get(SESSIONS);
		}
		
		@SuppressWarnings("unchecked")
		List<String> getTimers()
		{
			List<String> list = (List<String>) _node.get(TIMERS);
			if (list == null)
				return new ArrayList<>(0);
			return list;
		}
		
		public void setTimers(List<String> timers)
		{
			if (timers != null && !timers.isEmpty())
				_node.put(TIMERS, timers);
		}
		
		@SuppressWarnings("unchecked")
		Map<String, Object> getItemData(String key)
		{
			return (Map<String, Object>) _cache.get(key);
		}
		
		String getKeyFromId(String id)
		{
			return _id + "." + id;			
		}

		String getIdFromKey(String key)
		{
			return key.substring(_id.length() + 1);			
		}
	}
}
