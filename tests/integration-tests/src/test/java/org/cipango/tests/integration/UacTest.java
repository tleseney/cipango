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
package org.cipango.tests.integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.cipango.tests.matcher.SipMatchers.*;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipURI;

import org.cipango.client.SipHeaders;
import org.cipango.client.SipMethods;
import org.cipango.tests.UaRunnable;
import org.cipango.tests.UaTestCase;
import org.junit.Test;

public class UacTest extends UaTestCase
{

	/**
	 * <pre>
	 * Alice                         AS
	 *   | REGISTER                   |
	 *   |--------------------------->|
	 *   |                        200 |
	 *   |<---------------------------|
	 *   |                     INVITE |
	 *   |<---------------------------|
	 *   | 180                        |
	 *   |--------------------------->|
	 *   | 200                        |
	 *   |--------------------------->|
	 *   |                        ACK |
	 *   |<---------------------------|
	 *   |                     INVITE |
	 *   |<---------------------------|
	 *   | 200                        |
	 *   |--------------------------->|
	 *   |                        ACK |
	 *   |<---------------------------|
	 *   | BYE                        |
	 *   |--------------------------->|
	 *   |                        200 |
	 *   |<---------------------------|
	 * </pre>
	 */
	@Test
	public void testReInvite() throws Exception
	{
		UaRunnable call = new UaRunnable(_ua)
		{
			@Override
			public void doTest() throws Throwable
			{
				SipServletRequest request = waitForInitialRequest();
				assertThat(request.getMethod(), is(equalTo(SipMethods.INVITE)));
				_ua.createResponse(request, SipServletResponse.SC_RINGING).send();
				Thread.sleep(200);
				_ua.createResponse(request, SipServletResponse.SC_OK).send();
				assertThat(_dialog.waitForRequest().getMethod(), is(equalTo(SipMethods.ACK)));
				
				request = _dialog.waitForRequest();
				assertThat(request.getMethod(), is(equalTo(SipMethods.INVITE)));
				_ua.createResponse(request, SipServletResponse.SC_OK).send();
				assertThat(_dialog.waitForRequest().getMethod(), is(equalTo(SipMethods.ACK)));
				
				Thread.sleep(200);
				_dialog.createRequest(SipMethods.BYE).send();
				assertThat(_dialog.waitForResponse(), isSuccess());
			}
		};
		
		try
		{
			call.start();
			startScenario();
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
	
	
	/**
	 * Ensure that when a big request cannot be sent with TCP, it is sent with
	 * UDP.
	 * 
	 * <pre>
	 * Alice                         AS
	 *   | REGISTER                   |
	 *   |--------------------------->|
	 *   |                        200 |
	 *   |<---------------------------|
	 *   |                     INVITE |
	 *   |<---------------------------|
	 *   | 200                        |
	 *   |--------------------------->|
	 *   |                        ACK |
	 *   |<---------------------------|
	 *   | BYE                        |
	 *   |--------------------------->|
	 *   |                        200 |
	 *   |<---------------------------|
	 * </pre>
	 * 
	 * @see TcpTest#testBigRequest()
	 */
	@Test
	public void testBigRequestFallback() throws Exception
	{
		UaRunnable call = new UaRunnable(_ua)
		{
			@Override
			public void doTest() throws Throwable
			{
				SipServletRequest request = waitForInitialRequest();
				assertThat(request.getMethod(), is(equalTo(SipMethods.INVITE)));
		        SipURI uri = (SipURI) request.getRequestURI();
		        assertThat(uri.getTransportParam(), is(nullValue()));
		        String via = request.getHeader(SipHeaders.VIA);
		        assertThat(via.substring(8, 11), is(equalTo("UDP")));

		        _ua.createResponse(request, SipServletResponse.SC_OK).send();
				assertThat(_dialog.waitForRequest().getMethod(), is(equalTo(SipMethods.ACK)));
				
				Thread.sleep(200);
				_dialog.createRequest(SipMethods.BYE).send();
				assertThat(_dialog.waitForResponse(), isSuccess());
			}
		};
		
		try
		{
			call.start();
			startScenario();
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
