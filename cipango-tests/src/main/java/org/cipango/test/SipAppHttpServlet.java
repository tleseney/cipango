// ========================================================================
// Copyright 2007-2008 NEXCOM Systems
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
import java.util.Iterator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.sip.ConvergedHttpSession;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SipAppHttpServlet extends HttpServlet
{
	private static final Logger __logger = LoggerFactory.getLogger(SipAppHttpServlet.class);

	@SuppressWarnings("unchecked")
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse resp) throws ServletException,
			IOException
	{
		__logger.info("Got HTTP GET {}", request.getRequestURI());
		SipApplicationSession appSession = null;
		try 
		{
			String method = request.getSession(false).getAttribute("first") == null ? "INFO" : "BYE";
			request.getSession().setAttribute("first", "false");
			
			appSession = ((ConvergedHttpSession) request.getSession()).getApplicationSession(); 
			Iterator<SipSession> it = (Iterator<SipSession>) appSession.getSessions("SIP");
			if (!it.hasNext())
				throw new IllegalArgumentException("No SIP sessions found in SipApplicationSession " + appSession.getId());
			SipSession session = it.next();
			if (it.hasNext())
				throw new IllegalArgumentException("Multiple SIP sessions found in SipApplicationSession");

			session.createRequest(method).send();
			resp.getWriter().print(method + " Request sent on session " + session);
		}
		catch (Throwable e) 
		{
			e.printStackTrace(resp.getWriter());
			resp.setStatus(500);
		}
	}
	
	
}
