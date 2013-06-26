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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;

import javax.servlet.ServletException;
import javax.servlet.sip.ServletTimer;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.TimerService;

import org.cipango.tests.SipServletTestCase;

@javax.servlet.sip.annotation.SipServlet(name="org.cipango.tests.TimerTest", loadOnStartup=1)
public class TimerServlet extends SipServletTestCase
{

	private static final long serialVersionUID = 1L;
	private TimerService _timerService;
	public static Long __sysUpTime;
	public static final String SYS_UP_TIME = "sysUpTime";
	public static final String SESSION = "SESSION";

	public void init()
	{
		_timerService = (TimerService) getServletContext().getAttribute(
				TIMER_SERVICE);
		__sysUpTime = System.currentTimeMillis();
	}

	protected void doRequest(SipServletRequest req1) throws ServletException,
			IOException
	{
		if ("ACK".equals(req1.getMethod()))
		{
			System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			System.out.println("!       Cipango should be restarted now       !");
			System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		}
		else
		{

			SipServletResponse resp;
			if ("INVITE".equals(req1.getMethod()))
			{
				resp = req1.createResponse(200);
				req1.getApplicationSession().setAttribute(SESSION, req1.getSession());
				req1.getSession().setAttribute("custom", new Custom(3));
				ServletTimer timer = _timerService.createTimer(req1.getApplicationSession(), 50000,
						true, new Timeout());
				req1.getSession().setAttribute("timer", timer);
				req1.getSession().setAttribute(SYS_UP_TIME, __sysUpTime);
			}
			else if (!"UPDATE".equals(req1.getMethod()))
			{
				req1.getApplicationSession().invalidate();
				resp = req1.createResponse(200);
			}
			else
			{
				resp = req1.createResponse(200);
			}
			resp.send();
		}
	}
	
	public void destroy()
	{
		System.out.println("Destroy");
		log("Destroy UAS");
	}
	
	public static class Timeout implements TimerTimeout
	{		

		private static final long serialVersionUID = 1L;

		public void run(SipApplicationSession appSession) throws IOException, InterruptedException
		{
			SipSession session = (SipSession) appSession.getAttribute(SESSION);
			SipServletRequest request = session.createRequest("BYE");
			try
			{
				Iterator<?> it = appSession.getSessions();
				
				assertTrue(it.hasNext());

				SipSession other = (SipSession) it.next();
				assertEquals(session, other);
				assertFalse(it.hasNext());
				ServletTimer timer = (ServletTimer) session.getAttribute("timer");
				
				assertNotNull("No timer", timer);
				
				assertEquals(timer.getInfo(), this);
				
				Custom custom = (Custom) session.getAttribute("custom");
				
				assertNotNull(custom);
				
				request.setHeader("custom-attr", String.valueOf(custom));
				
				Long sysUpTime = (Long) session.getAttribute(SYS_UP_TIME);
				assertNotNull(sysUpTime);
				assertNotSame("Cipango have not have been restarted", TimerServlet.__sysUpTime, sysUpTime);
				
				assertTrue(custom.getNbPassivate() >= 2);
				assertEquals(3, custom.getParam());
				assertEquals("Cipango have not have been restarted or Activation listener does not work", 1, custom.getNbActivate());
				
				request.setHeader("timer", String.valueOf(timer));
				request.setHeader("session", session.toString());
			}
			catch (Throwable e)
			{
				ByteArrayOutputStream os = new ByteArrayOutputStream();
		        PrintWriter pw = new PrintWriter(os);
		        e.printStackTrace(pw);
		        pw.flush();
		        byte[] content = os.toByteArray();
		        request.setContent(content, "text/plain");
			}

			request.send();
			appSession.invalidate();
		}	
		
		
	}

}
