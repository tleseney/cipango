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

import java.util.Iterator;

import javax.servlet.sip.Address;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipURI;

import org.cipango.client.Call;
import org.cipango.client.SipHeaders;
import org.cipango.client.SipMethods;
import org.cipango.sipunit.UaRunnable;
import org.cipango.sipunit.UaTestCase;
import org.junit.Test;


public class DoubleRecordRouteTest extends UaTestCase
{
	/**
	 * Ensure that if different connectors are used, there are two record routes.
	 */
	@Test
	public void testUdpToTcp() throws Throwable 
	{
		Call callA;
		UaRunnable callB = new UaRunnable(getBobUserAgent())
		{
			@Override
			public void doTest() throws Throwable
			{
				SipServletRequest request = waitForInitialRequest();
				assertEquals(SipMethods.INVITE, request.getMethod());
				Iterator<Address> it = request.getAddressHeaders(SipHeaders.RECORD_ROUTE);
				Address addr = it.next();
				assertEquals("tcp", ((SipURI) addr.getURI()).getTransportParam());
				assertTrue(it.hasNext());
				addr = it.next();
				assertNull(((SipURI) addr.getURI()).getTransportParam());
				assertFalse(it.hasNext());
		        
				_ua.createResponse(request, SipServletResponse.SC_OK).send();
				assertEquals(SipMethods.ACK, _dialog.waitForRequest().getMethod());
				request = _dialog.waitForRequest();
				assertEquals(SipMethods.BYE, request.getMethod());
				_ua.createResponse(request, SipServletResponse.SC_OK).send();
			}
		};

		try
		{
			callB.start();
			
			SipServletRequest request = _ua.createRequest(SipMethods.INVITE, getBobUri());
			request.setRequestURI(getBobContact().getURI());
			callA = _ua.createCall(request);

			SipServletResponse response = callA.waitForResponse();
	        assertValid(response);
	        Iterator<String> it = response.getHeaders("mode");
	        assertTrue(it.hasNext());
	        it.next();
	        assertFalse(it.hasNext());
	
	        callA.createAck().send();
	        Thread.sleep(200);
	        callA.createBye().send();
	        assertValid(callA.waitForResponse());
			callB.assertDone();
		}
		catch (Throwable t)
		{
			throw new Exception(t);
		}
	}
	
	/**
	 * Ensure that if the same connector is used, there is only one record route.
	 */
	@Test
	public void testUdpToUdp() throws Throwable 
	{
		Call callA;
		UaRunnable callB = new UaRunnable(getBobUserAgent())
		{
			@Override
			public void doTest() throws Throwable
			{
				SipServletRequest request = waitForInitialRequest();
				assertEquals(SipMethods.INVITE, request.getMethod());
				Iterator<Address> it = request.getAddressHeaders(SipHeaders.RECORD_ROUTE);
				Address addr = it.next();
				assertNull(((SipURI) addr.getURI()).getTransportParam());
				assertFalse(it.hasNext());
		        
				_ua.createResponse(request, SipServletResponse.SC_OK).send();
				assertEquals(SipMethods.ACK, _dialog.waitForRequest().getMethod());
				request = _dialog.waitForRequest();
				assertEquals(SipMethods.BYE, request.getMethod());
				_ua.createResponse(request, SipServletResponse.SC_OK).send();
			}
		};
		
		try
		{
			callB.start();
			
			SipServletRequest request = _ua.createRequest(SipMethods.INVITE, getBobUri());
			request.setRequestURI(getBobContact().getURI());
			callA = _ua.createCall(request);

	        assertValid(callA.waitForResponse());
	        callA.createAck().send();
	        Thread.sleep(200);
	        callA.createBye().send();
	        assertValid(callA.waitForResponse());
			callB.assertDone();
		}
		catch (Throwable t)
		{
			throw new Exception(t);
		}
	}
}
