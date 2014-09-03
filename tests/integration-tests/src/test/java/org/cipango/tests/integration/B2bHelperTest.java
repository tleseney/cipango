// ========================================================================
// Copyright 2011-2012 NEXCOM Systems
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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.cipango.client.Call;
import org.cipango.client.SipHeaders;
import org.cipango.client.SipMethods;
import org.cipango.client.test.UaRunnable;
import org.cipango.tests.UaTestCase;
import org.junit.Test;

public class B2bHelperTest extends UaTestCase
{
	/**
	 * <pre>
	 *  Alice         B2b          Bob
	 *    | INVITE     |            |
	 *    |----------->|            |
	 *    |            | INVITE     |
	 *    |            |----------->|
	 *    |            |        183 |
	 *    |            |<-----------|
	 *    |        183 |            |
	 *    |<-----------|            |
	 *    | PRACK      |            |
	 *    |----------->|            |
	 *    |            | PRACK      |
	 *    |            |----------->|
	 *    |            |  200/PRACK |
	 *    |            |<-----------|
	 *    |  200/PRACK |            |
	 *    |<-----------|            |
	 *    | CANCEL     |            |
	 *    |----------->|            |
	 *    |            | CANCEL     |
	 *    |            |----------->|
	 *    | 200/CANCEL |            |
	 *    |<-----------|            |
	 *    | 487/INVITE |            |
	 *    |<-----------|            |
	 *    |            | 200/CANCEL |
	 *    |            |<-----------|
	 *    |            | 487/INVITE |
	 *    |            |<-----------|
	 *    |            | ACK        |
	 *    |            |----------->|
	 *    | ACK        |            |
	 *    |----------->|            |
	 * </pre>
	 */
	@Test
	public void testCancel() throws Throwable
	{
		Endpoint bob = createEndpoint("bob");
		UaRunnable callB = new UaRunnable(bob.getUserAgent())
		{
			@Override
			public void doTest() throws Throwable
			{
				SipServletRequest request = waitForInitialRequest();
				assertThat(request.getMethod(), is(equalTo(SipMethods.INVITE)));

				SipServletResponse response = _ua.createResponse(request,
						SipServletResponse.SC_SESSION_PROGRESS);
				response.addHeader(SipHeaders.REQUIRE, "100rel");
				response.sendReliably();

				request = _dialog.waitForRequest();
				assertThat(request.getMethod(), is(equalTo(SipMethods.PRACK)));
				_ua.createResponse(request, SipServletResponse.SC_OK).send();

				request = _dialog.waitForRequest();
				assertThat(request.getMethod(), is(equalTo(SipMethods.CANCEL)));
			}
		};

		callB.start();

		SipServletRequest request = _ua.createRequest(SipMethods.INVITE, bob.getUri());
		request.setRequestURI(bob.getContact().getURI());
		request.addHeader(SipHeaders.SUPPORTED, "100rel");
		Call callA = _ua.createCall(request);

		SipServletResponse response = callA.waitForResponse();
		assertThat(response, hasStatus(SipServletResponse.SC_SESSION_PROGRESS));
		_ua.decorate(response.createPrack()).send();
		assertThat(callA.waitForResponse(), isSuccess());
		Thread.sleep(50);

		callA.createCancel().send();
		assertThat(callA.waitForResponse(), hasStatus(SipServletResponse.SC_REQUEST_TERMINATED));
		callB.join(2000);
		callB.assertDone();

		checkForFailure();
	}

	/**
	 * <pre>
	 *  Alice         B2b          Bob
	 *    | INVITE     |            |
	 *    |----------->|            |
	 *    |            | INVITE     |
	 *    |            |----------->|
	 *    |            |        100 |
	 *    |            |<-----------|
	 *    |        100 |            |
	 *    |<-----------|            |
	 *    | CANCEL     |            |
	 *    |----------->|            |
	 *    |            | CANCEL     |
	 *    |            |----------->|
	 *    | 200/CANCEL |            |
	 *    |<-----------|            |
	 *    | 487/INVITE |            |
	 *    |<-----------|            |
	 *    |            | 200/CANCEL |
	 *    |            |<-----------|
	 *    |            | 487/INVITE |
	 *    |            |<-----------|
	 *    |            | ACK        |
	 *    |            |----------->|
	 *    | ACK        |            |
	 *    |----------->|            |
	 * </pre>
	 */
	@Test
	public void testEarlyCancel() throws Throwable
	{
		Endpoint bob = createEndpoint("bob");
		UaRunnable callB = new UaRunnable(bob.getUserAgent())
		{
			@Override
			public void doTest() throws Throwable
			{
				SipServletRequest request = waitForInitialRequest();
				assertThat(request.getMethod(), is(equalTo(SipMethods.INVITE)));
				request = _dialog.waitForRequest();
				assertThat(request.getMethod(), is(equalTo(SipMethods.CANCEL)));
			}
		};

		callB.start();

		SipServletRequest request = _ua.createRequest(SipMethods.INVITE, bob.getUri());
		request.setRequestURI(bob.getContact().getURI());
		Call callA = _ua.createCall(request);

		Thread.sleep(50);
		callA.createCancel().send();
		callB.join(2000);
		callB.assertDone();

		checkForFailure();

	}
}
