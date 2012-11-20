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

@SuppressWarnings("serial")
@SipServlet (name="org.cipango.sipunit.test.TcpTest")
public class TcpServlet extends AbstractServlet
{

	public void testBigRequest(SipServletRequest request) throws Throwable
	{
		String method = request.getMethod();
		if ("REGISTER".equals(method))
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
	
	public void testBigRequest(SipServletResponse response) throws Throwable
	{
		response.createAck().send();
	}
}
