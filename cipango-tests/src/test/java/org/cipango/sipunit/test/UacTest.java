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
import javax.servlet.sip.SipURI;

import org.cipango.client.SipHeaders;
import org.cipango.client.SipMethods;
import org.cipango.sipunit.UaRunnable;
import org.cipango.sipunit.UaTestCase;
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
				assertEquals(SipMethods.INVITE, request.getMethod());
				_ua.createResponse(request, SipServletResponse.SC_RINGING).send();
				Thread.sleep(200);
				_ua.createResponse(request, SipServletResponse.SC_OK).send();
				assertEquals(SipMethods.ACK, _dialog.waitForRequest().getMethod());
				
				request = _dialog.waitForRequest();
				assertEquals(SipMethods.INVITE, request.getMethod());
				_ua.createResponse(request, SipServletResponse.SC_OK).send();
				assertEquals(SipMethods.ACK, _dialog.waitForRequest().getMethod());
				
				Thread.sleep(200);
				_dialog.createRequest(SipMethods.BYE).send();
				assertValid(_dialog.waitForResponse());
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
				assert request.getMethod().equals(SipMethods.INVITE);
		        SipURI uri = (SipURI) request.getRequestURI();
		        assertNull(uri.getTransportParam());
		        String via = request.getHeader(SipHeaders.VIA);
		        assertEquals("UDP", via.substring(8, 11));

		        _ua.createResponse(request, SipServletResponse.SC_OK).send();
				assert _dialog.waitForRequest().getMethod().equals(SipMethods.ACK);

				Thread.sleep(200);
				_dialog.createRequest(SipMethods.BYE).send();
				assertValid(_dialog.waitForResponse());
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
