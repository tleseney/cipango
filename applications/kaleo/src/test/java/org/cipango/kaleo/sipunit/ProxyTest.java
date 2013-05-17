// ========================================================================
// Copyright 2008-2009 NEXCOM Systems
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
package org.cipango.kaleo.sipunit;

import javax.sip.message.Response;

import org.cafesip.sipunit.SipCall;
import org.cafesip.sipunit.SipResponse;

public class ProxyTest extends UaTestCase {

	
	public void testSimpleCall() throws Exception
	{
		getAlicePhone().register(null, 1800);
		assertLastOperationSuccess("Caller registration failed - "
				+ getAlicePhone().format(), getAlicePhone());

		getBobPhone().register(null, 600);
		assertLastOperationSuccess(getBobPhone());

		SipCall a = getAlicePhone().createSipCall();
		SipCall b = getBobPhone().createSipCall();

		b.listenForIncomingCall();
		Thread.sleep(10);

		// another way to invoke the operation and check the result
		// separately:

		boolean status_ok = a.initiateOutgoingCall(getBobUri(), null);
		assertTrue("Initiate outgoing call failed - " + a.format(), status_ok);

		// invoke the Sip operation and check positive result in one step,
		// no operation error details if the test fails:

		assertTrue("Wait incoming call error or timeout", b
				.waitForIncomingCall(5000));

		// invoke the Sip operation and result check in one step,
		// only standard JUnit output if the test fails:

		assertTrue(b.sendIncomingCallResponse(Response.RINGING, "Ringing", 0));

		Thread.sleep(1000);

		// although the 2-step method is not as compact, it's easier
		// to follow what a test is doing since the Sip operations are not
		// buried as parameters in assert statements:

		b.sendIncomingCallResponse(Response.OK, "Answer - Hello world", 0);
		assertLastOperationSuccess("Sending answer response failed - "
				+ b.format(), b);

		// note with the single step method, you cannot include operation
		// error details for when the test fails: ' + a.format()' wouldn't
		// work in the first parameter here:

		assertTrue("Wait response error", a.waitOutgoingCallResponse(10000));

		SipResponse resp = a.getLastReceivedResponse(); // watch for TRYING
		int status_code = resp.getStatusCode();
		while (status_code != Response.RINGING)
		{
			assertFalse("Unexpected final response, status = " + status_code,
					status_code > 200);

			assertFalse("Got OK but no RINGING", status_code == Response.OK);

			a.waitOutgoingCallResponse(10000);
			assertLastOperationSuccess("Subsequent response never received - "
					+ a.format(), a);
			resp = a.getLastReceivedResponse();
			status_code = resp.getStatusCode();
		}

		// if you want operation error details in your test fail output,
		// you have to invoke and complete the operation first:

		a.waitOutgoingCallResponse(10000);
		assertLastOperationSuccess("Wait response error - " + a.format(), a);

		// throw out any 'TRYING' responses
		// Note, you can also get the response status code from the SipCall
		// class itself (in addition to getting it from the response as
		// above)
		while (a.getReturnCode() == Response.TRYING)
		{
			a.waitOutgoingCallResponse(10000);
			assertLastOperationSuccess("Subsequent response never received - "
					+ a.format(), a);
		}
		resp = a.getLastReceivedResponse();

		// check for OK response.
		assertEquals("Unexpected response received", Response.OK, a
				.getReturnCode());

		// continue with the test call
		a.sendInviteOkAck();
		assertLastOperationSuccess("Failure sending ACK - " + a.format(), a);

		Thread.sleep(1000);

		b.listenForDisconnect();
		assertLastOperationSuccess("b listen disc - " + b.format(), b);

		a.disconnect();
		assertLastOperationSuccess("a disc - " + a.format(), a);

		b.waitForDisconnect(10000);
		assertLastOperationSuccess("b wait disc - " + b.format(), b);

		b.respondToDisconnect();
		assertLastOperationSuccess("b disc - " + b.format(), b);

		getBobPhone().unregister(null, 10000);
	}
	
	/*
	public void testQ() throws Exception
	{
		RegisterSession session = getBobPhone().getRegisterSession();
		Request request = session.createRegister(null, 1800);
		ContactHeader contact = (ContactHeader) request.getHeader(ContactHeader.NAME);
		contact.setQValue(0.7f);
		ContactHeader contact2 = (ContactHeader) contact.clone();
		contact2.setQValue(1.0f);
		SipURI uri = (SipURI) contact2.getAddress().getURI();
		uri.setUser("higherQuality");
		request.addHeader(contact2);
		session.sendRegistrationMessage(request, Response.OK);
		
		request = getAlicePhone().newRequest(Request.MESSAGE, 1, getBobUri());
		getBobPhone().listenRequestMessage();
		SipTransaction tx = getAlicePhone().sendRequestWithTransaction(request, true, null);
		tx = getAlicePhone().waitForProxyAuthentication(tx, 1);
		
		RequestEvent requestEvent = getBobPhone().waitRequest(5000);
		assertEquals(uri, requestEvent.getRequest().getRequestURI());
		getBobPhone().sendReply(requestEvent, Response.OK);
		
		getAlicePhone().waitResponse(tx, Response.OK);
	}
	*/
	
	
	public void testNotFound() throws Exception
	{
		getAlicePhone().register(null, 1800);
		assertLastOperationSuccess("Caller registration failed - "
				+ getAlicePhone().format(), getAlicePhone());

		SipCall callA = getAlicePhone().createSipCall();
		boolean status_ok = callA.initiateOutgoingCall(getBobUri(), null);
		assertTrue("Initiate outgoing call failed - " + callA.format(), status_ok);
				
		callA.waitForAnswer(Response.NOT_FOUND);
		
		callA.getLastReceivedResponse().toString();
	}
}
