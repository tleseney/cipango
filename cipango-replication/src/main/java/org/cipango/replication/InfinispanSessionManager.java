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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.sip.SipSession.State;

import org.cipango.server.session.ApplicationSession;
import org.cipango.server.session.ApplicationSession.Timer;
import org.cipango.server.session.SessionManager;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.annotation.ManagedOperation;
import org.eclipse.jetty.util.annotation.Name;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.infinispan.tree.Fqn;
import org.infinispan.tree.Node;
import org.infinispan.tree.TreeCache;

@ManagedObject
public class InfinispanSessionManager extends SessionManager
{
	public static final String SESSIONS = "sipSessions";
	public static final String TIMERS = "timers";
	
	private final static Logger __log = Log.getLogger(InfinispanSessionManager.class);
	private TreeCache<String, Object> _treeCache;
	private Fqn _baseFqn;
	
	@SuppressWarnings("unchecked")
	@Override
	protected void doStart() throws Exception
	{
		super.doStart();
		
		_treeCache = getSipAppContext().getServer().getBean(TreeCache.class);
		
		if (_treeCache == null)
			throw new NullPointerException("Tree cache is null");
		
		_treeCache.start();
		
		if (getSipAppContext().getContextId() != null)
			_baseFqn = Fqn.fromElements(getSipAppContext().getContextId(), "appSessions");
		else
			_baseFqn = Fqn.fromElements("appSessions");
		
		List<ReplicatedAppSession> calls = loadCalls();
		synchronized (this)
		{
			ConcurrentHashMap<String, ApplicationSession> sessions = getAppSessions();
			for (ReplicatedAppSession session : calls)
				sessions.put(session.getId(), session);
			
		}
	}
	
	protected List<ReplicatedAppSession> loadCalls()
	{
		List<ReplicatedAppSession> calls = new ArrayList<ReplicatedAppSession>();
		
		Node<String, Object> root = _treeCache.getNode(_baseFqn);
		if (root == null)
			return calls;
		
		ReplicatedAppSession session;
		Set<Node<String, Object>> children = root.getChildren();
		if (children != null)
		{
			for (Node<String, Object> node : children)
			{
				session = loadCall(node);
				if (session != null)
					calls.add(session);
			}
		}
		return calls;
		
	}
	
	protected ReplicatedAppSession loadCall(Node<String, Object> node)
	{
		String id = node.getFqn().getLastElementAsString();
		
		
		try
		{
			ReplicatedAppSession appSession = new ReplicatedAppSession(this, id, node.getData());
			
			if (appSession.getExpirationTime() == Long.MIN_VALUE)
			{
				__log.warn("Application session " + appSession + " is not added as it is expired");
				_treeCache.removeNode(node.getFqn());
				return null;
			}
			
			Node<String, Object> sessionsNode = node.getChild(SESSIONS);
			if (sessionsNode != null)
			{
				
				for (Node<String, Object> sessionNode : sessionsNode.getChildren())
				{
					ReplicatedSession session = new ReplicatedSession(appSession, sessionNode.getFqn()
							.getLastElementAsString(), sessionNode.getData());
					appSession.addSession(session);
				}
			}
			
			Node<String, Object> timersNode = node.getChild(TIMERS);
			if (timersNode != null)
			{
				for (Node<String, Object> timerNode : timersNode.getChildren())
					appSession.newTimer(timerNode.getData(), timerNode.getFqn().getLastElementAsString());
			}
			
			return appSession;
			
		}
		catch (Throwable e)
		{
			__log.warn("Failed to load application session with id " + id, e);
			try
			{
				_treeCache.removeNode(node.getFqn());
			}
			catch (Exception e2)
			{
				__log.ignore(e2);
			}
			return null;
		}
	}
	
	@Override
	protected void doStop() throws Exception
	{
		super.doStop();
		_treeCache.stop();
	}
	
	@Override
	protected void saveSession(ApplicationSession applicationSession)
	{
		super.saveSession(applicationSession);
		try
		{
			ReplicatedAppSession appSession = (ReplicatedAppSession) applicationSession;
			Fqn appSessionFqn = Fqn.fromRelativeElements(_baseFqn, applicationSession.getId());
			
			boolean hasConfirmedSession = putSessions(appSession,
					Fqn.fromRelativeElements(appSessionFqn, SESSIONS));
			if (hasConfirmedSession)
			{
				appSession.notifyActivationListener(false);
				_treeCache.put(appSessionFqn, appSession.getData());
				putTimers(appSession, Fqn.fromElements(appSessionFqn, TIMERS));
			}
			
			if (__log.isDebugEnabled())
				__log.debug("Successfully store call {} in replicated cache", applicationSession.getId());
			
			if (hasConfirmedSession)
				__log.warn(">>>>>>>>>>>>>>>>Replicated session:\n"
					+ viewReplicatedSession(applicationSession.getId()));
		}
		catch (Exception e)
		{
			__log.warn("Failed to store call " + applicationSession.getId() + " in replicated cache: " + e, e);
		}
	}
	
	@SuppressWarnings({ "rawtypes" })
	private boolean putSessions(ReplicatedAppSession appSession, Fqn sessionsFqn) throws Exception
	{
		boolean hasConfirmedSession = false;
		Node<String, Object> node = _treeCache.getNode(sessionsFqn);
		List<Fqn> toRemove = new ArrayList<>();
		if (node != null && node.getChildren() != null)
		{
			for (Node<String, Object> child : node.getChildren())
				toRemove.add(child.getFqn());
		}
		
		
		Iterator it = appSession.getSessions("sip");
		while (it.hasNext())
		{
			ReplicatedSession session = (ReplicatedSession) it.next();
			if (session.getState() == State.CONFIRMED)
			{
				Fqn sessionFqn = Fqn.fromRelativeElements(sessionsFqn, session.getId());
				session.notifyActivationListener(false);
				_treeCache.put(sessionFqn, session.getData());
				toRemove.remove(sessionFqn);
				hasConfirmedSession = true;
			}
		}
		
		Iterator<Fqn> it2 = toRemove.iterator();
		while (it2.hasNext())
			_treeCache.removeNode(it2.next());
		
		return hasConfirmedSession;
	}
	
	@SuppressWarnings({ "rawtypes" })
	private void putTimers(ReplicatedAppSession appSession, Fqn timersFqn) throws Exception
	{
		Node<String, Object> node = _treeCache.getNode(timersFqn);
		List<Fqn> toRemove = new ArrayList<>();
		if (node != null && node.getChildren() != null)
		{
			for (Node<String, Object> child : node.getChildren())
				toRemove.add(child.getFqn());
		}
		
		Iterator it = appSession.getTimers().iterator();
		while (it.hasNext())
		{
			Timer timer = (Timer) it.next();
			if (timer.isPersistent())
			{
				Fqn timerFqn = Fqn.fromElements(timersFqn, timer.getId());
				_treeCache.put(timerFqn, appSession.getTimerData(timer));
				toRemove.remove(timerFqn);
			}
		}
		
		Iterator<Fqn> it2 = toRemove.iterator();
		while (it2.hasNext())
			_treeCache.removeNode(it2.next());
	}
	
	@ManagedAttribute(value = "Returns a set containing the IDs of replicated application sessions")
	public Set<Object> getReplicatedAppSessionIds() throws Exception
	{
		Node<String, Object> node = _treeCache.getNode(_baseFqn);
		if (node == null)
			return Collections.emptySet();
		return node.getChildrenNames();
	}
	
	@ManagedOperation(value = "View data save in cache for application session with id sessionId", impact = "INFO")
	public String viewReplicatedSession(@Name("sessionId") String sessionId) throws Exception
	{
		int index = 1;
		StringBuilder sb = new StringBuilder();
		sb.append(sessionId).append('\n');
		
		Fqn appSession = Fqn.fromRelativeElements(_baseFqn, sessionId);
		
		Node<String, Object> node = _treeCache.getNode(appSession);
		if (node == null)
		{
			sb.append("No app session found");
			return sb.toString();
		}
		index++;
		ClassLoader ocl = Thread.currentThread().getContextClassLoader();
		
		try
		{
			Thread.currentThread().setContextClassLoader(getSipAppContext().getClassLoader());
			
			print(sb, node, index);
			// Iterator<Node<String, Object>> it = node.getChildren().iterator();
			// while (it.hasNext())
			// {
			// Node<String, Object> appSession = it.next();
			// sb.append("\t+ ").append(node.getFqn().getLastElement()).append("\n");
			// index++;
			//
			// print(sb, appSession, index);
			// }
		}
		finally
		{
			Thread.currentThread().setContextClassLoader(ocl);
		}
		return sb.toString();
	}
	
	private void print(StringBuilder sb, Node<String, Object> node, int index) throws Exception
	{
		for (Entry<String, Object> entry : node.getData().entrySet())
		{
			String key = entry.getKey();
			Object value = entry.getValue();
			
			for (int i = 0; i < index; i++)
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
		
		if (node.getChildren() != null)
		{
			for (Node<String, Object> node2 : node.getChildren())
			{
				for (int i = 0; i < index; i++)
					sb.append('\t');
				sb.append("+ [").append(node2.getFqn().getLastElement()).append("]\n");
				
				for (Node<String, Object> node3 : node2.getChildren())
				{
					for (int i = 0; i < index + 1; i++)
						sb.append('\t');
					sb.append("+ ").append(node3.getFqn().getLastElement()).append("\n");
					if (node3 != null)
						print(sb, node3, index + 2);
				}
			}
		}
	}
	
	@Override
	public void removeApplicationSession(ApplicationSession session)
	{
		// TODO Auto-generated method stub
		super.removeApplicationSession(session);
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
	
}
