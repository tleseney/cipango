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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.sip.Proxy;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.annotation.SipServlet;

import org.cipango.tests.SipServletTestCase;

@SipServlet(name="org.cipango.tests.replication.ProxyTest")
public class ProxyServlet extends SipServletTestCase
{
	private static final long serialVersionUID = 1L;

	protected void doRequest(SipServletRequest req) throws ServletException, IOException
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
			if ("INVITE".equals(req.getMethod()))
			{
				appSession.setAttribute(TimerServlet.SYS_UP_TIME, TimerServlet.__sysUpTime);
				Proxy proxy = req.getProxy();
				proxy.setRecordRoute(true);
				proxy.proxyTo(req.getRequestURI());
			}
			else if ("BYE".equals(req.getMethod()))
			{
				try
				{
					Long sysUpTime = (Long) appSession.getAttribute(TimerServlet.SYS_UP_TIME);
					assertNotNull(sysUpTime);
					assertNotSame("Cipango has not been restarted", TimerServlet.__sysUpTime, sysUpTime);
				}
				catch (Throwable e)
				{
					ByteArrayOutputStream os = new ByteArrayOutputStream();
			        PrintWriter pw = new PrintWriter(os);
			        e.printStackTrace(pw);
			        pw.flush();
			        byte[] content = os.toByteArray();
					req.setContent(content, "text/plain");
					req.setHeader("P-Failed", "true");
				}
				req.getApplicationSession().invalidate();

			}
		}
	}
}
