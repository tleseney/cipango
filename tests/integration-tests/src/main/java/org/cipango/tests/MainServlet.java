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
package org.cipango.tests;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
@javax.servlet.sip.annotation.SipServlet (name = "MainServlet")
public class MainServlet extends SipServletTestCase
{
	private final Logger _logger = LoggerFactory.getLogger(MainServlet.class);
	
	public static final String SERVLET_HEADER = "P-Servlet";
	public static final String METHOD_HEADER = "P-method";

	@Override
	protected void doRequest(SipServletRequest request) throws ServletException, IOException
	{
		try
		{
			String servlet = request.getHeader(SERVLET_HEADER);
			if (servlet == null) 
				throw new IllegalArgumentException("Missing header: " + SERVLET_HEADER);
			RequestDispatcher requestDispatcher = getServletContext().getNamedDispatcher(servlet);
			if (requestDispatcher == null)
				throw new IllegalStateException("Could not found servlet with name " + servlet);
			request.getSession().setHandler(servlet);
			requestDispatcher.forward(request, null);
		}		
		catch (Throwable e)
		{
			sendError(request, e);
		}
	}
	
	@Override
	protected void doResponse(SipServletResponse response)
	{
		_logger.warn("Received unexpected response on main servlet:\n" + response);
	}
}
