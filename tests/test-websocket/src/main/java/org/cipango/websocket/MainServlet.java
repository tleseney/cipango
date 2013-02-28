// ========================================================================
// Copyright 2011 NEXCOM Systems
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
package org.cipango.websocket;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.URI;

public class MainServlet extends SipServlet
{

	@Override
	protected void doRequest(SipServletRequest request) throws ServletException, IOException
	{
		String user = getUser(request);
		RequestDispatcher dispatcher = getServletContext().getNamedDispatcher(user);
		if (dispatcher != null)
		{
			request.getSession().setHandler(user);
			dispatcher.forward(request, null);
		}
		else
			request.createResponse(SipServletResponse.SC_NOT_FOUND).send();
	}
	
	public String getUser(SipServletRequest origRequest)
	{
		URI uri = origRequest.getTo().getURI();
		if (uri.isSipURI())
			return ((SipURI) uri).getUser();
		return null;
	}

}
