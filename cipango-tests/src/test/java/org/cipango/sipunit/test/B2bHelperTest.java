// ========================================================================
// Copyright 2011 NEXCOM Systems
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
import org.cipango.sipunit.UaRunnable;
import org.cipango.sipunit.UaTestCase;
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
	 *    |            | 200/CANCEL |
	 *    |            |<-----------|
	 *    |            | 487/INVITE |
	 *    |            |<-----------|
	 *    | 487/INVITE |            |
	 *    |<-----------|            |
	 *    |            | ACK        |
	 *    |            |----------->|
	 *    | ACK        |            |
	 *    |----------->|            |
	 * </pre>
	 */
	@Test
	public void testCancel() throws Throwable
	{
		Call callA = (Call) _ua.customize(new Call());
		UaRunnable callB = new UaRunnable(getBobUserAgent())
		{
			@Override
			public void doTest() throws Throwable
			{
				SipServletRequest request = waitForInitialRequest();
				SipServletResponse response = _ua.createResponse(
						request, SipServletResponse.SC_SESSION_PROGRESS);
				response.addHeader(SipHeaders.REQUIRE, "100rel");
				response.send();
				
				request = _dialog.waitForRequest();
				assert request.getMethod().equals(SipMethods.PRACK);
				_ua.createResponse(request, SipServletResponse.SC_OK);
				
				request = _dialog.waitForRequest();
				assert request.getMethod().equals(SipMethods.CANCEL);
			}
		};
		
		callB.start();
		
		SipServletRequest request = callA.createInitialInvite(
				_ua.getFactory().createURI(getFrom()),
				_ua.getFactory().createURI(getBobUri()));
		request.setRequestURI(getBobContact().getURI());
		request.addHeader(SipHeaders.SUPPORTED, "100rel");
		callA.start(request);

		try 
		{			
			SipServletResponse response = callA.waitForResponse();
	        assertValid(response, SipServletResponse.SC_SESSION_PROGRESS);
	        response.createPrack().send();
	        assertValid(callA.waitForResponse());
			Thread.sleep(50);
	        callA.createCancel().send();	        
	        assertValid(callA.waitForResponse(), SipServletResponse.SC_REQUEST_TERMINATED);
	        callB.assertDone();
		}
		finally
		{
			checkForFailure();
		}
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
	 *    |            | 200/CANCEL |
	 *    |            |<-----------|
	 *    |            | 487/INVITE |
	 *    |            |<-----------|
	 *    | 487/INVITE |            |
	 *    |<-----------|            |
	 *    |            | ACK        |
	 *    |            |----------->|
	 *    | ACK        |            |
	 *    |----------->|            |
	 * </pre>
	 */
	@Test
	public void testEarlyCancel() throws Throwable
	{
		Call callA = (Call) _ua.customize(new Call());
		UaRunnable callB = new UaRunnable(getBobUserAgent())
		{
			@Override
			public void doTest() throws Throwable
			{
				SipServletRequest request = waitForInitialRequest();
				assert request.getMethod().equals(SipMethods.INVITE);
				request = _dialog.waitForRequest();
				assert request.getMethod().equals(SipMethods.CANCEL);
			}
		};
		
		callB.start();
		
		SipServletRequest request = callA.createInitialInvite(
				_ua.getFactory().createURI(getFrom()),
				_ua.getFactory().createURI(getBobUri()));
		request.setRequestURI(getBobContact().getURI());
		callA.start(request);

		try 
		{			
			Thread.sleep(50);
			callA.createCancel().send();
	        assertValid(callA.waitForResponse(), SipServletResponse.SC_REQUEST_TIMEOUT);
	        callB.assertDone();
		}
		finally
		{
			checkForFailure();
		}
	}
}
