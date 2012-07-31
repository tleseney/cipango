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
package org.cipango.test;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.sip.Proxy;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.TooManyHopsException;
import javax.servlet.sip.annotation.SipServlet;

import org.cipango.test.common.AbstractServlet;

@SuppressWarnings("serial")
@SipServlet (name="org.cipango.sipunit.test.DoubleRecordRouteTest")
public class DoubleRecordRouteServlet extends AbstractServlet
{

	@Override
	protected void doRequest(SipServletRequest request) throws ServletException, IOException,
			TooManyHopsException
	{
		try
		{
			if (request.isInitial())
			{
				Proxy proxy = request.getProxy();
				proxy.setRecordRoute(true);
				proxy.setSupervised(true);
				proxy.getRecordRouteURI().setParameter("test", "1");
				proxy.proxyTo(request.getRequestURI());
			}
			else
			{
				assertEquals("1", request.getPoppedRoute().getURI().getParameter("test"));
			}
		}
		catch (Throwable e) 
		{
			sendError(request, e);
		}
	}

	@Override
	protected void doResponse(SipServletResponse response) throws ServletException, IOException,
			TooManyHopsException
	{
		response.addHeader("mode", "proxy");
		if (response.getMethod().equals("BYE"))
		{
			response.getApplicationSession().invalidate();
		}
	}

}
