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
package org.cipango.test;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.URI;
import javax.servlet.sip.annotation.SipServlet;

import org.cipango.test.common.AbstractServlet;

@SipServlet (name="org.cipango.sipunit.test.UacTest")
public class UacServlet extends AbstractServlet
{

	public void testReInvite(SipServletRequest request) throws Throwable
	{
		String method = request.getMethod();
		if ("MESSAGE".equals(method))
		{
			request.createResponse(SipServletResponse.SC_OK).send();
			Thread.sleep(200);
			
			SipServletRequest invite = getSipFactory().createRequest(request.getApplicationSession(),
					"INVITE", request.getTo(), request.getFrom());
			SipSession session = invite.getSession();
			session.setHandler(getServletName());
			invite.setRequestURI(request.getAddressHeader("Contact").getURI());
			invite.send();
		}
		else if ("BYE".equals(method))
		{
			request.createResponse(SipServletResponse.SC_OK).send();
			request.getApplicationSession().invalidate();
		}
	}
	
	public void testReInvite(SipServletResponse response) throws Throwable
	{
		if (response.getStatus() < 200)
			return;
		
		response.createAck().send();
		if (response.getRequest().getAttribute("second") == null)
		{
			Thread.sleep(100);
			SipServletRequest request = response.getSession().createRequest("INVITE");
			request.setAttribute("second", true);
			request.send();
		}
	}
	
	public void testBigRequestFallback(SipServletRequest request) throws Throwable
	{
		String method = request.getMethod();
		if ("MESSAGE".equals(method))
		{
			request.createResponse(SipServletResponse.SC_OK).send();
			Thread.sleep(20);
			
			SipServletRequest invite = getSipFactory().createRequest(request.getApplicationSession(),
					"INVITE", request.getTo(), request.getFrom());
			SipSession session = invite.getSession();
			session.setHandler(getServletName());
			URI contact = request.getAddressHeader("Contact").getURI().clone();
			contact.removeParameter("transport");
			invite.setRequestURI(contact);
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < 10; i++)
			{
				for (int j = 0; j < 150; j++)
					sb.append(String.valueOf(i));
				sb.append("\n");
			}
			invite.setContent(sb.toString().getBytes(), "text/plain");
			invite.send();
		}
		else if ("BYE".equals(method))
		{
			request.createResponse(SipServletResponse.SC_OK).send();
			request.getApplicationSession().invalidate();
		}
	}
	
	public void testBigRequestFallback(SipServletResponse response) throws Throwable
	{
		response.createAck().send();
	}
}
