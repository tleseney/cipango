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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.cipango.tests.matcher.SipMatchers.*;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.cipango.client.Call;
import org.cipango.client.SipHeaders;
import org.cipango.client.SipMethods;
import org.cipango.tests.UaRunnable;
import org.cipango.tests.UaTestCase;
import org.junit.Test;

public class ReliableTest extends UaTestCase
{

	/**
	 *  SipUnit        Cipango
	 *    |               |
	 * 1  | INVITE        |
	 *    |-------------->|
	 * 2  |           183 |
	 *    |<--------------|
	 * 3  | PRACK         |
	 *    |-------------->|
	 * 4  |     200/PRACK |
	 *    |<--------------|
	 * 5  |           183 |
	 *    |<--------------|
	 * 6  | PRACK         |
	 *    |-------------->|
	 * 7  |     200/PRACK |
	 *    |<--------------|
	 * 8  |    200/INVITE |
	 *    |<--------------|
	 * 9  | ACK           |
	 *    |-------------->|           
	 * 10 | BYE           |
	 *    |-------------->|
	 * 11 |        200 OK |
	 *    |<--------------|
	 */
	@Test
	public void test100Rel() throws Exception
	{
		SipServletRequest request = _ua.createRequest(
				SipMethods.INVITE, createEndpoint("bob").getUri());
		request.addHeader(SipHeaders.SUPPORTED, "100rel");
		Call call = _ua.createCall(request); // 1
	
		SipServletResponse response = call.waitForResponse(); // 2
		assertThat(response, hasStatus(SipServletResponse.SC_SESSION_PROGRESS));

		_ua.decorate(response.createPrack()).send();  // 3
		assertThat(call.waitForResponse(), isSuccess()); // 4
		
		response = call.waitForResponse(); // 5
		assertThat(response, hasStatus(SipServletResponse.SC_SESSION_PROGRESS));

		_ua.decorate(response.createPrack()).send();  // 6
		assertThat(call.waitForResponse(), isSuccess()); // 7
		
        assertThat(call.waitForResponse(), isSuccess()); // 8
		call.createAck().send();  // 9
		
		Thread.sleep(200);
		call.createBye().send(); // 10
        assertThat(call.waitForResponse(), isSuccess()); // 11
	}
	
	/**
	 *  SipUnit        Cipango
	 *    |               |
	 * 1  | INVITE        |
	 *    |-------------->|
	 * 2  |           183 |
	 *    |<--------------|
	 * 3  | PRACK         |
	 *    |-------------->|
	 * 4  |    200/INVITE |
	 *    |<--------------|
	 * 5  | ACK           |
	 *    |-------------->|
	 * 6  |     200/PRACK |
	 *    |<--------------|
	 * 7  | BYE           |
	 *    |-------------->|
	 * 8  |        200 OK |
	 *    |<--------------|
	 */
	public void testLatePrackAnswer() throws Exception
	{
		SipServletRequest request = _ua.createRequest(
				SipMethods.INVITE, createEndpoint("bob").getUri());
		request.addHeader(SipHeaders.SUPPORTED, "100rel");
		Call call = _ua.createCall(request); // 1
		
		SipServletResponse response = call.waitForResponse(); // 2
		assertThat(response, hasStatus(SipServletResponse.SC_SESSION_PROGRESS));
		
		_ua.decorate(response.createPrack()).send();  // 3
		assertThat(call.waitForResponse(), isSuccess()); // 4
		call.createAck().send();  // 5

		assertThat(call.waitForResponse(), isSuccess()); // 6
		Thread.sleep(200);
		call.createBye().send(); // 7
        assertThat(call.waitForResponse(), isSuccess()); // 8
	}
	
	/**
	 *  SipUnit        Cipango
	 *    |               |
	 *    | REGISTER      |
	 *    |-------------->|
	 *    |           200 |
	 *    |<--------------|
	 *    |        INVITE |
	 *    |<--------------|
	 *    |           183 |
	 *    |-------------->|
	 *    | PRACK         |
	 *    |<--------------|
	 *    |     200/PRACK |
	 *    |-------------->|
	 *    |    200/INVITE |
	 *    |-------------->|
	 *    | ACK           |
	 *    |<--------------|
	 *    | BYE           |
	 *    |-------------->|
	 *    |        200 OK |
	 *    |<--------------|
	 */
	@Test
	public void test100RelUac() throws Exception

	{
		test100RelUac(false);
	}
	
	/**
	 *  SipUnit        Cipango
	 *    |               |
	 *    | REGISTER      |
	 *    |-------------->|
	 *    |           200 |
	 *    |<--------------|
	 *    |        INVITE |
	 *    |<--------------|
	 *    |           183 |
	 *    |-------------->|
	 *    | PRACK         |
	 *    |<--------------|
	 *    |    200/INVITE |
	 *    |-------------->|
	 *    |     200/PRACK |
	 *    |-------------->|
	 *    | ACK           |
	 *    |<--------------|
	 *    | BYE           |
	 *    |-------------->|
	 *    |        200 OK |
	 *    |<--------------|
	 */
	@Test
	public void test100RelUacLatePrackAnswer() throws Exception
	{
		test100RelUac(true);
	}
	
	public void test100RelUac(final boolean latePrackAnswer) throws Exception
	{
		UaRunnable call = new UaRunnable(_ua)
		{
			@Override
			public void doTest() throws Throwable
			{
				SipServletRequest invite = waitForInitialRequest();
				assertThat(invite.getMethod(), is(equalTo(SipMethods.INVITE)));
				assertThat(invite.getHeader(SipHeaders.SUPPORTED), is(equalTo("100rel")));
				
				SipServletResponse response = _ua.createResponse(invite,
						SipServletResponse.SC_SESSION_PROGRESS);
				response.sendReliably();
				
				SipServletRequest prack = _dialog.waitForRequest();
				_ua.createResponse((latePrackAnswer) ? invite : prack,
						SipServletResponse.SC_OK).send();
				Thread.sleep(100);
				_ua.createResponse((latePrackAnswer) ? prack : invite,
						SipServletResponse.SC_OK).send();
				
				assertThat(_dialog.waitForRequest().getMethod(), is(equalTo(SipMethods.ACK)));
				
				Thread.sleep(200);
				_dialog.createRequest(SipMethods.BYE).send();
				assertThat(_dialog.waitForResponse(), isSuccess());
			}
		};
		
		try
		{
			call.start();
			startUacScenario();
			call.join(2000);
			call.assertDone();
		}
		catch (Throwable t)
		{
			throw new Exception(t);
		}
		finally
		{
			checkForFailure();
		}
	}
}
