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

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.sip.SipApplicationSession;

import org.cipango.diameter.AVP;
import org.cipango.diameter.AVPList;
import org.cipango.diameter.DiameterCommand;
import org.cipango.diameter.Type;
import org.cipango.diameter.api.DiameterServletMessage;
import org.cipango.diameter.api.DiameterSession;
import org.cipango.diameter.base.Common;
import org.cipango.diameter.util.CommandUtil;
import org.cipango.diameter.util.DiameterVisitor;
import org.cipango.diameter.util.PrettyPrinter;
import org.cipango.diameter.util.Visitable;

/**
 * Base class for diameter requests and answers.
 */
public abstract class DiameterMessage implements Visitable, DiameterServletMessage
{
	protected DiameterCommand _command;
	
	//protected int _command;
	protected AVPList _avps;
	
	protected int _applicationId;
	protected int _hopByHopId;
	protected int _endToEndId;

	protected Node _node;
	protected DiameterConnection _connection;
	
	protected Session _session;
	
	private Map<String, Object> _attributes;
		
	public DiameterMessage()
	{
	}
	
	public DiameterMessage(Node node, int appId, DiameterCommand command, int endToEndId, int hopByHopId, String sessionId)
	{
		_node = node;
		_applicationId = appId;
		_command = command;
		_hopByHopId = hopByHopId;
		_endToEndId = endToEndId;
	
		_avps = new AVPList();
		
		if (sessionId != null)
			_avps.add(Common.SESSION_ID, sessionId);
		_avps.add(Common.ORIGIN_HOST, node.getIdentity());
		_avps.add(Common.ORIGIN_REALM, node.getRealm());
	}
	
	public DiameterMessage(DiameterMessage message)
	{
		this(message._node, 
				message._applicationId, 
				CommandUtil.getAnswer(message._command), 
				message._endToEndId, 
				message._hopByHopId, 
				message.getSessionId());
	}
	
	public <T> T get(Type<T> type)
	{
		return _avps.getValue(type);
	}
	
	public <T> void add(Type<T> type, T value)
	{
		_avps.add(type, value);
	}
	
	public Node getNode()
	{
		return _node;
	}
	
	public void setNode(Node node)
	{
		_node = node;
	}
	
	public void setConnection(DiameterConnection connection)
	{
		_connection = connection;
	}
	
	public DiameterConnection getConnection()
	{
		return _connection;
	}

	public int getApplicationId()
	{
		return _applicationId;
	}
	
	public void setApplicationId(int applicationId)
	{
		_applicationId = applicationId;
	}
	
	public int getHopByHopId()
	{
		return _hopByHopId;
	}
	
	public void setHopByHopId(int hopByHopId)
	{
		_hopByHopId = hopByHopId;
	}
	
	public int getEndToEndId()
	{
		return _endToEndId;
	}
	
	public void setEndToEndId(int endToEndId)
	{
		_endToEndId = endToEndId;
	}
	
	public void setCommand(DiameterCommand command)
	{
		_command = command;
	}
	
	public DiameterCommand getCommand()
	{
		return _command;
	}
	
	public String getOriginHost()
	{
		return get(Common.ORIGIN_HOST);
	}
	
	public String getOriginRealm()
	{
		return get(Common.ORIGIN_REALM);
	}
	
	public String getSessionId()
	{
		return get(Common.SESSION_ID);
	}
	
	public int size()
	{
		return _avps.size();
	}
	
	public AVPList getAVPs()
	{
		return _avps;
	}
	
	public void setAVPList(AVPList avps)
	{
		_avps = avps;
	}
	
	public DiameterSession getSession()
	{
		return getSession(true);
	}

	public abstract DiameterSession getSession(boolean create);
	
	public abstract SipApplicationSession getApplicationSession();
	
	public void setSession(Session session)
	{
		_session = session;
	}
	
	public abstract boolean isRequest();
	public abstract void send() throws IOException;
	
	public void accept(DiameterVisitor visitor)
	{
		visitor.visit(this);
		
		for (AVP<?> avp : _avps)
		{
			avp.accept(visitor);
		}
	}
	
	public String toString()
	{
		PrettyPrinter p = new PrettyPrinter();
		accept(p);
		return p.toString();
	}
	
	public Object getAttribute(String name) 
	{
		if (_attributes != null) 
			return _attributes.get(name);
		return null;
	}
	
	public void removeAttribute(String name)
	{
		if (_attributes == null)
			return;
		_attributes.remove(name);
	}
	

	@SuppressWarnings("unchecked")
	public Enumeration<String> getAttributeNames() 
	{
		if (_attributes != null) 
			return Collections.enumeration(_attributes.keySet());
		
		return Collections.enumeration(Collections.EMPTY_LIST);
	}
	
	public void setAttribute(String name, Object o) 
	{
		if (o == null || name == null) 
			throw new NullPointerException("name or value is null");
		
		if (_attributes == null) 
			_attributes = new HashMap<String, Object>();

		_attributes.put(name, o);
	}	
}
