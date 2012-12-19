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

import java.util.List;

import javax.servlet.sip.B2buaHelper;
import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.UAMode;
import javax.servlet.sip.annotation.SipServlet;

import org.cipango.test.common.AbstractServlet;

@SuppressWarnings("serial")
@SipServlet (name="org.cipango.sipunit.test.B2bHelperTest")
public class B2bHelperServlet extends AbstractServlet
{
	
	public void testCancel(SipServletRequest request) throws Exception
	{
		//System.out.println("Session: " + request.getSession() +  " on request: " + request);
		B2buaHelper helper = request.getB2buaHelper();
		SipServletRequest newRequest = null;
		if (request.isInitial())
		{
			request.getSession().setInvalidateWhenReady(false);
			newRequest = helper.createRequest(request, true, null);
			newRequest.getSession().setHandler(getServletName());
			newRequest.getSession().setInvalidateWhenReady(false);
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
					fail("Could not find UAC response for PRACK in pending messages: " + l);
			}
			else if ("CANCEL".equals(request.getMethod()))
			{
				newRequest = helper.createCancel(session);
			}
		}

		newRequest.send();
	}
	
	public void testCancel(SipServletResponse response) throws Exception
	{
		//System.out.println("Session: " + response.getSession() +  " on Response: " + response);
		B2buaHelper helper = response.getRequest().getB2buaHelper();
		SipSession sessionUas = helper.getLinkedSession(response.getSession());
		
		String method = response.getMethod();
		if (("INVITE".equals(method) && response.getStatus() == SipServletResponse.SC_REQUEST_TERMINATED)
				|| "CANCEL".equals(method))
			return;
						
		SipServletResponse responseUas = null;
		if (response.getRequest().isInitial())
		{
			responseUas = helper.createResponseToOriginalRequest(
						sessionUas, response.getStatus(), response.getReasonPhrase() + " leg UAS");
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
		
	}

	
	public void testEarlyCancel(SipServletRequest request) throws Exception
	{
		//System.out.println("Session: " + request.getSession() +  " on request: " + request);
		B2buaHelper helper = request.getB2buaHelper();
		SipServletRequest newRequest = null;
		if (request.isInitial())
		{
			request.getSession().setInvalidateWhenReady(false);
			newRequest = helper.createRequest(request, true, null);
			newRequest.getSession().setHandler(getServletName());
			newRequest.getSession().setInvalidateWhenReady(false);
		} 
		else
		{
			SipSession session = helper.getLinkedSession(request.getSession());
			if ("CANCEL".equals(request.getMethod()))
			{
				newRequest = helper.createCancel(session);
			}
		}

		newRequest.send();
	}
	
	public void testEarlyCancel(SipServletResponse response) throws Exception
	{
		//System.out.println("Session: " + response.getSession() +  " on Response: " + response);
		B2buaHelper helper = response.getRequest().getB2buaHelper();
		SipSession sessionUas = helper.getLinkedSession(response.getSession());
		
		String method = response.getMethod();
		if (("INVITE".equals(method) && response.getStatus() == SipServletResponse.SC_REQUEST_TERMINATED)
				|| "CANCEL".equals(method))
			return;
						
		SipServletResponse responseUas = null;
		if (response.getRequest().isInitial())
		{
			responseUas = helper.createResponseToOriginalRequest(
						sessionUas, response.getStatus(), response.getReasonPhrase() + " leg UAS");
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
		
		responseUas.send();
		
	}

	
}