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
package org.cipango.tests.replication;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.sip.B2buaHelper;
import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.UAMode;
import javax.servlet.sip.annotation.SipServlet;

import org.cipango.tests.SipServletTestCase;


@SipServlet(name="org.cipango.tests.replication.B2bTest")
public class B2bServlet extends SipServletTestCase
{

	private static final long serialVersionUID = 1L;



	@Override
	protected void doRequest(SipServletRequest request) throws ServletException, IOException
	{
		try
		{
			B2buaHelper helper = request.getB2buaHelper();
			SipServletRequest newRequest;
			if (request.isInitial())
			{
				newRequest = helper.createRequest(request, true, null);
				SipSession uas = request.getSession();
				SipSession uac = newRequest.getSession();
				
				uas.setAttribute(TimerServlet.SYS_UP_TIME, TimerServlet.__sysUpTime);
				uac.setHandler(getServletName());
				uac.setAttribute(TimerServlet.SESSION, uas);
				uas.setAttribute(TimerServlet.SESSION, uac);
			}
			else
			{
				SipSession session = helper.getLinkedSession(request.getSession());
				assertNotNull(session);
				if ("ACK".equals(request.getMethod()))
				{
					List<SipServletMessage> l = helper.getPendingMessages(session, UAMode.UAC);
					assertTrue(l.size() == 1);
					SipServletResponse response = (SipServletResponse) l.get(0);
					newRequest = response.createAck();
					
					System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
					System.out.println("!       Cipango should be restarted now       !");
					System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
				}
				else
				{
					newRequest = helper.createRequest(session, request, null);
					if ("BYE".equals(request.getMethod()))
					{					
						Long sysUpTime = (Long) request.getSession().getAttribute(TimerServlet.SYS_UP_TIME);
						assertNotNull(sysUpTime);
						assertNotSame("Cipango have not have been restarted", TimerServlet.__sysUpTime, sysUpTime);
						assertEquals(session, request.getSession().getAttribute(TimerServlet.SESSION));
						assertEquals(request.getSession(), session.getAttribute(TimerServlet.SESSION));
					}
				}
			}

			newRequest.send();
		}
		catch (Throwable e)
		{
			sendError(request, e);
		}
	}



	protected void doResponse(SipServletResponse response)
	{
		try
		{
			B2buaHelper helper = response.getRequest().getB2buaHelper();
			SipServletRequest linkedRequest = helper
					.getLinkedSipServletRequest(response.getRequest());
			SipServletResponse newResponse = linkedRequest.createResponse(
					response.getStatus(), response.getReasonPhrase());
			newResponse.send();
		}
		catch (Throwable e)
		{
			log("Failed to handle:\n" + response, e);
		}
	}
	

}
