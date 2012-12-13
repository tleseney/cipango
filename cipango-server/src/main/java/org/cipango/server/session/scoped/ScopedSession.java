// ========================================================================
// Copyright 2008-2012 NEXCOM Systems
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

package org.cipango.server.session.scoped;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Enumeration;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.sip.Address;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.URI;
import javax.servlet.sip.ar.SipApplicationRoutingRegion;

import org.cipango.server.session.ApplicationSession;
import org.cipango.server.session.Session;
import org.cipango.server.session.SessionManager.ApplicationSessionScope;
import org.cipango.server.session.SessionManager.SipSessionIf;

public class ScopedSession extends ScopedObject implements SipSessionIf
{
	private Session _session;

	public ScopedSession(Session session)
	{
		_session = session;
	}

	public SipServletRequest createRequest(String method)
	{
		return _session.createRequest(method);
	}

	public SipApplicationSession getApplicationSession()
	{
		return new ScopedAppSession(_session.appSession());
	}

	public Object getAttribute(String name)
	{
		return _session.getAttribute(name);
	}

	public Enumeration<String> getAttributeNames()
	{
		return _session.getAttributeNames();
	}

	public String getCallId()
	{
		return _session.getCallId();
	}

	public long getCreationTime()
	{
		return _session.getCreationTime();
	}

	public String getId()
	{
		return _session.getId();
	}

	public boolean getInvalidateWhenReady()
	{
		return _session.getInvalidateWhenReady();
	}

	public long getLastAccessedTime()
	{
		return _session.getLastAccessedTime();
	}

	public Address getLocalParty()
	{
		return _session.getLocalParty();
	}

	public SipApplicationRoutingRegion getRegion()
	{
		return _session.getRegion();
	}

	public Address getRemoteParty()
	{
		return _session.getRemoteParty();
	}

	public ServletContext getServletContext()
	{
		return _session.getServletContext();
	}

	public State getState()
	{
		return _session.getState();
	}

	public URI getSubscriberURI()
	{
		return _session.getSubscriberURI();
	}

	public void invalidate()
	{
		ApplicationSessionScope scope = openScope();
		try
		{
			_session.invalidate();
		}
		finally
		{
			scope.close();
		}
	}

	public boolean isReadyToInvalidate()
	{
		return _session.isReadyToInvalidate();
	}

	public boolean isValid()
	{
		return _session.isValid();
	}

	public void removeAttribute(String name)
	{
		ApplicationSessionScope scope = openScope();
		try
		{
			_session.removeAttribute(name);
		}
		finally
		{
			scope.close();
		}
	}

	public void setAttribute(String name, Object value)
	{
		ApplicationSessionScope scope = openScope();
		try
		{
			_session.setAttribute(name, value);
		}
		finally
		{
			scope.close();
		}
	}

	public void setHandler(String name) throws ServletException
	{
		ApplicationSessionScope scope = openScope();
		try
		{
			_session.setHandler(name);
		}
		finally
		{
			scope.close();
		}
	}

	public void setInvalidateWhenReady(boolean invalidateWhenReady)
	{
		_session.setInvalidateWhenReady(invalidateWhenReady);
	}

	public void setOutboundInterface(InetAddress address)
	{
		_session.setOutboundInterface(address);
	}

	public void setOutboundInterface(InetSocketAddress address)
	{
		_session.setOutboundInterface(address);
	}

	public Session getSession()
	{
		return _session;
	}

	@Override
	public String toString()
	{
		return _session.toString();
	}

	@Override
	public boolean equals(Object o)
	{
		if (!(o instanceof SipSessionIf))
			return false;
		return _session.equals(((SipSessionIf) o).getSession());
	}

	@Override
	public int hashCode()
	{
		return _session.hashCode();
	}

	@Override
	protected ApplicationSession getAppSession()
	{
		return _session.appSession();
	}
}
