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
package org.cipango.tests.integration;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.annotation.SipServlet;

import org.cipango.tests.SipServletTestCase;

/*
 * This servlet is declared using sip.xml
 */
@SuppressWarnings("serial")
@SipServlet (name="org.cipango.tests.integration.ProxyAuthenticationTest")
public class ProxyAuthenticationServlet extends SipServletTestCase
{


	public void doRequest(SipServletRequest request) throws ServletException, IOException
	{
		String method = request.getMethod();
		
		assertEquals("user", request.getUserPrincipal().getName());
		assertEquals("user", request.getRemoteUser());
		assertTrue(request.isUserInRole("FOO")); // due to security-role-ref
		assertFalse(request.isUserInRole("admin"));
		
		if ("FORBIDDEN_METHOD".equals(method))
		{
			fail("method FORBIDDEN_METHOD is forbidden");
		}
		else 
			request.createResponse(SipServletResponse.SC_OK).send();
	}
	

}
