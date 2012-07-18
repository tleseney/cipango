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

import org.cipango.client.Authentication;
import org.cipango.client.Authentication.Digest;
import org.cipango.client.Call;
import org.cipango.client.SipHeaders;
import org.cipango.client.SipMethods;
import org.cipango.sipunit.CredentialsImpl;
import org.cipango.sipunit.UaTestCase;
import org.junit.Test;

public class AuthenticationTest extends UaTestCase
{
	class MutableDigest extends Digest
	{
		public MutableDigest(Digest digest)
		{
			_realm = digest.getRealm();
			_qop = digest.getQop();
			_nonce = digest.getNonce();
			_opaque = digest.getOpaque();
			_stale = String.valueOf(digest.isStale());
		}

		public void setNonce(String nonce)
		{
			_nonce = nonce;
		}
	}

	/**
	 * Check Digest authentication on a simple transaction.
	 * 
	 * <pre>
	 * Alice                         AS
	 *   |                            |
	 *   | MESSAGE                    |
	 *   |--------------------------->|
	 *   |                        401 |
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
        assertNotNull(response.getHeader("servlet"));
        
		@SuppressWarnings("unchecked")
		List<SipServletResponse> responses = (List<SipServletResponse>) response
				.getRequest().getAttribute(SipServletResponse.class.getName());
		assertEquals(2, responses.size());
		assertValid(responses.get(0), 401);
		assertSame(response, responses.get(1));
	}
	
	/**
	 * Check a forbidden method.
	 * 
	 * <pre>
	 * Alice                         AS
	 *   |                            |
	 *   | FORBIDDEN_METHOD           |
	 *   |--------------------------->|
	 *   |                        403 |
	 *   |<---------------------------|
	 * </pre>
	 */
	@Test
	public void testAuthForbidden() throws Exception
	{
		SipServletRequest request = _ua.createRequest("FORBIDDEN_METHOD", getTo());
		SipServletResponse response = _ua.sendSynchronous(request);
        assertValid(response, SipServletResponse.SC_FORBIDDEN);
	}
	
	/**
	 * Check Digest authentication with a wrong password.
	 * 
	 * <pre>
	 * Alice                         AS
	 *   |                            |
	 *   | MESSAGE                    |
	 *   |--------------------------->|
	 *   |                        401 |
	 *   |<---------------------------|
	 *   | MESSAGE                    |
	 *   |--------------------------->|
	 *   |                        403 |
	 *   |<---------------------------|
	 * </pre>
	 */
	@Test
	public void testInvalidPassword() throws Exception
	{
		_ua.addCredentials(new CredentialsImpl("Test", "user", "invalidPassword"));

		SipServletRequest request = _ua.createRequest(SipMethods.MESSAGE, getTo());
		SipServletResponse response = _ua.sendSynchronous(request);
        assertValid(response, SipServletResponse.SC_FORBIDDEN);
        
		@SuppressWarnings("unchecked")
		List<SipServletResponse> responses = (List<SipServletResponse>) response
				.getRequest().getAttribute(SipServletResponse.class.getName());
		assertEquals(2, responses.size());
		assertValid(responses.get(0), SipServletResponse.SC_UNAUTHORIZED);
		assertSame(response, responses.get(1));
	}

	/**
	 * Check Digest authentication using an invalid Nonce.
	 * 
	 * <pre>
	 * Alice                         AS
	 *   |                            |
	 *   | MESSAGE                    |
	 *   |--------------------------->|
	 *   |                        401 |
	 *   |<---------------------------|
	 *   | MESSAGE                    |
	 *   |--------------------------->|
	 *   |                        401 |
	 *   |<---------------------------|
	 * </pre>
	 */
	@Test
	public void testInvalidNonce() throws Exception
	{
		SipServletRequest request = _ua.createRequest(SipMethods.MESSAGE, getTo());
		SipServletResponse response = _ua.sendSynchronous(request);
        assertValid(response, SipServletResponse.SC_UNAUTHORIZED);
        
		MutableDigest digest = new MutableDigest(
				Authentication.createDigest(response.getHeader(SipHeaders.WWW_AUTHENTICATE)));
        digest.setNonce(digest.getNonce().substring(4) + "AAAA");
        Authentication auth = new Authentication(digest);

		request = _ua.customize(request.getSession().createRequest(SipMethods.MESSAGE));
		request.addHeader(SipHeaders.AUTHORIZATION, auth.authorize(
				request.getMethod(), request.getRequestURI().toString(),
				new CredentialsImpl("", "user", "password")));
        response = _ua.sendSynchronous(request);
        assertValid(response, SipServletResponse.SC_UNAUTHORIZED);
		assertTrue(Authentication.createDigest(
				response.getHeader(SipHeaders.WWW_AUTHENTICATE)).isStale());
	}

	/**
	 * Check Digest authentication on a complete dialog.
	 * 
	 * <pre>
	 * Alice                         AS
	 *   |                            |
	 *   | INVITE                     |
	 *   |--------------------------->|
	 *   |                        401 |
	 *   |<---------------------------|
	 *   | ACK                        |
	 *   |--------------------------->|
	 *   | INVITE                     |
	 *   |--------------------------->|
	 *   |                        200 |
	 *   |<---------------------------|
	 *   | ACK                        |
	 *   |--------------------------->|
	 *   | BYE                        |
	 *   |--------------------------->|
	 *   |                        401 |
	 *   |<---------------------------|
	 *   | BYE                        |
	 *   |--------------------------->|
	 *   |                        200 |
	 *   |<---------------------------|
	 * </pre>
	 */
	@Test
	public void testCall() throws Exception
	{
		_ua.addCredentials(new CredentialsImpl("Test", "user", "password"));
		
		Call call = _ua.createCall(_ua.getFactory().createURI(getTo()));
        assertValid(call.waitForFinalResponse());
        Thread.sleep(50);
		call.createAck().send();
		call.createRequest(SipMethods.BYE).send();
        assertValid(call.waitForResponse());
	}
}
