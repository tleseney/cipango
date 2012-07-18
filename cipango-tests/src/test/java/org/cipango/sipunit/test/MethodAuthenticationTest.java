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
package org.cipango.sipunit.test;

import java.util.List;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.cipango.sipunit.CredentialsImpl;
import org.cipango.sipunit.UaTestCase;
import org.junit.Test;

public class MethodAuthenticationTest extends UaTestCase
{

	/**
	 * The request AUTH_METHOD MUST be challenged.
	 * 
	 * <pre>
	 * Alice                         AS
	 *   |                            |
	 *   | AUTH_METHOD                |
	 *   |--------------------------->|
	 *   |                        401 |
	 *   |<---------------------------|
	 *   | AUTH_METHOD                |
	 *   |--------------------------->|
	 *   |                        200 |
	 *   |<---------------------------|
	 * </pre>
	 */
	@Test
	public void testAuthSucceed() throws Exception
	{
		_ua.addCredentials(new CredentialsImpl("Test", "user", "password"));

		SipServletRequest request = _ua.createRequest("AUTH_METHOD", getTo());
		SipServletResponse response = _ua.sendSynchronous(request);
        assertValid(response);
        
		@SuppressWarnings("unchecked")
		List<SipServletResponse> responses = (List<SipServletResponse>) response
				.getRequest().getAttribute(SipServletResponse.class.getName());
		assertEquals(2, responses.size());
		assertValid(responses.get(0), SipServletResponse.SC_UNAUTHORIZED);
		assertSame(response, responses.get(1));
	}

	/**
	 * The request MESSAGE MUST not be challenged.
	 * 
	 * <pre>
	 * Alice                         AS
	 *   |                            |
	 *   | MESSAGE                    |
	 *   |--------------------------->|
	 *   |                        200 |
	 *   |<---------------------------|
	 * </pre>
	 */
	@Test
	public void testNoAuth() throws Exception
	{		
		sendAndAssertMessage();
	}
	
	/**
	 * The request AUTH_METHOD MUST be restricted to the role user.
	 * 
	 * <pre>
	 * Alice                         AS
	 *   |                            |
	 *   | AUTH_METHOD                |
	 *   |--------------------------->|
	 *   |                        401 |
	 *   |<---------------------------|
	 *   | AUTH_METHOD                |
	 *   |--------------------------->|
	 *   |                        403 |
	 *   |<---------------------------|
	 * </pre>
	 */
	@Test
	public void testRole() throws Exception
	{		
		_ua.addCredentials(new CredentialsImpl("Test", "manager", "invalidPassword"));

		SipServletRequest request = _ua.createRequest("AUTH_METHOD", getTo());
		SipServletResponse response = _ua.sendSynchronous(request);
        assertValid(response, SipServletResponse.SC_FORBIDDEN);
        assertEquals("Ensure that the user 'manager' with password 'password' is defined and is NOT associated with role user",
        		"!role", response.getReasonPhrase());
        
		@SuppressWarnings("unchecked")
		List<SipServletResponse> responses = (List<SipServletResponse>) response
				.getRequest().getAttribute(SipServletResponse.class.getName());
		assertEquals(2, responses.size());
		assertValid(responses.get(0), SipServletResponse.SC_UNAUTHORIZED);
		assertSame(response, responses.get(1));
	}

	
}
