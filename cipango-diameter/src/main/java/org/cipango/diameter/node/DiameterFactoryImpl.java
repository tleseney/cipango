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

import javax.servlet.sip.SipApplicationSession;

import org.cipango.diameter.ApplicationId;
import org.cipango.diameter.DiameterCommand;
import org.cipango.diameter.api.DiameterFactory;
import org.cipango.diameter.api.DiameterServletRequest;
import org.cipango.diameter.base.Common;
import org.cipango.server.sipapp.SipAppContext;

public class DiameterFactoryImpl implements DiameterFactory
{
	private Node _node;
	private SipAppContext _appContext;
	
	public DiameterServletRequest createRequest(SipApplicationSession appSession, ApplicationId id,
			DiameterCommand command, String destinationRealm)
	{
		return createRequest(appSession, id, command, destinationRealm, null);
	}

	public DiameterServletRequest createRequest(SipApplicationSession appSession, ApplicationId id,
			DiameterCommand command, String destinationRealm, String destinationHost)
	{	
		String sessionId = _node.getSessionManager().newSessionId();
		DiameterRequest request = new DiameterRequest(_node, command, id.getId(),sessionId);
		request.getAVPs().add(Common.DESTINATION_REALM, destinationRealm);
		if (destinationHost != null)
			request.getAVPs().add(Common.DESTINATION_HOST, destinationHost);
		
		request.getAVPs().add(id.getAVP());
		request.setApplicationSession(appSession);
		request.setContext(_appContext);
		request.setUac(true);
		return request;
	}

	@Deprecated
	public DiameterServletRequest createRequest(ApplicationId id, DiameterCommand command, String destinationRealm)
	{
		return createRequest(id, command, destinationRealm, null);
	}
	
	@Deprecated
	public DiameterServletRequest createRequest(ApplicationId id, DiameterCommand command, String destinationRealm, String destinationHost)
	{
		return createRequest(null, id, command, destinationRealm, destinationHost);
	}
	
	public void setNode(Node node)
	{
		_node = node;
	}
	
	protected Node getNode()
	{
		return _node;
	}

	public SipAppContext getAppContext()
	{
		return _appContext;
	}

	public void setAppContext(SipAppContext appContext)
	{
		_appContext = appContext;
	}

}
