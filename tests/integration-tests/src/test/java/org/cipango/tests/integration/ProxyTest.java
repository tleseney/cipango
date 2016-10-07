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
package org.cipango.tests.integration;


import static org.cipango.client.test.matcher.SipMatchers.hasStatus;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.cipango.client.Call;
import org.cipango.client.MessageHandler;
import org.cipango.client.SipMethods;
import org.cipango.client.test.UaRunnable;
import org.cipango.client.test.UasScript;
import org.cipango.tests.UaTestCase;
import org.junit.Ignore;
import org.junit.Test;

public class ProxyTest extends UaTestCase
{
	/**
	 * <pre>
	 *  Alice        Proxy       Bob        Bob(2)       HSS
	 * 1  | INVITE     |          |           |           |
	 *    |----------->|          |           |           |
	 * 2  |            | INVITE   |           |           |
	 *    |            |--------->|           |           |
	 * 3  |            |      404 |           |           |
	 *    |            |<---------|           |           |
	 * 4  |            | ACK      |           |           |
	 *    |            |--------->|           |           |
	 * 5  |            | UDR      |           |           |
	 *    |            |--------------------------------->|
	 * 6  |            |          |           |       UDA |
	 *    |            |<---------------------------------|
	 * 7  |            | INVITE   |           |           |
	 *    |            |--------------------->|           |
	 * 8  |            |          |       200 |           |
	 *    |            |<---------------------|           |
	 * 9  |        200 |          |           |           |
	 *    |<-----------|          |           |           |
	 * 10 | ACK        |          |           |           |
	 *    |----------->|          |           |           |
	 * 11 |            | ACK      |           |           |
	 *    |            |--------------------->|           |
	 * 12 | BYE        |          |           |           |
	 *    |----------->|          |           |           |
	 * 13 |            | BYE      |           |           |
	 *    |            |--------------------->|           |
	 * 14 |            |          |       200 |           |
	 *    |            |<---------------------|           |
	 * 15 |        200 |          |           |           |
	 *    |<-----------|          |           |           |
	 * </pre>
	 */
	@Test
	public void testProxyDiameter() throws Throwable
	{
		Endpoint bob = createEndpoint("bob");
		UaRunnable callB = new UasScript.NotFound(bob.getUserAgent());
		Endpoint bob2 = createEndpoint("bob");
		UaRunnable callC = new UasScript.OkBye(bob2.getUserAgent());

		callB.start();
		callC.start();

		SipServletRequest request = _ua.createRequest(SipMethods.INVITE, bob.getUri());
		request.setRequestURI(bob.getContact().getURI());
		request.addHeader("proxy", bob2.getContact().toString());
		Call callA = _ua.createCall(request);

		try
		{
			assertThat(callA.waitForResponse(), isSuccess());
			callA.createAck().send();
			Thread.sleep(200);
			callA.createBye().send();
			assertThat(callA.waitForResponse(), isSuccess());
			callB.assertDone();
			callC.assertDone();
		}
		finally
		{
			checkForFailure();
		}
	}

	/**
	 * <pre>
	 *  Alice        Proxy         Bob
	 * 1  | INVITE     |            |
	 *    |----------->|            |
	 * 2  |            | INVITE     |
	 *    |            |----------->|
	 * 3  |            |        180 |
	 *    |            |<-----------|
	 * 4  |        180 |            |
	 *    |<-----------|            |
	 * 5  |            | CANCEL     |
	 *    |            |----------->|
	 * 6  |        408 |            |
	 *    |<-----------|            |
	 * 7  |            | 200/CANCEL |
	 *    |            |<-----------|
	 * 8  |            | 487/INVITE |
	 *    |            |<-----------|
	 * 9  |            | ACK        |
	 *    |            |----------->|
	 * 10 | ACK        |            |
	 *    |----------->|            |
	 * </pre>
	 */
	@Test
	public void testVirtualBranch() throws Exception
	{
		Endpoint bob = createEndpoint("bob");
		UaRunnable callB = new UasScript.RingingCanceled(bob.getUserAgent());

		callB.start();

		SipServletRequest request = _ua.createRequest(SipMethods.INVITE, bob.getUri());
		request.setRequestURI(bob.getContact().getURI());
		Call callA = _ua.createCall(request);

		assertThat(callA.waitForResponse(), hasStatus(SipServletResponse.SC_REQUEST_TIMEOUT));
		checkForFailure();
	}

	/**
	 * Ensure that the servlet is not invoked if session is invalidated.
	 * <p>
	 * See http://jira.cipango.org/browse/CIPANGO-121
	 * 
	 * <pre>
	 *  Alice        Proxy       Bob
	 * 1  | INVITE     |          |
	 *    |----------->|          |
	 * 2  |            | INVITE   |
	 *    |            |--------->|
	 * 3  |            |      200 |
	 *    |            |<---------|
	 * 4  |        200 |          |
	 *    |<-----------|          |
	 * 5  | ACK        |          |
	 *    |----------->|          |
	 * 6  |            | ACK      |
	 *    |            |--------->|
	 * 7  | BYE        |          |
	 *    |----------->|          |
	 * 8  |            | BYE      |
	 *    |            |--------->|
	 * 9  |            |      200 |
	 *    |            |<---------|
	 * 10 |        200 |          |
	 *    |<-----------|          |
	 * </pre>
	 */
	@Test
	public void testInvalidateBefore200() throws Exception
	{
		Endpoint bob = createEndpoint("bob");
		UaRunnable callB = new UasScript.OkBye(bob.getUserAgent());

		callB.start();

		SipServletRequest request = _ua.createRequest(SipMethods.INVITE, bob.getUri());
		request.setRequestURI(bob.getContact().getURI());
		Call callA = _ua.createCall(request);
		assertThat(callA.waitForResponse(), isSuccess());
		callA.createAck().send();
		
		Thread.sleep(100);
		callA.createBye().send();

		SipServletResponse response = callA.waitForResponse();
		assertThat(response, isSuccess());
		assertThat(response.getHeader("error"), is(nullValue()));
	}

	/**
	 * <pre>
	 *  Alice         Proxy             Proxy       Bob
	 * 1  | INVITE      |                 |          |
	 *    |------------>|                 |          |
	 * 2  |             |INVITE tel:1234  |          |
	 *    |             |---------------->|          |
	 * 3  |             |                 | INVITE   |
	 *    |             |                 |--------->|
	 * 4  |             |                 |      603 |
	 *    |             |                 |<---------|
	 * 5  |             |             603 |          |
	 *    |             |<----------------|          |
	 * 6  |         603 |                 |          |
	 *    |<------------|                 |          |
	 * 7  | ACK         |                 |          |
	 *    |------------>|                 |          |
	 * 8  |             |ACK              |          |
	 *    |             |---------------->|          |
	 * 9  |             |                 | ACK      |
	 *    |             |                 |--------->|
	 * </pre>
	 */
	@Test
	public void testTelUri() throws Exception
	{
		final Endpoint bob = createEndpoint("bob");
		bob.getUserAgent().setDefaultHandler(new MessageHandler()
		{
			public void handleRequest(SipServletRequest request) throws IOException, ServletException
			{
				assertThat(request.getHeader("req-uri"), containsString("tel:1234"));
				bob.getUserAgent().createResponse(request, SipServletResponse.SC_DECLINE).send();
			}

			public void handleResponse(SipServletResponse response) throws IOException, ServletException
			{
			}
		});

		SipServletRequest request = _ua.createRequest(SipMethods.INVITE, bob.getUri());
		request.setRequestURI(bob.getContact().getURI());
		Call call = _ua.createCall(request);
		assertThat(call.waitForResponse(), hasStatus(SipServletResponse.SC_DECLINE));
	}
	
	
	/**
	 * Ensures that
	 *  - DNS resolution is done using NAPTR and SRV in proxy mode;
	 *  - retry is done if a 503 is received.
	 *  
	 * 
	 * To run this test, the DNS server for domain cipango.voip should be configured with
	 * <pre>
	 * bob         IN NAPTR 90  40  "s"  "SIP+D2U"      ""  _sip._udp.bob
	 * _sip._udp.bob           SRV 0 0 5064 local
	 * _sip._udp.bob           SRV 5 0 5063 local
	 * </pre>
	 * Note:This test is marked as ignored as it requires a specific DNS configuration.
	 */
	@Ignore
	public void dnsCase() throws Exception
	{
		final Endpoint bob = createEndpoint("bob");
		bob.getUserAgent().setDefaultHandler(new MessageHandler()
		{
			public void handleRequest(SipServletRequest request) throws IOException, ServletException
			{
				SipServletResponse response = bob.getUserAgent().createResponse(request, SipServletResponse.SC_ACCEPTED);
				response.addHeader("X-Dns", "ok");
				response.send();
			}

			public void handleResponse(SipServletResponse response) throws IOException, ServletException
			{
			}
		});
		
		final Endpoint bob2 = createEndpoint("bob");
		bob2.getUserAgent().setDefaultHandler(new MessageHandler()
		{
			public void handleRequest(SipServletRequest request) throws IOException, ServletException
			{
				SipServletResponse response = bob.getUserAgent().createResponse(request, SipServletResponse.SC_SERVICE_UNAVAILABLE);
				response.send();
			}

			public void handleResponse(SipServletResponse response) throws IOException, ServletException
			{
			}
		});
		
		SipServletRequest request = _ua.createRequest(SipMethods.MESSAGE, bob.getUri());
		request.setRequestURI(_sipClient.getFactory().createURI("sip:bob.cipango.voip"));
		Call call = _ua.createCall(request);
		assertThat(call.waitForResponse(), hasStatus(SipServletResponse.SC_ACCEPTED));
		
		checkForFailure();
	}
	
	/**
	 * Test if a CANCEL is received when servlet is in proxy mode but has not proxy any request.
	 * <pre>
	 * Alice                         AS
	 *   | INVITE                     |
	 *   |--------------------------->|
	 *   |                        100 |
	 *   |<---------------------------|
	 *   | CANCEL                     |
	 *   |--------------------------->|
 	 *   |                 200/CANCEL |
	 *   |<---------------------------|
	 *   |                 487/INVITE |
	 *   |<---------------------------|
	 *   |                        ACK |
	 *   |--------------------------->|
	 * </pre>
	 */
	@Test
	public void testEarlyCancel() throws Exception
	{
		Call call = _ua.createCall(_ua.getFactory().createURI(getTo()));
		call.createCancel().send();
        assertThat(call.waitForResponse(), hasStatus(SipServletResponse.SC_REQUEST_TERMINATED));        
        // CANCEL response is filtered by container
        checkForFailure();
	}
}
