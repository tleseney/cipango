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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.TimerService;
import javax.servlet.sip.annotation.SipServlet;

import org.cipango.tests.SipServletTestCase;

@SipServlet(name="org.cipango.tests.replication.UasTest")
public class UasServlet extends SipServletTestCase
{

	private static final long serialVersionUID = 1L;
	private TimerService _timerService;
	private SipFactory _sipFactory;

	public void init()
	{
		_timerService = (TimerService) getServletContext().getAttribute(TIMER_SERVICE);
		_sipFactory = (SipFactory) getServletContext().getAttribute(SIP_FACTORY);
	}


	protected void doRequest(SipServletRequest req) throws ServletException,
			IOException
	{
		SipApplicationSession appSession = req.getApplicationSession();
		if ("ACK".equals(req.getMethod()))
		{
			System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			System.out.println("!       Cipango should be restarted now       !");
			System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		}
		else
		{
			SipServletResponse resp = null;
			if ("INVITE".equals(req.getMethod()))
			{
				resp = req.createResponse(180);
				appSession.setAttribute("INVITE", req);
				Custom custom = new Custom(5);
				custom.setAddr(req.getFrom());
				custom.setUri(_sipFactory.createURI("http://cipango.org;param=1"));
				custom.setAddr(req.getFrom());
				custom.setTelUri(_sipFactory.createURI("tel:1234;phone-context=cipango.org"));
				appSession.setAttribute("custom", custom);
				appSession.setAttribute(TimerServlet.SYS_UP_TIME, TimerServlet.__sysUpTime);
				
				appSession.setAttribute(TimerServlet.SESSION, req.getSession());
				req.getSession().setAttribute("appSession", appSession);
				
				
				_timerService.createTimer(appSession, 8000,
						true, new Timeout());				
			}
			else if ("BYE".equals(req.getMethod()))
			{
				try
				{
					Custom custom = (Custom) appSession.getAttribute("custom");
					//System.out.println(custom);
					Long sysUpTime = (Long) appSession.getAttribute(TimerServlet.SYS_UP_TIME);
					assertNotNull(sysUpTime);
					assertNotSame("Cipango have not have been restarted", TimerServlet.__sysUpTime, sysUpTime);
					
					assertTrue(custom.getNbPassivate() >= 2);
					assertEquals(1, custom.getNbActivate());
					
					assertEquals(req.getFrom(), custom.getAddr());
					assertEquals(_sipFactory.createURI("tel:1234;phone-context=cipango.org"), custom.getTelUri());
					assertEquals(_sipFactory.createURI("http://cipango.org;param=1"), custom.getUri());
					
					SipSession session = (SipSession) appSession.getAttribute(TimerServlet.SESSION);
					assertNotNull(session);
					assertEquals(req.getSession(), session);
					assertEquals(appSession, session.getAttribute("appSession"));
					
					resp = req.createResponse(200);
				}
				catch (Throwable e) 
				{
					sendError(req, e);
				}
				req.getApplicationSession().invalidate();
				
			}
			if (resp != null)
				resp.send();
		}
	}
	
	public static class Timeout implements TimerTimeout
	{		
		private static final long serialVersionUID = 1L;

		public void run(SipApplicationSession appSession) throws Exception
		{
			SipServletRequest req = (SipServletRequest) appSession.getAttribute("INVITE");
			SipServletResponse response;
			try
			{
				response = req.createResponse(200);
				Custom custom = (Custom) appSession.getAttribute("custom");
				assertEquals(0, custom.getNbPassivate());
				assertEquals(0, custom.getNbActivate());
			}
			catch (Throwable e)
			{
				response = req.createResponse(500, e.getMessage());
				ByteArrayOutputStream os = new ByteArrayOutputStream();
		        PrintWriter pw = new PrintWriter(os);
		        e.printStackTrace(pw);
		        pw.flush();
		        byte[] content = os.toByteArray();
		        response.setContent(content, "text/plain");
			}

			response.send();
		}	
		
		
	}

}
