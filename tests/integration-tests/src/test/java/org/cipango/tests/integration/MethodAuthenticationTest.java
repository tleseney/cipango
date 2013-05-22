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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.cipango.tests.matcher.SipMatchers.*;

import java.util.List;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.cipango.client.test.CredentialsImpl;
import org.cipango.tests.UaTestCase;
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
		assertThat(response, isSuccess());
        
		@SuppressWarnings("unchecked")
		List<SipServletResponse> responses = (List<SipServletResponse>) response
				.getRequest().getAttribute(SipServletResponse.class.getName());
		assertThat(responses.size(), is(2));
		assertThat(responses.get(0), hasStatus(SipServletResponse.SC_UNAUTHORIZED));
		assertThat(responses.get(1), is(sameInstance(response)));
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
		_ua.addCredentials(new CredentialsImpl("Test", "manager", "password"));

		SipServletRequest request = _ua.createRequest("AUTH_METHOD", getTo());
		SipServletResponse response = _ua.sendSynchronous(request);
        assertThat(response, hasStatus(SipServletResponse.SC_FORBIDDEN));
        assertThat(
        		"Ensure that the user 'manager' with password 'password' is defined and is NOT associated with role user",
        		response.getReasonPhrase(), equalTo("!role"));
        
		@SuppressWarnings("unchecked")
		List<SipServletResponse> responses = (List<SipServletResponse>) response
				.getRequest().getAttribute(SipServletResponse.class.getName());
		assertThat(responses.size(), is(2));
		assertThat(responses.get(0), hasStatus(SipServletResponse.SC_UNAUTHORIZED));
		assertThat(responses.get(1), is(sameInstance(response)));
	}
}
