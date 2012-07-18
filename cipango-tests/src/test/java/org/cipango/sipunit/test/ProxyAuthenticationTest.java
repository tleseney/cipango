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

import org.cipango.client.SipMethods;
import org.cipango.sipunit.CredentialsImpl;
import org.cipango.sipunit.UaTestCase;
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
        assertValid(response);
        
		@SuppressWarnings("unchecked")
		List<SipServletResponse> responses = (List<SipServletResponse>) response
				.getRequest().getAttribute(SipServletResponse.class.getName());
		assertEquals(2, responses.size());
		assertValid(responses.get(0), SipServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED);
		assertSame(response, responses.get(1));
	}
}
