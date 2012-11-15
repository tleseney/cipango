// ========================================================================
// Copyright 2010 NEXCOM Systems
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
package org.cipango.dar;

import javax.servlet.sip.ar.SipApplicationRoutingRegion;
import javax.servlet.sip.ar.SipRouteModifier;

public class RouterInfo
{
	private String _name;
	private String _identity;
	private SipApplicationRoutingRegion _region;
	private String _uri;
	private SipRouteModifier _routeModifier;

	public RouterInfo(String name, String identity, SipApplicationRoutingRegion region, String uri, SipRouteModifier routeModifier)
	{
		_name = name;
		_identity = identity;
		_region = region;
		_uri = uri;
		_routeModifier = routeModifier;
	}
	
	public String getUri()
	{
		return _uri;
	}

	public SipRouteModifier getRouteModifier()
	{
		return _routeModifier;
	}

	public String getName()
	{
		return _name;
	}

	public String getIdentity()
	{
		return _identity;
	}

	public SipApplicationRoutingRegion getRegion()
	{
		return _region;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append('(');
		sb.append('"').append(getName()).append("\", ");
		sb.append('"').append(getIdentity()).append("\", ");
		sb.append('"').append(getRegion().getType()).append("\", ");
		sb.append('"').append(getUri()).append("\", ");
		sb.append('"').append(getRouteModifier()).append("\", ");
		sb.append('"').append('"');
		sb.append(')');
		return sb.toString();
	}
}
