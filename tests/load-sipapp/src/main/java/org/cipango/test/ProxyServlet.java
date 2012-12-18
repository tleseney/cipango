// ========================================================================
// Copyright 2008-2010 NEXCOM Systems
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
package org.cipango.test;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.sip.Proxy;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

public class ProxyServlet extends SipServlet
{

	protected void doRequest(SipServletRequest req) throws ServletException, IOException
	{
		if (req.isInitial())
		{
			Proxy proxy = req.getProxy();
			proxy.setRecordRoute(true);
			proxy.setSupervised(true);

			proxy.proxyTo(req.getRequestURI());
		}
	}

	protected void doResponse(SipServletResponse response)
	{
		if (response.getMethod().equals("BYE"))
		{
			response.getApplicationSession().invalidate();
		}
	}
}
