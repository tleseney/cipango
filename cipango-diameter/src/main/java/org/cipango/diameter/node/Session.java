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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.sip.SipApplicationSession;

import org.cipango.diameter.ApplicationId;
import org.cipango.diameter.DiameterCommand;
import org.cipango.diameter.api.DiameterSession;
import org.cipango.diameter.base.Common;
import org.cipango.diameter.base.Common.AuthSessionState;
import org.cipango.server.session.ApplicationSession;
import org.cipango.server.session.scoped.ScopedAppSession;
import org.cipango.server.sipapp.SipAppContext;

/**
 * Point-to-point Diameter relationship. 
 */
public class Session implements DiameterSession
{
	private Node _node;
	
	private ApplicationId _appId;
	private String _sessionId;
	
	private String _destinationRealm;
	private String _destinationHost;
	
	private SipApplicationSession _appSession;
	private SipAppContext _context;
	
	private boolean _valid = true;
	
	private Map<String, Object> _attributes;
		
	public Session(SipApplicationSession appSession, String sessionId, SipAppContext context)
	{
		_sessionId = sessionId;
		_appSession = appSession;
		_context = context;
	}
	
	public SipApplicationSession getApplicationSession()
	{
		if (_appSession instanceof ApplicationSession)
			return new ScopedAppSession((ApplicationSession) _appSession);
		return _appSession;
	}

	public void setApplicationId(ApplicationId appId)
	{
		_appId = appId;
	}
	
	public void setDestinationRealm(String destinationRealm)
	{
		_destinationRealm = destinationRealm;
	}
	
	/**
	 * Returns a new <code>DiameterRequest</code>.
	 * @param command the command of the new <code>DiameterRequest</code>.
	 * @param maintained if <code>true</code>, add the AVP Auth-Session-State with the value AuthSessionState.STATE_MAINTAINED.
	 * @return a new <code>DiameterRequest</code>.
	 * @throws java.lang.IllegalStateException if this <code>DiameterSession</code> has been invalidated.
	 * @see Common#AUTH_SESSION_STATE
	 * @see AuthSessionState#STATE_MAINTAINED
	 */
	public DiameterRequest createRequest(DiameterCommand command, boolean maintained)
	{
		checkValid();
		
		DiameterRequest request = new DiameterRequest(_node, command, _appId.getId(), _sessionId);
		request.getAVPs().add(Common.DESTINATION_REALM, _destinationRealm);
		if (_destinationHost != null)
			request.getAVPs().add(Common.DESTINATION_HOST, _destinationHost);
		
		request.getAVPs().add(_appId.getAVP());
		
		if (maintained)
			request.getAVPs().add(Common.AUTH_SESSION_STATE, AuthSessionState.STATE_MAINTAINED);
		
		request.setContext(_context);
		request.setSession(this);
		
		return request;
	}
	
	public String getId()
	{
		return _sessionId;
	}
	
	public ApplicationId getApplicationId()
	{
		return _appId;
	}
	
	public String getDestinationRealm()
	{
		return _destinationRealm;
	}
	
	public String getDestinationHost()
	{
		return _destinationHost;
	}
	
	public void setDestinationHost(String destinationHost)
	{
		_destinationHost = destinationHost;
	}
	
	public void setNode(Node node)
	{
		_node = node;
	}

	public boolean isValid()
	{
		return _valid;
	}
	
	public void invalidate()
	{
		checkValid();
		_valid = false;
		_node.getSessionManager().removeSession(this);
	}
	
	private void checkValid()
	{
		if (!_valid)
			throw new IllegalStateException("Session has been invalidated");
	}
	
	public Object getAttribute(String name) 
	{
		checkValid();
		if (name == null)
			throw new NullPointerException("Attribute name is null");
		if (_attributes == null)
			return null;
		return _attributes.get(name);
	}

	public Enumeration<String> getAttributeNames() 
	{
		checkValid();
		List<String> names;
		if (_attributes == null)
			names = Collections.emptyList();
		else
			names = new ArrayList<String>(_attributes.keySet());
		return Collections.enumeration(names);
	}
	
	public void removeAttribute(String name) 
	{
		checkValid();
		
		if (_attributes == null)
			return;
		
		 _attributes.remove(name);
	}

	public void setAttribute(String name, Object value) 
	{
		checkValid();
		
		if (name == null || value == null)
			throw new NullPointerException("name or value is null");
		
		if (_attributes == null)
			_attributes = new HashMap<String, Object>(3);
		
		_attributes.put(name, value);	
	}
}