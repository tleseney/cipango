// ========================================================================
// Copyright 2007-2012 NEXCOM Systems
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

import static org.junit.Assert.*;
import java.util.Iterator;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.annotation.SipApplicationKey;
import javax.servlet.sip.annotation.SipServlet;

import org.cipango.tests.AbstractServlet;

@SuppressWarnings("serial")
@SipServlet (name="org.cipango.tests.integration.SipApplicationKeyTest")
public class SipApplicationKeyServlet extends AbstractServlet
{
	@SipApplicationKey
	public static String getApplicationKey(SipServletRequest request)
	{
		return request.getHeader("sipApplicationKey");
	}
	
	public void testApplicationKey(SipServletRequest request) throws Exception
	{
		if ("BYE".equals(request.getMethod()) || "ACK".equals(request.getMethod()))
		{
			doSubsequentRequest(request);
			return;
		}
		
		String mode = request.getHeader("mode");
		if (mode.equals("create"))
		{
			Iterator<?> it = request.getApplicationSession().getSessions();
			it.next();
			assertFalse("Found multiple sessions", it.hasNext());
			request.createResponse(SipServletResponse.SC_OK).send();
		}
		else if (mode.equals("join"))
		{
			Iterator<?> it = request.getApplicationSession().getSessions();
			SipSession other = null;
			while (it.hasNext())
			{
				SipSession session = (SipSession) it.next();
				if (!session.equals(request.getSession()))
				{
					assertNull("Found multiple sessions", other);
					other = session;
				}
			}
			assertNotNull("Found multiple sessions", other);
			request.getSession().setAttribute("invite", request);
			other.createRequest("BYE").send();
		}
		else
		{
			throw new IllegalStateException("Unknown mode: " + mode);
		}
	}
	
	public void testApplicationKey(SipServletResponse response) throws Exception
	{
		if ("BYE".equals(response.getMethod()))
		{
			Iterator<?> it = response.getApplicationSession().getSessions();
			SipSession other = null;
			while (it.hasNext())
			{
				SipSession session = (SipSession) it.next();
				if (!session.equals(response.getSession()))
				{
					assertNull("Found multiple sessions", other);
					other = session;
				}
			}
			assertNotNull("Found multiple sessions", other);
			SipServletRequest request = (SipServletRequest) other.getAttribute("invite");
			request.createResponse(SipServletResponse.SC_TEMPORARLY_UNAVAILABLE).send();
			request.getApplicationSession().invalidate();
		}
	}
	
	public void testApplicationKeyNoKey(SipServletRequest request) throws Exception
	{
		testApplicationKeyDifferent(request);
	}
	
	public void testApplicationKeyDifferent(SipServletRequest request) throws Exception
	{
		Iterator<?> it = request.getApplicationSession().getSessions();
		it.next();
		assertFalse("Found multiple sessions", it.hasNext());
				
		request.createResponse(SipServletResponse.SC_TEMPORARLY_UNAVAILABLE).send();
		request.getApplicationSession().setExpires(1);
	}
	
	public void testConcurrentApplicationKey(SipServletRequest request) throws Exception
	{
		request.getSession().setInvalidateWhenReady(false);
		String mode = request.getHeader("mode");
		if (mode.equals("create"))
		{
			request.createResponse(SipServletResponse.SC_RINGING).send();
			Iterator<?> it = request.getApplicationSession().getSessions();
			it.next();
			assertFalse("Found multiple sessions", it.hasNext());
			Thread.sleep(1000);
			request.createResponse(SipServletResponse.SC_TEMPORARLY_UNAVAILABLE).send();
		}
		else if (mode.equals("join"))
		{
			Iterator<?> it = request.getApplicationSession().getSessions();
			SipSession other = null;
			while (it.hasNext())
			{
				SipSession session = (SipSession) it.next();
				if (!session.equals(request.getSession()))
				{
					assertNull("Found multiple sessions", other);
					other = session;
				}
			}
			assertNotNull("Found multiple sessions", other);
			request.createResponse(SipServletResponse.SC_TEMPORARLY_UNAVAILABLE).send();
			request.getApplicationSession().invalidate();
		}
		else
			fail("Unknown mode: " + mode);
	}
	
	public void testConcurrentApplicationKey2(SipServletRequest request) throws Exception
	{
		String mode = request.getHeader("mode");
		if (mode.equals("create"))
		{
			Iterator<?> it = request.getApplicationSession().getSessions();
			it.next();
			assertFalse("Found multiple sessions", it.hasNext());
			Thread.sleep(1000);
			request.getSession().setAttribute("INVITE", request);
		}
		else if (mode.equals("join"))
		{
			Iterator<?> it = request.getApplicationSession().getSessions();
			SipSession other = null;
			while (it.hasNext())
			{
				SipSession session = (SipSession) it.next();
				if (!session.equals(request.getSession()))
				{
					assertNull("Found multiple sessions", other);
					other = session;
				}
			}
			assertNotNull("Found multiple sessions", other);
			
			if (request.getMethod().equals("INVITE"))
			{
				other.setAttribute("hasReceivedInvite", "true");
				Thread.sleep(1000);
			} 
			else if (request.getMethod().equals("CANCEL"))
			{
				SipServletRequest invite = (SipServletRequest) other.getAttribute("INVITE");
				assertNotNull("Could not found initial INVITE", invite);
				
				assertNotNull("Does not received INVITE request before CANCEL",
						other.getAttribute("hasReceivedInvite"));
				
				invite.createResponse(SipServletResponse.SC_TEMPORARLY_UNAVAILABLE).send();
				request.getApplicationSession().invalidate();
			}
			else
				fail("Unknown method " + request.getMethod());
		}
		else
			fail("Unknown mode: " + mode);
	}
	
	public void testCancel(SipServletRequest request) throws Exception
	{
		String method = request.getMethod();
		if ("INVITE".equals(method))
		{
			request.createResponse(SipServletResponse.SC_RINGING).send();
		}
		else if ("CANCEL".equals(method))
		{
			request.getApplicationSession().invalidate();
		}
	}
}
