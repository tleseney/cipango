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
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.sip.SipSession.State;

import org.cipango.server.session.ApplicationSession;
import org.cipango.server.session.ApplicationSession.Timer;
import org.cipango.server.session.SessionManager;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.annotation.ManagedOperation;
import org.eclipse.jetty.util.annotation.Name;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.infinispan.marshall.MarshalledValue;
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
			
			boolean hasConfirmedSession = putSessions(appSession, Fqn.fromRelativeElements(appSessionFqn, SESSIONS));	
			if (hasConfirmedSession)
			{
				appSession.notifyActivationListener(false);
				_treeCache.put(appSessionFqn, appSession.getData());	
				putTimers(appSession, Fqn.fromElements(appSessionFqn, TIMERS));
			}
			
			if (__log.isDebugEnabled())
				__log.debug("Successfully store call {} in replicated cache", applicationSession.getId());
			//S__log.warn(">>>>>>>>>>>>>>>>Call:"+ hasConfirmedSession+ "\n" + viewApplicationSession(applicationSession.getId()));
			__log.warn(">>>>>>>>>>>>>>>>Replicated Call:"+ hasConfirmedSession+ "\n" + viewReplicatedSession(applicationSession.getId()));
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
		List<Node<String, Object>> toRemove;
		if (node != null && node.getChildren() != null)
			toRemove = new ArrayList<>(node.getChildren());
		else
			toRemove = new ArrayList<>();
		
		Iterator it = appSession.getSessions("sip");
		while (it.hasNext())
		{
			ReplicatedSession session = (ReplicatedSession) it.next();
			if (session.getState() == State.CONFIRMED)
			{
				Fqn sessionFqn = Fqn.fromRelativeElements(sessionsFqn, session.getId());
				session.notifyActivationListener(false);	
				_treeCache.put(sessionFqn, session.getData());
				toRemove.remove(session.getId());
				hasConfirmedSession = true;
			}
		}
		
		Iterator<Node<String, Object>> it2 = toRemove.iterator();
		while (it2.hasNext())
			_treeCache.removeNode(it2.next().getFqn());
		
		return hasConfirmedSession;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void putTimers(ReplicatedAppSession appSession, Fqn timersFqn) throws Exception
	{
		Node node = _treeCache.getNode(timersFqn);
		List<Node<String, Object>> toRemove;
		if (node != null && node.getChildren() != null)
			toRemove = new ArrayList<>(node.getChildren());
		else
			toRemove = new ArrayList<>();
		Iterator it = appSession.getTimers().iterator();
		while (it.hasNext())
		{
			Timer timer = (Timer) it.next();
			if (timer.isPersistent())
			{
				Fqn timerFqn = Fqn.fromElements(timersFqn, timer.getId());
				_treeCache.put(timerFqn, appSession.getTimerData(timer));
				toRemove.remove(timer.getId());
			}
		}
		
		Iterator<Node<String, Object>> it2 = toRemove.iterator();
		while (it2.hasNext())
			_treeCache.removeNode(it2.next().getFqn());
	}
	
	@ManagedOperation(value="Returns a set containing the IDs of replicated application sessions", impact="INFO")
	public Set<Object> getReplicatedAppSessionIds() throws Exception
	{
		return _treeCache.getNode(_baseFqn).getChildrenNames();
	} 
	
	@ManagedOperation(value="View data save in cache for application session with id sessionId", impact="INFO")
	public String viewReplicatedSession(@Name("sessionId")String sessionId) throws Exception
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
//			Iterator<Node<String, Object>> it = node.getChildren().iterator();
//			while (it.hasNext())
//			{
//				Node<String, Object> appSession = it.next();
//				sb.append("\t+ ").append(node.getFqn().getLastElement()).append("\n");
//				index++;
//								
//				print(sb, appSession, index);
//			}
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
			if (value instanceof MarshalledValue)
			{
				MarshalledValue m = (MarshalledValue) value;
				try
				{
					sb.append(m.get());
				}
				catch (Throwable e) 
				{
					sb.append("FAILED TO DESERIALIZE NODE with key ").append(key);
					sb.append(": ").append(e.getMessage());
				}
				//sb.append(" (size: ").append(m.getRaw().size()).append(" bytes)");
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

	
}
