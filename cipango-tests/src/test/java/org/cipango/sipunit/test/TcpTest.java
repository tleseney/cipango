// ========================================================================
// Copyright 2012 NEXCOM Systems
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

public class TcpTest extends UaTestCase
{

	public TcpTest()
	{
	}
	
	
	/**
	 * Ensure that a big request is sent over TCP.
	 * 
	 * <pre>
	 * Alice                          AS
	 *   | MESSAGE                    |
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
	 */
	@Test
	public void testBigRequest() throws Exception
	{
		UaRunnable call = new UaRunnable(_ua)
		{
			@Override
			public void doTest() throws Throwable
			{
				SipServletRequest request = waitForInitialRequest();
				assertEquals(SipMethods.INVITE, request.getMethod());
		        SipURI uri = (SipURI) request.getRequestURI();
		        assertNull(uri.getTransportParam());
		        String via = request.getHeader(SipHeaders.VIA);
		        assertTrue(via.contains("/TCP "));
		        
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
			System.out.println("Ensure that mtu is configured to 1500 in cipango !");
    		throw new Exception(t);
    	}
    	finally
    	{
    		checkForFailure();
    	}
	}
}
