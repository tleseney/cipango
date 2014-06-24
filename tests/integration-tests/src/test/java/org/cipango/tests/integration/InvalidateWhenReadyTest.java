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


import static org.cipango.client.test.matcher.SipMatchers.hasStatus;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.util.concurrent.Semaphore;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.cipango.client.Call;
import org.cipango.client.Dialog;
import org.cipango.client.SipHeaders;
import org.cipango.client.SipMethods;
import org.cipango.client.Subscriber;
import org.cipango.client.test.UaRunnable;
import org.cipango.client.test.UasScript;
import org.cipango.tests.UaTestCase;
import org.cipango.tests.integration.category.NotCompliantV2;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Test invalidate when ready and session state
 */
public class InvalidateWhenReadyTest extends UaTestCase
{

	private static final Logger LOG = Log.getLogger(InvalidateWhenReadyTest.class);
	
	/**
	 * <pre>
	 * Alice                         AS
	 *   | INVITE                     |
	 *   |--------------------------->|
	 *   |                        180 |
	 *   |<---------------------------|
	 *   | CANCEL                     |
	 *   |--------------------------->|
 	 *   |                        200 |
	 *   |<---------------------------|
	 *   |                        487 |
	 *   |<---------------------------|
	 *   |                        ACK |
	 *   |--------------------------->|
	 * </pre>
	 */
	@Test
	public void testUasCancel() throws Throwable 
	{
		Call call = _ua.createCall(_ua.getFactory().createURI(getTo()));
		assertThat(call.waitForResponse(), hasStatus(SipServletResponse.SC_RINGING));
		call.createCancel().send();
        assertThat(call.waitForResponse(), hasStatus(SipServletResponse.SC_REQUEST_TERMINATED));        

        checkForFailure();
	}

	/**
	 * <pre>
	 * Alice                         AS
	 *   | INVITE                     |
	 *   |--------------------------->|
	 *   |                        200 |
	 *   |<---------------------------|
	 *   |                        ACK |
	 *   |--------------------------->|
	 *   | BYE                        |
	 *   |--------------------------->|
 	 *   |                        200 |
	 *   |<---------------------------|
	 * </pre>
	 */
	@Test
	public void testUasBye() throws Throwable 
	{
		Call call = _ua.createCall(_ua.getFactory().createURI(getTo()));
        assertThat(call.waitForResponse(), isSuccess());
        call.createAck().send();
        call.createRequest(SipMethods.BYE).send();
        assertThat(call.waitForResponse(), isSuccess());
        
        checkForFailure();
	}
	
	@Test
	public void testMessage() throws Throwable 
	{
		sendAndAssertMessage();
	}
	
	/**
	 * <pre>
	 * Alice                         AS
	 *   | MESSAGE                    |
	 *   |--------------------------->|
	 *   |                        200 |
	 *   |<---------------------------|
	 *   |                     INVITE |
	 *   |<---------------------------|
	 *   | 180                        |
	 *   |--------------------------->|
	 *   | 403                        |
	 *   |--------------------------->|
	 *   |                        ACK |
	 *   |<---------------------------|
	 * </pre>
	 */
	@Test
	@Category(NotCompliantV2.class)
	public void testUac4xx() throws Throwable 
	{
		UaRunnable call = new UasScript.RingingForbidden(_ua);
		call.start();
		startUacScenario();
		call.join(2000);
		call.assertDone();
		checkForFailure();
	}
	
	/**
	 * Simulate case when response is received while session is still locked by sender thread.
	 * Ensure that the session is not invalidated before servlet has been invoked.
	 * <pre>
	 *   
	 * Alice          AS       Bob
	 *   | INVITE     |          |
	 *   |----------->|          |
	 *   |        180 |          |
	 *   |<-----------|          |
	 *   |            | MESSAGE  |
	 *   |            |--------->|
	 *   |            |      200 |
	 *   |            |<---------|
	 *   |        403 |          |
	 *   |<-----------|          |
	 *   | ACK        |          |
	 *   |----------->|          |
	 * </pre>
	 * @see http://jira.cipango.org/browse/CIPANGO-191
	 */
	@Test
	@Category(NotCompliantV2.class)
	public void testUacEarlyResponse() throws Throwable 
	{
		Call callA;
		Endpoint bob = createEndpoint("bob");
		UaRunnable callB = new UasScript.OkNonInvite(bob.getUserAgent());
		
		callB.start();
		
		SipServletRequest request = _ua.createRequest(SipMethods.INVITE, bob.getUri());
		request.setRequestURI(bob.getContact().getURI());
		callA = _ua.createCall(request);
		assertThat(callA.waitForResponse(), hasStatus(SipServletResponse.SC_RINGING));
		SipServletResponse response = callA.waitForResponse();
		assertThat("No response received, doResponse might have not been invoked", response, notNullValue());
		assertThat(response, hasStatus(SipServletResponse.SC_FORBIDDEN));
	}
	
	/**
	 * <pre>
	 * Alice        Proxy       Bob
	 *   | INVITE     |          |
	 *   |----------->|          |
	 *   |            | INVITE   |
	 *   |            |--------->|
	 *   |            |      200 |
	 *   |            |<---------|
	 *   |        200 |          |
	 *   |<-----------|          |
	 *   | ACK        |          |
	 *   |----------->|          |
	 *   |            | ACK      |
	 *   |            |--------->|
	 *   | BYE        |          |
	 *   |----------->|          |
	 *   |            | BYE      |
	 *   |            |----------|
	 *   |            |      200 |
	 *   |            |<---------|
	 *   |        200 |          |
	 *   |<-----------|          |
	 * </pre>
	 */
	@Test
	public void testProxyRecordRoute() throws Throwable
	{
		testProxy(true);
	}
	
	/**
	 * While INVITE transaction is in ACCEPTED state, the session cannot be in
	 * invalidate-when-ready state. So a sleep should be done to ensure that transaction 
	 * is terminated before checking the state.
	 * <pre>
	 * Alice       Proxy       Bob
	 *   | INVITE    |          |
	 *   |---------->|          |
	 *   |           | INVITE   |
	 *   |           |--------->|
	 *   |           |      200 |
	 *   |           |<---------|
	 *   |       200 |          |
	 *   |<----------|          |
	 *   | ACK       |          |
	 *   |--------------------->|
	 *   | BYE       |          |
	 *   |--------------------->|
	 *   |           |      200 |
	 *   |<---------------------|
	 * </pre>
	 */
	@Test
	@Category(NotCompliantV2.class)
	public void testProxyNoRecordRoute() throws Throwable
	{
		testProxy(false);
	}

	public void testProxy(boolean recordRoute) throws Throwable 
	{
		Call callA;
		Endpoint bob = createEndpoint("bob");
		UaRunnable callB = new UasScript.OkBye(bob.getUserAgent());
		
		callB.start();
		
		SipServletRequest request = _ua.createRequest(SipMethods.INVITE, bob.getUri());
		request.setRequestURI(bob.getContact().getURI());
		request.addHeader(InvalidateWhenReadyServlet.RECORD_ROUTE,
				Boolean.toString(recordRoute));
		callA = _ua.createCall(request);

        assertThat(callA.waitForResponse(), isSuccess());
        callA.createAck().send();
        Thread.sleep(200);
        callA.createBye().send();
        assertThat(callA.waitForResponse(), isSuccess());
        
        callB.assertDone();
		if (!recordRoute)
		{
			LOG.info("Wait 32 seconds, to ensure INVITE transaction is terminated");
			Thread.sleep(32000);
		}
		checkForFailure();

	}
	
	/**
	 * If a 4xx is received, the session should be invalidated after forwarding the 4xx.
	 * 
	 * <pre>
	 *  Alice        Proxy       Bob
	 * 1  | INVITE     |          |
	 *    |----------->|          |
	 * 2  |            | INVITE   |
	 *    |            |--------->|
	 * 3  |            |      404 |
	 *    |            |<---------|
	 * 4  |            | ACK      |
	 *    |            |--------->|
	 * 5  |        404 |          |
	 *    |<-----------|          |
	 * 6  | ACK        |          |
	 *    |----------->|          |
	 * </pre>
	 */
	@Test
	@Category(NotCompliantV2.class)
	public void testProxy4xx() throws Throwable 
	{
		Call callA;
		Endpoint bob = createEndpoint("bob");
		UaRunnable callB = new UasScript.NotFound(bob.getUserAgent());
		
		callB.start();
		
		SipServletRequest request = _ua.createRequest(SipMethods.INVITE,  bob.getUri());
		request.setRequestURI(bob.getContact().getURI());
		callA = _ua.createCall(request);

		try 
		{
	        assertThat(callA.waitForResponse(), hasStatus(SipServletResponse.SC_NOT_FOUND));
	        callB.assertDone();
		}
		finally
		{
			checkForFailure();
		}
	}

	/**
	 * Test in sequential proxy case.
	 * 
	 * <pre>
	 *  Alice        Proxy       Bob        Bob(2)
	 * 1  | INVITE     |          |           |
	 *    |----------->|          |           |
	 * 2  |            | INVITE   |           |
	 *    |            |--------->|           |
	 * 3  |            |      404 |           |
	 *    |            |<---------|           |
	 * 4  |            | ACK      |           |
	 *    |            |--------->|           |
	 * 5  |            | INVITE   |           |
	 *    |            |--------------------->|
	 * 6  |            |          |       200 |
	 *    |            |<---------------------|
	 * 7  |        200 |          |           |
	 *    |<-----------|          |           |
	 * 8  | ACK        |          |           |
	 *    |----------->|          |           |
	 * 9  |            | ACK      |           |
	 *    |            |--------------------->|
	 * 10 | BYE        |          |           |
	 *    |----------->|          |           |
	 * 11 |            | BYE      |           |
	 *    |            |--------------------->|
	 * 12 |            |          |       200 |
	 *    |            |<---------------------|
	 * 13 |        200 |          |           |
	 *    |<-----------|          |           |
	 * </pre>
	 */
	@Test
	public void testProxySequential() throws Throwable 
	{
		Call callA;;
		Endpoint bob = createEndpoint("bob");
		UaRunnable callB = new UasScript.NotFound(bob.getUserAgent());
		Endpoint bob2 = createEndpoint("bob");
		UaRunnable callC = new UasScript.OkBye(bob2.getUserAgent());
		
		callB.start();
		callC.start();
		
		SipServletRequest request = _ua.createRequest(SipMethods.INVITE, bob.getUri());
		request.setRequestURI(bob.getContact().getURI());
		request.addHeader("proxy", bob2.getContact().toString());
		callA = _ua.createCall(request);

		try 
		{
	        assertThat(callA.waitForResponse(), isSuccess());
	        callA.createAck().send();
	        Thread.sleep(200);
	        callA.createBye().send();
	        assertThat(callA.waitForResponse(), isSuccess());

	        callB.assertDone();
	        callC.assertDone();
		}
		finally
		{
			checkForFailure();
		}
	}

	/**
	 * If a provisional response is received, then the session is in state
	 * EARLY.
	 * 
	 * <pre>
	 * Alice         Proxy       Bob        Bob(2)
	 *    | INVITE     |          |           |
	 *    |----------->|          |           |
	 *    |            | INVITE   |           |
	 *    |            |--------->|           |
	 *    |            |      180 |           |
	 *    |            |<---------|           |
	 *    |        180 |          |           |
	 *    |<-----------|          |           |
	 *    |            |      404 |           |
	 *    |            |<---------|           |
	 *    |            | ACK      |           |
	 *    |            |--------->|           |
	 *    |            | INVITE   |           |
	 *    |            |--------------------->|
	 *    |            |          |       180 |
	 *    |            |<---------------------|
	 *    |        180 |          |           |
	 *    |<-----------|          |           |
	 *    |            |          |       200 |
	 *    |            |<---------------------|
	 *    |        200 |          |           |
	 *    |<-----------|          |           |
	 *    | ACK        |          |           |
	 *    |----------->|          |           |
	 *    |            | ACK      |           |
	 *    |            |--------------------->|
	 *    | BYE        |          |           |
	 *    |----------->|          |           |
	 *    |            | BYE      |           |
	 *    |            |--------------------->|
	 *    |            |          |       200 |
	 *    |            |<---------------------|
	 *    |        200 |          |           |
	 *    |<-----------|          |           |
	 * </pre>
	 */
	@Test
	@Category(NotCompliantV2.class)
	public void testProxyProvisional4xx() throws Throwable 
	{
		Call callA;
		Endpoint bob = createEndpoint("bob");
		UaRunnable callB = new UasScript.RingingNotFound(bob.getUserAgent());
		Endpoint bob2 = createEndpoint("bob");
		UaRunnable callC = new UasScript.RingingOkBye(bob2.getUserAgent());
		
		callB.start();
		callC.start();
		
		SipServletRequest request = _ua.createRequest(SipMethods.INVITE, bob.getUri());
		request.setRequestURI(bob.getContact().getURI());
		request.addHeader("proxy", bob2.getContact().toString());
		callA = _ua.createCall(request);
			
		try
		{	
			assertThat(callA.waitForResponse(), hasStatus(SipServletResponse.SC_RINGING));
			assertThat(callA.waitForResponse(), hasStatus(SipServletResponse.SC_RINGING));
	        assertThat(callA.waitForResponse(), isSuccess());
	        callA.createAck().send();
	        Thread.sleep(500);
	        callA.createBye().send();
	        assertThat(callA.waitForResponse(), isSuccess());

	        callB.assertDone();
	        callC.assertDone();
		}
		catch (Throwable e) 
		{
			e.printStackTrace();
			throw e;
		}
        finally
        {
        	checkForFailure();
        }
	}
	
	/**
	 * In parallel mode, the session is cloned.
	 * 
	 * <pre>
	 * Alice        Proxy       Bob        Carol
	 *   | INVITE     |          |           |
	 *   |----------->|          |           |
	 *   |            | INVITE   |           |
	 *   |            |--------->|           |
	 *   |            | INVITE   |           |
	 *   |            |--------------------->|
	 *   |            |          |       180 |
	 *   |            |<---------------------|
	 *   |        180 |          |           |
	 *   |<-----------|          |           |
	 *   |            |      180 |           |
	 *   |            |<---------|           |
	 *   |        180 |          |           |
	 *   |<-----------|          |           |
	 *   |            |      404 |           |
	 *   |            |<---------|           |
	 *   |            | ACK      |           |
	 *   |            |--------->|           |
	 *   |            |          |       200 |
	 *   |            |<---------------------|
	 *   |        200 |          |           |
	 *   |<-----------|          |           |
	 *   | ACK        |          |           |
	 *   |----------->|          |           |
	 *   |            | ACK      |           |
	 *   |            |--------------------->|
	 *   | BYE        |          |           |
	 *   |----------->|          |           |
	 *   |            | BYE      |           |
	 *   |            |--------------------->|
	 *   |            |          |       200 |
	 *   |            |<---------------------|
	 *   |        200 |          |           |
	 *   |<-----------|          |           |
	 * </pre>
	 */
	@Test
	@Category(NotCompliantV2.class)
	public void testProxyParallel() throws Throwable 
	{
		Call callA;
		Endpoint bob = createEndpoint("bob");
		UaRunnable callB = new UasScript.RingingNotFound(bob.getUserAgent());
		Endpoint bob2 = createEndpoint("bob");
		UaRunnable callC = new UasScript.RingingOkBye(bob2.getUserAgent());
		
		callB.start();
		callC.start();
		
		SipServletRequest request = _ua.createRequest(SipMethods.INVITE, bob.getUri());
		request.setRequestURI(bob.getContact().getURI());
		request.addHeader("proxy", bob2.getContact().toString());
		callA = _ua.createCall(request);
		

		assertThat(callA.waitForResponse(), hasStatus(SipServletResponse.SC_RINGING));
		assertThat(callA.waitForResponse(), hasStatus(SipServletResponse.SC_RINGING));
        assertThat(callA.waitForResponse(), isSuccess());
        callA.createAck().send();
        Thread.sleep(500);
        callA.createBye().send();
        assertThat(callA.waitForResponse(), isSuccess());

        callB.assertDone();
        callC.assertDone();

    	checkForFailure();

	}
	
	/**
	 * In parallel mode, the session is cloned.
	 * 
	 * <pre>
	 * Alice        Proxy         Bob        Bob(2)
	 *   | INVITE     |            |           |
	 *   |----------->|            |           |
	 *   |            | INVITE     |           |
	 *   |            |----------->|           |
	 *   |            | INVITE     |           |
	 *   |            |----------------------->|
	 *   |            |            |       180 |
	 *   |            |<-----------------------|
	 *   |            |        180 |           |
	 *   |            |<-----------|           |
	 *   |            |            |       200 |
	 *   |            |<-----------------------|
	 *   |        200 |            |           |
	 *   |<-----------|            |           |
	 *   |            | CANCEL     |           |
	 *   |            |----------->|           |
	 *   |            | 200/CANCEL |           |
	 *   |            |<-----------|           |
	 *   |            | 487/INVITE |           |
	 *   |            |<-----------|           |
	 *   |            | ACK        |           |
	 *   |            |----------->|           |
	 *   | ACK        |            |           |
	 *   |----------->|            |           |
	 *   |            | ACK        |           |
	 *   |            |----------------------->|
	 *   | BYE        |            |           |
	 *   |----------->|            |           |
	 *   |            | BYE        |           |
	 *   |            |----------------------->|
	 *   |            |            |       200 |
	 *   |            |<-----------------------|
	 *   |        200 |            |           |
	 *   |<-----------|            |           |
	 * </pre>
	 */
	@Test
	@Category(NotCompliantV2.class)
	public void testProxyCancel1() throws Throwable 
	{
		testProxyCancel(false);
	}
	
	/**
	 * In parallel mode, the session is cloned.
	 * 
	 * <pre>
	 * Alice        Proxy         Bob        Bob(2)
	 *   | INVITE     |            |           |
	 *   |----------->|            |           |
	 *   |            | INVITE     |           |
	 *   |            |----------->|           |
	 *   |            | INVITE     |           |
	 *   |            |----------------------->|
	 *   |            |        180 |           |
	 *   |            |<-----------|           |
	 *   |            |            |       180 |
	 *   |            |<-----------------------|
	 *   |            |            |       200 |
	 *   |            |<-----------------------|
	 *   |        200 |            |           |
	 *   |<-----------|            |           |
	 *   |            | CANCEL     |           |
	 *   |            |----------->|           |
	 *   |            | 200/CANCEL |           |
	 *   |            |<-----------|           |
	 *   |            | 487/INVITE |           |
	 *   |            |<-----------|           |
	 *   |            | ACK        |           |
	 *   |            |----------->|           |
	 *   | ACK        |            |           |
	 *   |----------->|            |           |
	 *   |            | ACK        |           |
	 *   |            |----------------------->|
	 *   | BYE        |            |           |
	 *   |----------->|            |           |
	 *   |            | BYE        |           |
	 *   |            |----------------------->|
	 *   |            |            |       200 |
	 *   |            |<-----------------------|
	 *   |        200 |            |           |
	 *   |<-----------|            |           |
	 * </pre>
	 */
	@Test
	@Category(NotCompliantV2.class)
	public void testProxyCancel2() throws Throwable 
	{
		testProxyCancel(true);
	}
	
	public void testProxyCancel(final boolean bobReplyFirst) throws Throwable 
	{
		final Semaphore semaphore = new Semaphore(0);
		Call callA = (Call) _ua.customize(new Call());

		Endpoint bob = createEndpoint("bob");
		UaRunnable callB = new UaRunnable(bob.getUserAgent())
		{
			@Override
			public void doTest() throws Throwable
			{
				SipServletRequest request = waitForInitialRequest();
				assertThat(request.getMethod(), is(SipMethods.INVITE));
				
				LOG.debug("Bob1 got request " + request);
				if (!bobReplyFirst)
				{
					LOG.info("Waiting for semaphore release");
					semaphore.acquire();
					Thread.sleep(50); // Ensure that the response will be proceeded after the other leg
				}

				_ua.createResponse(request, SipServletResponse.SC_RINGING).send();
				semaphore.release();
				request = _dialog.waitForRequest();
				assertThat(request.getMethod(), is(SipMethods.CANCEL));
			}
		};

		Endpoint bob2 = createEndpoint("bob");
		UaRunnable callC = new UaRunnable(bob2.getUserAgent())
		{
			@Override
			public void doTest() throws Throwable
			{
				SipServletRequest request = waitForInitialRequest();
				assertThat(request.getMethod(), is(SipMethods.INVITE));
				LOG.debug("Bob 2 got request " + request);

				if (bobReplyFirst)
				{
					LOG.info("Waiting for semaphore release");
					semaphore.acquire();
					Thread.sleep(50); // Ensure that the response will be proceeded after the other leg
				}

				_ua.createResponse(request, SipServletResponse.SC_RINGING).send();
				semaphore.release();
				Thread.sleep(500);
				_ua.createResponse(request, SipServletResponse.SC_OK).send();
				request = _dialog.waitForRequest();
				assertThat(request.getMethod(), is(equalTo(SipMethods.ACK)));
				request = _dialog.waitForRequest();
				assertThat(request.getMethod(), is(equalTo(SipMethods.BYE)));
				_ua.createResponse(request, SipServletResponse.SC_OK).send();
			}
		};

		callB.start();
		callC.start();
		
		SipServletRequest request = callA.createInitialInvite(
				_ua.getFactory().createURI(getFrom()),
				_ua.getFactory().createURI(bob.getUri()));
		request.setRequestURI(bob.getContact().getURI());
		request.addHeader("proxy", bob2.getContact().toString());
		callA.start(request);

		assertThat(callA.waitForResponse(), hasStatus(SipServletResponse.SC_RINGING));
		assertThat(callA.waitForResponse(), hasStatus(SipServletResponse.SC_RINGING));
        assertThat(callA.waitForResponse(), isSuccess());
        callA.createAck().send();
        Thread.sleep(500);
        callA.createBye().send();
        assertThat(callA.waitForResponse(), isSuccess());

        callB.assertDone();
        callC.assertDone();
    	checkForFailure();

	}
	
	
	/**
	 * Parallel then sequential.
	 * 
	 * <pre>
	 * Alice        Proxy       Bob        Carol           Dan
	 *   | INVITE     |          |           |              |
	 *   |----------->|          |           |              |
	 *   |            | INVITE   |           |              |
	 *   |            |--------->|           |              |
	 *   |            | INVITE   |           |              |
	 *   |            |--------------------->|              |
	 *   |            |      180 |           |              |
	 *   |            |<---------|           |              |
	 *   |        180 |          |           |              |
	 *   |<-----------|          |           |              |
	 *   |            |          |       180 |              |
	 *   |            |<---------------------|              |
	 *   |        180 |          |           |              |
	 *   |<-----------|          |           |              |
	 *   |            |      404 |           |              |
	 *   |            |<---------|           |              |
	 *   |            | ACK      |           |              |
	 *   |            |--------->|           |              |
	 *   |            |          |       404 |              |
	 *   |            |<---------------------|              |
	 *   |            | ACK      |           |              |
	 *   |            |--------------------->|              |
	 *   |            | INVITE   |           |              |
	 *   |            |------------------------------------>|
	 *   |            |          |           |          200 |
	 *   |            |<---------------------|--------------|
	 *   |        200 |          |           |              |
	 *   |<-----------|          |           |              |
	 *   | ACK        |          |           |              |
	 *   |----------->|          |           |              |
	 *   |            | ACK      |           |              |
	 *   |            |------------------------------------>|
	 *   | BYE        |          |           |              |
	 *   |----------->|          |           |              |
	 *   |            | BYE      |           |              |
	 *   |            |------------------------------------>|
	 *   |            |          |           |          200 |
	 *   |            |<------------------------------------|
	 *   |        200 |          |           |              |
	 *   |<-----------|          |           |              |
	 * </pre>
	 */

	
	/**
	 * Two proxies:
	 * <ul>
	 * <li>Proxy 1 is record route.
	 * <li>Proxy 2 is not record route and forks to Bob and Carol.
	 * </ul>
	 *  
	 * <pre>
	 * Alice       Proxy 1    Proxy 2       Bob           Carol
	 *   | INVITE     |          |           |              |
	 *   |----------->|          |           |              |
	 *   |            | INVITE   |           |              |
	 *   |            |--------->|           |              |
	 *   |            |          | INVITE    |              |
	 *   |            |          |---------->|              |
	 *   |            |          | INVITE    |              |
	 *   |            |          |------------------------->|
	 *   |            |          |       180 |              |
	 *   |            |          |<----------|              |
	 *   |            |      180 |           |              |
	 *   |            |<---------|           |              |
	 *   |        180 |          |           |              |
	 *   |<-----------|          |           |              |
	 *   |            |          |       180 |              |
	 *   |            |          |<-------------------------|
	 *   |            |      180 |           |              |
	 *   |            |<---------|           |              |
	 *   |        180 |          |           |              |
	 *   |<-----------|          |           |              |
	 *   |            |          |       404 |              |
	 *   |            |          |<----------|              |
	 *   |            |          | ACK       |              |
	 *   |            |          |---------->|              |
	 *   |            |          |           |          200 |
	 *   |            |          |<-------------------------|
	 *   |            |      200 |           |              |
	 *   |            |<---------|           |              |
	 *   |        200 |          |           |              |
	 *   |<-----------|          |           |              |
	 *   | ACK        |          |           |              |
	 *   |----------->|          |           |              |
	 *   |            | ACK      |           |              |
	 *   |            |------------------------------------>|
	 *   | BYE        |          |           |              |
	 *   |----------->|          |           |              |
	 *   |            | BYE      |           |              |
	 *   |            |------------------------------------>|
	 *   |            |          |           |          200 |
	 *   |            |<------------------------------------|
	 *   |        200 |          |           |              |
	 *   |<-----------|          |           |              |
	 * </pre>
	 */
	@Test
	public void testTwoProxy() throws Throwable
	{
		Call callA;
		
		Endpoint bob = createEndpoint("bob");
		UaRunnable callB = new UasScript.RingingNotFound(bob.getUserAgent());
		Endpoint carol = createEndpoint("bob");
		UaRunnable callC = new UasScript.RingingOkBye(carol.getUserAgent());

		callB.start();
		callC.start();
		
		SipServletRequest request = _ua.createRequest(SipMethods.INVITE, bob.getUri());
		request.setRequestURI(bob.getContact().getURI());
		request.addHeader("proxy", carol.getContact().toString());
		callA = _ua.createCall(request);
		        
		assertThat(callA.waitForResponse(), hasStatus(SipServletResponse.SC_RINGING));
		assertThat(callA.waitForResponse(), hasStatus(SipServletResponse.SC_RINGING));
		assertThat(callA.waitForResponse(), isSuccess());

		callA.createAck().send();

		Thread.sleep(500);
		callA.createBye().send();
		assertThat(callA.waitForResponse(), isSuccess());

		callB.assertDone();
		callC.assertDone();

		checkForFailure();

	}

	/**
	 * <pre>
	 * Alice                         AS
	 *   | SUBSCRIBE                  |
	 *   |--------------------------->|
	 *   | expires=0                  |
	 *   |                        200 |
	 *   |<---------------------------|
 	 *   |                     NOTIFY |
	 *   |<---------------------------|
	 *   |                        200 |
	 *   |--------------------------->|
	 *   |                            |
	 * </pre>
	 */
	@Test
	@Category(NotCompliantV2.class)
	public void testSubscribe() throws Throwable 
	{
		Dialog dialog = _ua.customize(new Dialog());
		SipServletRequest request = dialog.createInitialRequest(
				SipMethods.SUBSCRIBE, _sipClient.getFactory().createURI(getFrom()),
				_sipClient.getFactory().createURI(createEndpoint("bob").getUri()));
		request.addHeader("Event", "presence");
		request.setExpires(0);
		dialog.start(request);
		assertThat(dialog.waitForResponse(), isSuccess());
		
		request = dialog.waitForRequest();
		assertThat(request.getMethod(), is(equalTo(SipMethods.NOTIFY)));
		_ua.createResponse(request, SipServletResponse.SC_OK).send();
		
        checkForFailure();
	}
	
	/**
	 * <pre>
	 * Alice                         AS
	 *   | SUBSCRIBE                  |
	 *   | expires=60                 |
	 *   |--------------------------->|
	 *   |                        200 |
	 *   |<---------------------------|
 	 *   |                     NOTIFY |
	 *   |<---------------------------|
	 *   |                        200 |
	 *   |--------------------------->|
	 *   |                            | 
	 *   | SUBSCRIBE                  |
	 *   | expires=0                  |
	 *   |--------------------------->|
	 *   |                        200 |
	 *   |<---------------------------|
 	 *   |                     NOTIFY |
	 *   |<---------------------------|
	 *   |                        200 |
	 *   |--------------------------->|
	 *   |                            |
	 * </pre>
	 */
	@Test
	@Category(NotCompliantV2.class)
	public void testSubscribe2() throws Throwable 
	{
		Subscriber subscriber = new Subscriber("presence", _ua.customize(new Dialog()));
		SipServletResponse response = subscriber.startSubscription(
				_sipClient.getFactory().createAddress(getFrom()), 
				_sipClient.getFactory().createAddress(createEndpoint("bob").getUri()), 
				60);

		assertThat(response, isSuccess());
		
		SipServletRequest request = subscriber.waitForNotify();
		_ua.createResponse(request, SipServletResponse.SC_OK).send();
		
		response = subscriber.stopSubscription();
		assertThat(response, isSuccess());
		
		request = subscriber.waitForNotify();
		_ua.createResponse(request, SipServletResponse.SC_OK).send();
		
        checkForFailure();
	}
	
	/**
	 * <pre>
	 * Alice                         AS
	 *   | MESSAGE                    |
	 *   |--------------------------->|
	 *   |                        200 |
	 *   |<---------------------------|
	 *   |                  SUBSCRIBE |
	 *   |                 expires=60 |
	 *   |<---------------------------|
	 *   | 200                        |
	 *   |--------------------------->|
 	 *   |NOTIFY                      |
	 *   |--------------------------->|
	 *   |                        200 |
	 *   |<---------------------------|	 
	 *   |                  SUBSCRIBE |
	 *   |                  expires=0 |
	 *   |<---------------------------|
	 *   | 200                        |
	 *   |--------------------------->|
 	 *   |NOTIFY                      |
	 *   |--------------------------->|
	 *   |                        200 |
	 *   |<---------------------------|
	 *   |                            |
	 * </pre>
	 */
	@Test
	@Category(NotCompliantV2.class)
	public void testUacSubscribe() throws Throwable 
	{
		UaRunnable call = new UaRunnable(_ua)
		{
			
			@Override
			public void doTest() throws Throwable
			{
				SipServletRequest request = waitForInitialRequest();
				SipServletResponse response = _ua.createResponse(request, SipServletResponse.SC_OK);
				response.setExpires(60);
				response.send();
				
				Thread.sleep(20);
				request = _dialog.createRequest(SipMethods.NOTIFY);
				request.setHeader(SipHeaders.SUBSCRIPTION_STATE, "active;expires=60");
				request.send();	
				assertThat(_dialog.waitForFinalResponse(), isSuccess());
				
				request = _dialog.waitForRequest();
				response = _ua.createResponse(request, SipServletResponse.SC_OK);
				response.setExpires(0);
				response.send();
				
				request =  _dialog.createRequest(SipMethods.NOTIFY);
				request.setHeader(SipHeaders.SUBSCRIPTION_STATE, "terminated");
				request.send();	
				
				assertThat(_dialog.waitForFinalResponse(), isSuccess());
			}
		};

		call.start();
		startUacScenario();
		call.join(2000);
		call.assertDone();
		checkForFailure();
	}
}
