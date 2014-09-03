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
package org.cipango.tests.replication;

import java.io.Serializable;

import javax.servlet.sip.Address;
import javax.servlet.sip.SipApplicationSessionActivationListener;
import javax.servlet.sip.SipApplicationSessionEvent;
import javax.servlet.sip.SipSessionActivationListener;
import javax.servlet.sip.SipSessionEvent;
import javax.servlet.sip.URI;

public class Custom implements Serializable, SipSessionActivationListener, SipApplicationSessionActivationListener
{

	private static final long serialVersionUID = 1L;
	
	private transient int _param;
	private int _nbActivate = 0;
	private int _nbPassivate = 0;
	private int _backup = -1;
	private URI _uri;
	private Address _addr;
	private URI _telUri;

	public Custom(int param)
	{
		_param = param;
	}
		
	@Override
	public String toString()
	{
		return "{Activate: " + _nbActivate + ", passivate: " +  _nbPassivate 
					+ ", param: " + _param + ",addr: " + _addr + "}";
	}

	public void sessionDidActivate(SipSessionEvent e)
	{
		_param = _backup;
		_nbActivate++;
	}

	public void sessionWillPassivate(SipSessionEvent e)
	{
		_backup = _param;
		_nbPassivate++;
	}

	public int getParam()
	{
		return _param;
	}

	public int getNbActivate()
	{
		return _nbActivate;
	}

	public int getNbPassivate()
	{
		return _nbPassivate;
	}

	public void sessionDidActivate(SipApplicationSessionEvent e)
	{
		_param = _backup;
		_nbActivate++;
	}

	public void sessionWillPassivate(SipApplicationSessionEvent e)
	{
		_backup = _param;
		_nbPassivate++;
	}

	public Address getAddr()
	{
		return _addr;
	}

	public void setAddr(Address addr)
	{
		_addr = addr;
	}

	public URI getUri()
	{
		return _uri;
	}

	public void setUri(URI uri)
	{
		_uri = uri;
	}

	public URI getTelUri()
	{
		return _telUri;
	}

	public void setTelUri(URI telUri)
	{
		_telUri = telUri;
	}
}
