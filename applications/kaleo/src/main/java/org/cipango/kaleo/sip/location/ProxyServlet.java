// ========================================================================
// Copyright 2008-2009 NEXCOM Systems
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
package org.cipango.kaleo.sip.location;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.cipango.kaleo.URIUtil;
import org.cipango.kaleo.location.Binding;
import org.cipango.kaleo.location.LocationService;

public class ProxyServlet extends SipServlet
{
	private LocationService _locationService;
	
	@Override
	public void init()
	{
		_locationService = (LocationService) getServletContext().getAttribute(LocationService.class.getName());
	}
	
	@Override
	protected void doInvite(SipServletRequest invite) throws ServletException, IOException
	{
		String target = URIUtil.toCanonical(invite.getRequestURI());
		
		List<Binding> bindings = _locationService.getBindings(target);
		
		if (bindings.size() == 0)
		{
			invite.createResponse(SipServletResponse.SC_NOT_FOUND).send();
			return;
		}
		
		Binding binding = bindings.get(0);
		
		invite.getSession().setHandler(getServletName());
		invite.getProxy().proxyTo(binding.getContact());
	}
}
