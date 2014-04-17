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
package org.cipango.tests.authentication;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.cipango.client.test.matcher.SipMatchers.*;

import java.util.List;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.cipango.client.SipMethods;
import org.cipango.client.test.CredentialsImpl;
import org.cipango.tests.UaTestCase;
import org.junit.Test;

public class ProxyAuthenticationTest extends UaTestCase
{
	/**
	 * Check Digest proxy-authentication on a simple transaction.
	 * 
	 * <pre>
	 * Alice                         AS
	 *   |                            |
	 *   | MESSAGE                    |
	 *   |--------------------------->|
	 *   |                        407 |
	 *   |<---------------------------|
	 *   | MESSAGE                    |
	 *   |--------------------------->|
	 *   |                        200 |
	 *   |<---------------------------|
	 * </pre>
	 */
	@Test
	public void testAuthSucceed() throws Exception
	{
		_ua.addCredentials(new CredentialsImpl("Test", "user", "password"));

		SipServletRequest request = _ua.createRequest(SipMethods.MESSAGE, getTo());
		SipServletResponse response = _ua.sendSynchronous(request);
        assertThat(response, isSuccess());
        
		@SuppressWarnings("unchecked")
		List<SipServletResponse> responses = (List<SipServletResponse>) response
				.getRequest().getAttribute(SipServletResponse.class.getName());
		assertThat(responses.size(), is(2));
		assertThat(responses.get(0), hasStatus(SipServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED));
		assertThat(responses.get(1), is(sameInstance(response)));
	}
}
