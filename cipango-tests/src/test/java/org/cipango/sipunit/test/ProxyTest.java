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
import org.cipango.sipunit.UaRunnable;
import org.cipango.sipunit.UaTestCase;
import org.cipango.sipunit.UasScript;
import org.junit.Test;

public class ProxyTest extends UaTestCase
{
	/**
	 * <pre>
	 *  Alice        Proxy       Bob        Carol        HSS
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
		Call callA = (Call) _ua.customize(new Call());
		UaRunnable callB = new UasScript.NotFound(getBobUserAgent());
		UaRunnable callC = new UasScript.OkBye(getCarolUserAgent());

		callB.start();
		callC.start();
		
		SipServletRequest request = callA.createInitialInvite(
				_ua.getFactory().createURI(getFrom()),
				_ua.getFactory().createURI(getBobUri()));
		request.addHeader("proxy", getCarolContact().toString());
		callA.start(request);

		try 
		{			
	        assertValid(callA.waitForResponse());
	        callA.createAck().send();	        
	        Thread.sleep(200);
	        callA.createBye().send();
	        assertValid(callA.waitForResponse());
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
		Call callA = (Call) _ua.customize(new Call());
		UaRunnable callB = new UasScript.RingingCanceled(getBobUserAgent());
		
		callB.start();
		
		SipServletRequest request = callA.createInitialInvite(
				_ua.getFactory().createURI(getFrom()),
				_ua.getFactory().createURI(getBobUri()));
		request.setRequestURI(getBobContact().getURI());
		callA.start(request);

		try 
		{
	        assertValid(callA.waitForResponse(), SipServletResponse.SC_REQUEST_TIMEOUT);
		}
		finally
		{
			checkForFailure();
		}
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
	public void testInvalidateBefore200() throws Exception
	{
		Call callA = (Call) _ua.customize(new Call());
		UaRunnable callB = new UasScript.OkBye(getBobUserAgent());

		callB.start();
		
		SipServletRequest request = callA.createInitialInvite(
				_ua.getFactory().createURI(getFrom()),
				_ua.getFactory().createURI(getBobUri()));
		request.setRequestURI(getBobContact().getURI());
		callA.start(request);
		
        assertValid(callA.waitForResponse());
        callA.createBye().send();
        
        SipServletResponse response = callA.waitForResponse();
        assertValid(response);
        String error = response.getHeader("error");
        if (error != null)
        	fail(error);
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
	public void testTelUri() throws Exception
	{
		Call callA = (Call) _ua.customize(new Call());
		Call callB = (Call) getBobUserAgent().customize(new Call());
		
		SipServletRequest request = callA.createInitialInvite(
				_ua.getFactory().createURI(getFrom()),
				_ua.getFactory().createURI(getBobUri()));
		request.setRequestURI(getBobContact().getURI());
		callA.start(request);
		
		request = callB.waitForRequest();
		assertTrue(request.getHeader("req-uri").toString().contains("tel:1234"));
		getBobUserAgent().createResponse(request, SipServletResponse.SC_DECLINE).send();
        assertValid(callA.waitForResponse(), SipServletResponse.SC_DECLINE);
	}
}
