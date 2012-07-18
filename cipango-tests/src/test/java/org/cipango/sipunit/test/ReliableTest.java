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

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.cipango.client.Call;
import org.cipango.client.SipHeaders;
import org.cipango.client.SipMethods;
import org.cipango.sipunit.TestAgent;
import org.cipango.sipunit.UaRunnable;
import org.cipango.sipunit.UaTestCase;
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
		Call call = (Call) _ua.customize(new Call());
		
		SipServletRequest request = call.createInitialInvite(
				_ua.getFactory().createURI(getFrom()),
				_ua.getFactory().createURI(getBobUri()));
		request.setRequestURI(getBobContact().getURI());
		request.addHeader(SipHeaders.SUPPORTED, "100rel");
		call.start(request); // 1
	
		SipServletResponse response = call.waitForResponse(); // 2
		assertValid(response, SipServletResponse.SC_SESSION_PROGRESS);

		TestAgent.decorate(response.createPrack()).send();  // 3
		assertValid(call.waitForResponse()); // 4
		
		response = call.waitForResponse(); // 5
		assertValid(response, SipServletResponse.SC_SESSION_PROGRESS);

		TestAgent.decorate(response.createPrack()).send();  // 6
		assertValid(call.waitForResponse()); // 7
		
        assertValid(call.waitForResponse()); // 8
		call.createAck().send();  // 9
		
		Thread.sleep(200);
		call.createBye().send(); // 10
        assertValid(call.waitForResponse()); // 11
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
		Call call = (Call) _ua.customize(new Call());
		
		SipServletRequest request = call.createInitialInvite(
				_ua.getFactory().createURI(getFrom()),
				_ua.getFactory().createURI(getBobUri()));
		request.setRequestURI(getBobContact().getURI());
		request.addHeader(SipHeaders.SUPPORTED, "100rel");
		call.start(request); // 1
		
		SipServletResponse response = call.waitForResponse(); // 2
		assertValid(response, SipServletResponse.SC_SESSION_PROGRESS);
		
		TestAgent.decorate(response.createPrack()).send();  // 3
		assertValid(call.waitForResponse()); // 4
		call.createAck().send();  // 5

		assertValid(call.waitForResponse()); // 6
		Thread.sleep(200);
		call.createBye().send(); // 7
        assertValid(call.waitForResponse()); // 8
	}
	
	/**
	 *  SipUnit        Cipango
	 *    |               |
	 *    | MESSAGE       |
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
	 *    | MESSAGE       |
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
				assertEquals(SipMethods.INVITE, invite.getMethod());
				assertEquals("100rel", invite.getHeader(SipHeaders.SUPPORTED));
				
				SipServletResponse response = _ua.createResponse(invite,
						SipServletResponse.SC_SESSION_PROGRESS);
				response.addHeader(SipHeaders.REQUIRE, "100rel");
				response.send();
				
				SipServletRequest prack = _dialog.waitForRequest();
				_ua.createResponse((latePrackAnswer) ? invite : prack,
						SipServletResponse.SC_OK).send();
				Thread.sleep(100);
				_ua.createResponse((latePrackAnswer) ? prack : invite,
						SipServletResponse.SC_OK).send();
				
				assertEquals(SipMethods.ACK, _dialog.waitForRequest().getMethod());
				
				Thread.sleep(200);
				_dialog.createRequest(SipMethods.BYE).send();
				assertValid(_dialog.waitForResponse());
			}
		};
		
		call.start();

		try
		{
			sendAndAssertMessage();
			call.join();
		}
		catch (Throwable e)
		{
			e.printStackTrace();
			fail();
		}
		finally
		{
			checkForFailure();
		}
	}
}
