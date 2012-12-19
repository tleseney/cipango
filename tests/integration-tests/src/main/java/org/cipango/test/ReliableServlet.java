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

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.annotation.SipServlet;

import org.cipango.test.common.AbstractServlet;

@SuppressWarnings("serial")
@SipServlet(name="org.cipango.sipunit.test.ReliableTest")
public class ReliableServlet extends AbstractServlet
{


	public void test100Rel(SipServletRequest request) throws ServletException, IOException
	{
		String method = request.getMethod();
		if ("INVITE".equals(method))
		{
			SipServletResponse response = request.createResponse(SipServletResponse.SC_SESSION_PROGRESS);
			request.getSession().setAttribute("INVITE", request);
			response.sendReliably();
		}
		else if ("PRACK".equals(method))
		{
			SipServletResponse response = request.createResponse(SipServletResponse.SC_OK);
			response.send();

			SipServletRequest invite = (SipServletRequest) request.getSession().getAttribute("INVITE");
			if (request.getSession().getAttribute("FirstPrack") == null)
			{
				request.getSession().setAttribute("FirstPrack", "true");
				invite.createResponse(SipServletResponse.SC_SESSION_PROGRESS).sendReliably();
			}
			else
			{
				try { Thread.sleep(200); } catch (InterruptedException e){}
				invite.createResponse(SipServletResponse.SC_OK).send();
			}
		}
		else if ("ACK".equals(method))
		{
			
		}
		else if ("BYE".equals(method))
		{
			checkForFailure(request);
			request.getApplicationSession().invalidate();
		}
	}
	
	public void testLatePrackAnswer(SipServletRequest request) throws ServletException, IOException
	{
		String method = request.getMethod();
		if ("INVITE".equals(method))
		{
			SipServletResponse response = request.createResponse(SipServletResponse.SC_SESSION_PROGRESS);
			request.getSession().setAttribute("INVITE", request);
			response.sendReliably();
		}
		else if ("PRACK".equals(method))
		{
			request.getSession().setAttribute("PRACK", request);
			SipServletRequest invite = (SipServletRequest) request.getSession().getAttribute("INVITE");
			invite.createResponse(SipServletResponse.SC_OK).send();
		}
		else if ("ACK".equals(method))
		{
			SipServletRequest prack = (SipServletRequest) request.getSession().getAttribute("PRACK");
			SipServletResponse response = prack.createResponse(SipServletResponse.SC_OK);
			response.send();
		}
		else if ("BYE".equals(method))
		{
			checkForFailure(request);
			request.getApplicationSession().invalidate();
		}
	}
	
	public void test100RelUac(SipServletRequest request) throws Exception
	{
		String method = request.getMethod();
		
		request.createResponse(SipServletResponse.SC_OK).send();
		request.getApplicationSession().invalidate();
		
		if ("REGISTER".equals(method))
		{
			Thread.sleep(200);	
			
			SipServletRequest invite = getSipFactory().createRequest(getSipFactory().createApplicationSession(),
					"INVITE", request.getTo(), request.getFrom());
			SipSession session = invite.getSession();
			session.setHandler(getServletName());
			invite.setRequestURI(request.getAddressHeader("Contact").getURI());
			invite.addHeader("Supported", "100rel");
			invite.send();
		}
	}
	
	public void test100RelUac(SipServletResponse response) throws Exception
	{
		String method = response.getMethod();
		if ("INVITE".equals(method))
		{
			if (response.getStatus() == SipServletResponse.SC_SESSION_PROGRESS)
				response.createPrack().send();
			else // 200
				response.createAck().send();
		}
	}

	public void test100RelUacLatePrackAnswer(SipServletRequest request) throws Exception
	{
		test100RelUac(request);
	}

	public void test100RelUacLatePrackAnswer(SipServletResponse response) throws Exception
	{
		test100RelUac(response);
	}
}
