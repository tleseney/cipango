// ========================================================================
// Copyright 2007-2008 NEXCOM Systems
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.cipango.sipunit.test.matcher.SipMatchers.*;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.cipango.client.Call;
import org.cipango.client.SipMethods;
import org.cipango.sipunit.UaTestCase;
import org.junit.Test;

public class SipApplicationKeyTest extends UaTestCase
{
	/**
	 * <pre>
	 * Call 1                  AS               call 2
	 *   | INVITE               |                  |
	 *   |--------------------->|                  |
	 *   |                  200 |                  |
	 *   |<---------------------|                  |
	 *   | ACK                  |                  |
	 *   |--------------------->|                  |
	 *   |                      |           INVITE |
	 *   |                      |<-----------------|
	 *   |                  BYE |                  |
	 *   |<---------------------|                  |
	 *   |                  200 |                  |
	 *   |--------------------->|                  |
	 *   |                      | 480              |
	 *   |                      |----------------->|
	 *   |                      |              ACK |
	 *   |                      |<-----------------|
	 * </pre>
	 */
	@Test
	public void testApplicationKey() throws Throwable 
	{
		SipServletRequest request = _ua.createRequest(SipMethods.INVITE, getTo());
		request.addHeader("sipApplicationKey", "key1");
		request.addHeader("mode", "create");
		Call call1 = _ua.createCall(request);
        assertThat(call1.waitForFinalResponse(), isSuccess());
		call1.createAck().send();

		request = _ua.createRequest(SipMethods.INVITE, getTo());
		request.addHeader("sipApplicationKey", "key1");
		request.addHeader("mode", "join");
		Call call2 = _ua.createCall(request);
		
		request = call1.waitForRequest();
		assertThat(request.getMethod(), is(equalTo(SipMethods.BYE)));
		_ua.createResponse(_ua.decorate(request), SipServletResponse.SC_OK).send();
		
		// TODO: 480 keeps being emitted ?!
        assertThat(call2.waitForResponse(), hasStatus(SipServletResponse.SC_TEMPORARLY_UNAVAILABLE));
        
        // Sleep so as the final ACK can be correctly sent.
        Thread.sleep(200);
	}
	
	/**
	 * Check that applications with different SipApplicationKey do not share the
	 * same SipApplicationSession instance.
	 */
	@Test
	public void testApplicationKeyDifferent() throws Throwable
	{
		SipServletRequest request = _ua.createRequest(SipMethods.INVITE, getTo());
		request.addHeader("sipApplicationKey", "key3");
		request.addHeader("mode", "join");
		Call call1 = _ua.createCall(request);
		assertThat(call1.waitForResponse(), hasStatus(SipServletResponse.SC_TEMPORARLY_UNAVAILABLE));

		request = _ua.createRequest(SipMethods.INVITE, getTo());
		request.addHeader("sipApplicationKey", "key4");
		request.addHeader("mode", "join");
		Call call2 = _ua.createCall(request);
		assertThat(call2.waitForResponse(), hasStatus(SipServletResponse.SC_TEMPORARLY_UNAVAILABLE));

        // Sleep so as the final ACK can be correctly sent.
		Thread.sleep(200);
	}
	
	/**
	 * Check that applications which have a null sip application key do not
	 * share the same SipApplicationSession instance.
	 */
	@Test
	public void testApplicationKeyNoKey() throws Throwable 
	{
		SipServletRequest request = _ua.createRequest(SipMethods.INVITE, getTo());
		request.addHeader("mode", "join");
		Call call1 = _ua.createCall(request);
		assertThat(call1.waitForResponse(), hasStatus(SipServletResponse.SC_TEMPORARLY_UNAVAILABLE));

		request = _ua.createRequest(SipMethods.INVITE, getTo());
		request.addHeader("mode", "join");
		Call call2 = _ua.createCall(request);
		assertThat(call2.waitForResponse(), hasStatus(SipServletResponse.SC_TEMPORARLY_UNAVAILABLE));
        
        // Sleep so as the final ACK can be correctly sent.
        Thread.sleep(200);
	}
	
	/**
	 * Test concurrent access on a same SipApplicationSession for two incoming calls.
	 * This is done with a Thread.sleep(1000) in doRequest of call 1.
	 * 
	 * <pre>
	 * Call 1                   AS               call 2
	 *   | INVITE                |                  |
	 *   |---------------------->|                  |
	 *   |                   180 |                  |
	 *   |<----------------------|                  |
	 *   |                       |           INVITE |
	 *   |          480 After 1s |<-----------------|
	 *   |<----------------------|                  |
	 *   |                       | 480              |
	 *   |                       |----------------->|
	 *   |                       |                  |
	 * </pre>
	 */
	@Test
	public void testConcurrentApplicationKey() throws Throwable 
	{
		SipServletRequest request = _ua.createRequest(SipMethods.INVITE, getTo());
		request.addHeader("sipApplicationKey", "key5");
		request.addHeader("mode", "create");
		Call call1 = _ua.createCall(request);
		assertThat(call1.waitForResponse(), hasStatus(SipServletResponse.SC_RINGING));

		request = _ua.createRequest(SipMethods.INVITE, getTo());
		request.addHeader("sipApplicationKey", "key5");
		request.addHeader("mode", "join");
		Call call2 = _ua.createCall(request);

		assertThat(call1.waitForResponse(), hasStatus(SipServletResponse.SC_TEMPORARLY_UNAVAILABLE));
		assertThat(call2.waitForResponse(), hasStatus(SipServletResponse.SC_TEMPORARLY_UNAVAILABLE));
		
        // Sleep so as the final ACK can be correctly sent.
        Thread.sleep(200);
	}
	
	/**
	 * In this case, a second request is set in the queue before the first is
	 * treated for call 2.
	 * 
	 * <pre>
	 * Call 1                  AS               call 2
	 *   | INVITE               |                  |
	 *   |--------------------->|                  |
	 *   |                  100 |                  |
	 *   |<---------------------|                  |
	 *   |                      |           INVITE |
	 *   |                      |<-----------------|
	 *   |                      | 100/INVITE       |
	 *   |                      |----------------->|
	 *   |                      |           CANCEL |
	 *   |                      |<-----------------|
	 *   |                      | 487/INVITE       |
	 *   |                      |----------------->|
	 *   |                      | 200/CANCEL       |
	 *   |                      |----------------->|
	 *   |                  480 |                  |
	 *   |<---------------------|                  |
	 * </pre>
	 */
	@Test
	public void testConcurrentApplicationKey2() throws Throwable 
	{
		SipServletRequest request = _ua.createRequest(SipMethods.INVITE, getTo());
		request.addHeader("sipApplicationKey", "key6");
		request.addHeader("mode", "create");
		Call call1 = _ua.createCall(request);
		Thread.sleep(100);

		request = _ua.createRequest(SipMethods.INVITE, getTo());
		request.addHeader("sipApplicationKey", "key6");
		request.addHeader("mode", "join");
		Call call2 = _ua.createCall(request);
		Thread.sleep(100);

		call2.createCancel().send();
		assertThat(call2.waitForResponse(), hasStatus(SipServletResponse.SC_REQUEST_TERMINATED));
		assertThat(call1.waitForResponse(), hasStatus(SipServletResponse.SC_TEMPORARLY_UNAVAILABLE));
		
        // Sleep so as the final ACK can be correctly sent.
        Thread.sleep(200);
	}
	
	/**
	 * <pre>
	 * Alice                   AS
	 *   | INVITE               |
	 *   |--------------------->|
	 *   |                  100 |
	 *   |<---------------------|
	 *   |                  180 |
	 *   |<---------------------|
	 *   | CANCEL               |
	 *   |--------------------->|
	 *   |           200/CANCEL |
	 *   |<---------------------|
	 *   |           487/INVITE |
	 *   |<---------------------|
	 *   | ACK                  |
	 *   |--------------------->|
	 * </pre>
	 */
	@Test
	public void testCancel() throws Throwable 
	{
		SipServletRequest request = _ua.createRequest(SipMethods.INVITE, getTo());
		request.addHeader("sipApplicationKey", "key7");
		request.addHeader("mode", "create");
		Call call = _ua.createCall(request);
		assertThat(call.waitForResponse(), hasStatus(SipServletResponse.SC_RINGING));

		call.createCancel().send();
		assertThat(call.waitForResponse(), hasStatus(SipServletResponse.SC_REQUEST_TERMINATED));
						
        // Sleep so as the final ACK can be correctly sent.
        Thread.sleep(200);
	}
}
