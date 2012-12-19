// ========================================================================
// Copyright 2007-2010 NEXCOM Systems
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
package org.cipango.tests.integration;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.sip.B2buaHelper;
import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.UAMode;
import javax.servlet.sip.annotation.SipServlet;

import org.cipango.tests.AbstractServlet;
import org.cipango.tests.MainServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
@SipServlet (name="org.cipango.tests.integration.B2bHelperForkTest")
public class B2bHelperForkServlet extends AbstractServlet
{
	private static final Logger __logger = LoggerFactory.getLogger(B2bHelperForkServlet.class);
	public static final String ROLE = "Role";
	public enum Role { SESSION_UAC_1, SESSION_UAC_2, SESSION_UAS_1, SESSION_UAS_2; }
	
	@Override
	protected void doRequest(SipServletRequest request) throws ServletException, IOException
	{
		try
		{			
			log(request);
			B2buaHelper helper = request.getB2buaHelper();
			SipServletRequest newRequest = null;
			if (request.isInitial())
			{
				request.getSession().setAttribute(ROLE, Role.SESSION_UAS_1);
				newRequest = helper.createRequest(request, true, null);
				newRequest.getSession().setHandler(getServletName());
				newRequest.getSession().setAttribute(ROLE, Role.SESSION_UAC_1);
				request.getApplicationSession().setAttribute(Role.SESSION_UAS_1.toString(), request.getSession());
				newRequest.getSession().setAttribute("test", "same");
				newRequest.setHeader(MainServlet.SERVLET_HEADER, B2bHelperProxyServlet.class.getName());
				newRequest.pushRoute(getOwnUri());
			}
			else
			{
				SipSession session = helper.getLinkedSession(request.getSession());
				if ("PRACK".equals(request.getMethod()))
				{
					List<SipServletMessage> l = helper.getPendingMessages(session, UAMode.UAC);
					for (SipServletMessage message : l)
					{
						if (message instanceof SipServletResponse && message.getMethod().equals("INVITE"))
						{
							SipServletResponse response = (SipServletResponse) message;
							newRequest = response.createPrack();
						}
					}
					if (newRequest == null)
						fail("Could not found UAC response for PRACK in pending messages: " + l);
				}
				else if ("ACK".equals(request.getMethod()))
				{
					List<SipServletMessage> l = helper.getPendingMessages(session, UAMode.UAC);
					SipServletResponse response = (SipServletResponse) l.get(0);
					newRequest = response.createAck();
				}
				else if ("CANCEL".equals(request.getMethod()))
				{
					newRequest = helper.createCancel(session);
				}
				else
				{
					if ("BYE".equals(request.getMethod()) &&  getServletContext().getAttribute(getFailureKey()) != null)
						checkForFailure(request);
					newRequest = helper.createRequest(session, request, null);
				}
			}
	
			newRequest.send();
		}
		catch (Throwable e) 
		{
			sendError(request, e);
		}
	}
	
	private void log(SipServletRequest request)
	{
		StringBuilder sb = new StringBuilder();
		sb.append(request.isInitial() ? "Starting test " : "continue test ");
		sb.append("B2bHelperTest.").append(request.getHeader(MainServlet.METHOD_HEADER));
		
		if (!request.isInitial())
			sb.append(" on " + request.getMethod() + " and session " + request.getSession().getAttribute(ROLE));
		__logger.info(sb.toString());
		
	}
	
	private void log(SipServletResponse response) throws ServletParseException
	{
		StringBuilder sb = new StringBuilder();
		sb.append("Continue test B2bHelperTest.").append(response.getHeader(MainServlet.METHOD_HEADER));
		sb.append(" on " + response.getStatus() + "/" + response.getMethod());
		sb.append(" and session " + response.getSession().getAttribute(ROLE));
		sb.append(" from " + getRemote(response));
		__logger.info(sb.toString());
		
	}

	@Override
	protected void doResponse(SipServletResponse response)
	{
		try
		{
			log(response);
			
			B2buaHelper helper = response.getRequest().getB2buaHelper();
			SipSession sessionUas = helper.getLinkedSession(response.getSession());
			
			String method = response.getMethod();
			if (("INVITE".equals(method) && response.getStatus() == SipServletResponse.SC_REQUEST_TERMINATED)
					|| "CANCEL".equals(method))
				return;
			
			if (sessionUas == null)
				__logger.warn("Could not found UAS session for response:\n" + response);		
			
			boolean checkSessions = false;
			
			SipServletResponse responseUas = null;
			if (response.getRequest().isInitial())
			{
				responseUas = helper.createResponseToOriginalRequest(
							sessionUas, response.getStatus(), response.getReasonPhrase() + " leg UAS");
				if (!response.getApplicationSession().getAttribute(Role.SESSION_UAS_1.toString()).equals(sessionUas))
				{
					response.getSession().setAttribute(ROLE, Role.SESSION_UAC_2);
					responseUas.getSession().setAttribute(ROLE, Role.SESSION_UAS_2);
					checkSessions = true;
				}
			}
			else
			{
				List<SipServletMessage> l = helper.getPendingMessages(sessionUas, UAMode.UAS);
				for (SipServletMessage message : l)
				{
					if (message instanceof SipServletRequest && message.getMethod().equals(response.getMethod()))
					{
						SipServletRequest requestUas = (SipServletRequest) message;
						responseUas = requestUas.createResponse(response.getStatus(), response.getReasonPhrase() + " leg UAS");
					}
				}
				if (responseUas == null)
					fail("Could not found UAS request for " + response.getStatus() + "/" + response.getMethod() 
							+ " in pending messages: " + l);
			}
			
			if (responseUas.getStatus() == SipServletResponse.SC_SESSION_PROGRESS)
				responseUas.sendReliably();
			else
				responseUas.send();
			
			if (checkSessions)
				checkSessions(response.getApplicationSession(), helper);
			
		}
		catch (Throwable e)
		{
			__logger.warn("Failed to handle:\n" + response, e);
			sendError(response, e);
		}
	}
	
	private void checkSessions(SipApplicationSession appSession, B2buaHelper helper)
	{
		SipSession sessionUac1 = null, sessionUac2 = null, sessionUas1 = null, sessionUas2 = null;
		@SuppressWarnings("rawtypes")
		Iterator it = appSession.getSessions("sip");
		while (it.hasNext())
		{
			SipSession session = (SipSession) it.next();
			Role role = (Role) session.getAttribute(ROLE);
			assertNotNull(role);
			switch (role)
			{
			case SESSION_UAC_1:
				sessionUac1 = session;
				break;
			case SESSION_UAC_2:
				sessionUac2 = session;
				break;
			case SESSION_UAS_1:
				sessionUas1 = session;
				break;
			case SESSION_UAS_2:
				sessionUas2 = session;
				break;
			default:
				fail("Unknown role");
				break;
			}
		}
		
		assertNotNull(sessionUas1);
		assertNotNull(sessionUas2);
		assertNotNull(sessionUac1);
		assertNotNull(sessionUac2);
		
		assertNotSame(sessionUac1, sessionUac2);
		assertNotSame(sessionUac1.getId(), sessionUac2.getId());
		
		assertEquals(sessionUac1.getCallId(), sessionUac2.getCallId());
		assertEquals("same", sessionUac2.getAttribute("test"));

		sessionUac1.setAttribute("test2", "not same");
		assertNull(sessionUac2.getAttribute("test2"));
		
		assertNotSame(sessionUas1, sessionUas2);
		assertNotSame(sessionUas1.getId(), sessionUas2.getId());
		assertEquals(sessionUas1.getCallId(), sessionUas2.getCallId());
		assertEquals(sessionUac1.getCallId(), sessionUac2.getCallId());
		
		assertEquals(sessionUac1, helper.getLinkedSession(sessionUas1));
		assertEquals(sessionUac2, helper.getLinkedSession(sessionUas2));
		assertEquals(sessionUas1, helper.getLinkedSession(sessionUac1));
		assertEquals(sessionUas2, helper.getLinkedSession(sessionUac2));
		
		// tag check
		assertNotSame(sessionUac1.getRemoteParty().getParameter("tag"), sessionUac2.getRemoteParty().getParameter("tag"));
		assertEquals(sessionUac1.getLocalParty(), sessionUac2.getLocalParty());
		assertEquals(sessionUas1.getRemoteParty(), sessionUas2.getRemoteParty());
		assertEquals(sessionUas1.getLocalParty().getURI(), sessionUas2.getLocalParty().getURI());
		assertNotSame(sessionUas1.getLocalParty().getParameter("tag"), sessionUas2.getLocalParty().getParameter("tag"));
		assertNotNull(sessionUas2.getLocalParty().getParameter("tag"));
		assertNotNull(sessionUas2.getRemoteParty().getParameter("tag"));
		assertNotNull(sessionUac2.getLocalParty().getParameter("tag"));
		assertNotNull(sessionUac2.getRemoteParty().getParameter("tag"));
	}
	
	private String getRemote(SipServletResponse response) throws ServletParseException
	{
		if (response.getAddressHeader("Contact") != null)
		{
			SipURI uri = (SipURI) response.getAddressHeader("Contact").getURI();
			return uri.getUser();
		}
		return response.getReasonPhrase().substring(response.getReasonPhrase().lastIndexOf(' '));
	}
}
