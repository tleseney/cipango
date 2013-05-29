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
package org.cipango.diameter.node;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.sip.SipApplicationSession;

import org.cipango.server.session.SessionManager.AppSessionIf;
import org.cipango.server.session.SessionManager.ApplicationSessionScope;
import org.cipango.server.sipapp.SipAppContext;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.statistic.CounterStatistic;

@ManagedObject("Session manager")
public class SessionManager 
{
	private long _startTimestamp = ((System.currentTimeMillis() / 1000) & 0xffffffffl);
	private long _id;
	
	private Node _node;
	private Map<String, Session> _sessions = new HashMap<String, Session>();
	
	private CounterStatistic _sessionsCounter = new CounterStatistic();
	
	public Session createSession(DiameterRequest request)
	{
		Session diameterSession = new Session(request.getApplicationSession(), request.getSessionId(), request.getContext());
		synchronized (_sessions)
		{
			_sessions.put(diameterSession.getId(), diameterSession);
			if (_node.isStatsOn())
				_sessionsCounter.increment();
		}
		diameterSession.setNode(_node);
		return diameterSession;
	}
		
	public Session get(String id)
	{
		synchronized(_sessions)
		{
			return _sessions.get(id);
		}
	}
		
	public ApplicationSessionScope openScope(SipApplicationSession session)
	{
		AppSessionIf appSession = (AppSessionIf) session;
		if (appSession != null && appSession.getAppSession().getContext() != null)
		{
			SipAppContext context = appSession.getAppSession().getContext();
			return context.getSessionHandler().getSessionManager().openScope(appSession.getAppSession());
		}
		return null;
	}
		
	public void removeSession(Session session)
	{
		synchronized(_sessions)
		{
			_sessions.remove(session.getId());
			if (_node.isStatsOn())
				_sessionsCounter.decrement();
		}
	}
	
	protected synchronized String newSessionId()
	{
		return _node.getIdentity() + ";" + _startTimestamp + ";" + (++_id);
	}
	
	public void setNode(Node node)
	{
		_node = node;
	}
	
	@ManagedAttribute("Current sessions")
	public long getCurrentSessions()
	{
		return _sessionsCounter.getCurrent();
	}
	
	@ManagedAttribute("Max sessions")
	public long getMaxSessions()
	{
		return _sessionsCounter.getMax();
	}
	
	@ManagedAttribute("Total sessions")
	public long getTotalSessions()
	{
		return _sessionsCounter.getTotal();
	}
	
	public void statsReset()
    {
        _sessionsCounter.reset();
    }
	
}
