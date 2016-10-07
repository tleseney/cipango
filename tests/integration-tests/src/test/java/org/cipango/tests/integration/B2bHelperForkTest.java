// ========================================================================
// Copyright 2007-2012 NEXCOM Systems
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
import static org.hamcrest.Matchers.is;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;

import org.cipango.client.Call;
import org.cipango.client.SipHeaders;
import org.cipango.client.SipMethods;
import org.cipango.client.test.TestAgent;
import org.cipango.client.test.UaRunnable;
import org.cipango.server.session.SessionManager.ApplicationSessionScope;
import org.cipango.tests.UaTestCase;
import org.junit.Test;

public class B2bHelperForkTest extends UaTestCase
{
	
	/**
	 * <pre>
	 * SipUnit     B2bHelper      proxy       bob      carol
	 *  |  INVITE     |             |          |          |
	 *  |------------>|             |          |          |
	 *  |             |INVITE       |          |          |
	 *  |             |------------>|          |          |
	 *  |             |             |INVITE    |          |
	 *  |             |             |--------->|          |
	 *  |             |             |INVITE    |          |
	 *  |             |             |-------------------->|
	 *  |             |             |      200 |          |
	 *  |             |             |<---------|          |
	 *  |             |         200 |          |          |
	 *  |             |<------------|          |          |
	 *  |         200 |             |          |          |
	 *  |<------------|             |          |          |
	 *  |             |             |          |      200 |
	 *  |             |             |<--------------------|
	 *  |             |         200 |          |          |
	 *  |             |<------------|          |          |
	 *  |         200 |             |          |          |
	 *  |<------------|             |          |          |
	 *  | ACK         |             |          |          |
	 *  |------------>|             |          |          |
	 *  |             |ACK          |          |          |
	 *  |             |----------------------->|          |
	 *  | ACK         |             |          |          |
	 *  |------------>|             |          |          |
	 *  |             |ACK          |          |          |
	 *  |             |---------------------------------->|
	 *  </pre>
	 *  
	 *  
	 */
	@Test
	public void testMultipleResponse() throws Throwable 
	{

		Endpoint bob = createEndpoint("bob");
		UaRunnable replyB = new NoProvisionalReply(bob.getUserAgent());
		Endpoint carol = createEndpoint("bob", "carol");
		UaRunnable replyC = new NoProvisionalReply(carol.getUserAgent());
		replyB.start();
		replyC.start();
		Thread.sleep(10);
		
		SipServletRequest request = _ua.createRequest(SipMethods.INVITE,  bob.getUri());
		request.addAddressHeader(B2bHelperProxyServlet.PROXY_URIS, bob.getContact(), true);
		request.addAddressHeader(B2bHelperProxyServlet.PROXY_URIS, carol.getContact(), true);
		
		Call callA = _ua.createCall(request);

		SipServletResponse response = callA.waitForResponse();
        assertThat(response, isSuccess());
        response.createAck().send();
        
        SipServletResponse response2 = callA.waitForResponse();
        assertThat(response2, isSuccess());
        response2.createAck().send();
				
		assertNotSame(response.getSession(), response2.getSession());
			
		Thread.sleep(2000);
		response.getSession().createRequest(SipMethods.BYE).send();
		assertThat(callA.waitForResponse(), isSuccess());
		
		response2.getSession().createRequest(SipMethods.BYE).send();
		assertThat(callA.waitForResponse(), isSuccess());
		
		replyB.assertDone();
		replyC.assertDone();
	}
		
	/**
	 * <pre>
	 * SipUnit     B2bHelper      proxy       bob      carol
	 *  |  INVITE     |             |          |          |
	 *  |------------>|             |          |          |
	 *  |             |INVITE       |          |          |
	 *  |             |------------>|          |          |
	 *  |             |             |INVITE    |          |
	 *  |             |             |--------->|          |
	 *  |             |             |INVITE    |          |
	 *  |             |             |-------------------->|
	 *  |             |             |      180 |          |
	 *  |             |             |<---------|          |
	 *  |             |         180 |          |          |
	 *  |             |<------------|          |          |
	 *  |         180 |             |          |          |
	 *  |<------------|             |          |          |
	 *  |             |             |          |      180 |
	 *  |             |             |<--------------------|
	 *  |             |         180 |          |          |
	 *  |             |<------------|          |          |
	 *  |         180 |             |          |          |
	 *  |<------------|             |          |          |
	 *  |             |             |      200 |          |
	 *  |             |             |<---------|          |
	 *  |             |         200 |          |          |
	 *  |             |<------------|          |          |
	 *  |         200 |             |          |          |
	 *  |<------------|             |          |          |
	 *  |             |             |          |      200 |
	 *  |             |             |<--------------------|
	 *  |             |         200 |          |          |
	 *  |             |<------------|          |          |
	 *  |         200 |             |          |          |
	 *  |<------------|             |          |          |
	 *  | ACK         |             |          |          |
	 *  |------------>|             |          |          |
	 *  |             |ACK          |          |          |
	 *  |             |----------------------->|          |
	 *  | ACK         |             |          |          |
	 *  |------------>|             |          |          |
	 *  |             |ACK          |          |          |
	 *  |             |---------------------------------->|
	 *  </pre>
	 *  
	 *  
	 */
	@Test
	public void testMultipleProvisionalResponse() throws Throwable 
	{
		Endpoint bob = createEndpoint("bob");
		UaRunnable replyB = new RingingReply(bob.getUserAgent());
		Endpoint carol = createEndpoint("bob", "carol");
		UaRunnable replyC = new RingingReply(carol.getUserAgent());
		replyB.start();
		replyC.start();
		Thread.sleep(10);
		
		SipServletRequest request = _ua.createRequest(SipMethods.INVITE,  bob.getUri());
		request.addAddressHeader(B2bHelperProxyServlet.PROXY_URIS, bob.getContact(), true);
		request.addAddressHeader(B2bHelperProxyServlet.PROXY_URIS, carol.getContact(), true);
		
		Call callA = _ua.createCall(request);
		
		SipServletResponse response = callA.waitForResponse();
		assertThat(response, hasStatus(SipServletResponse.SC_RINGING));
		SipSession session1 = response.getSession();
		
		response = callA.waitForResponse();
		assertThat(response, hasStatus(SipServletResponse.SC_RINGING));
		SipSession session2 = response.getSession();
		assertNotSame(session1.getId(), session2.getId());
		
		response = callA.waitForResponse();
        assertThat(response, isSuccess());
        response.createAck().send();
        
        SipServletResponse response2 = callA.waitForResponse();
        assertThat(response2, isSuccess());
        response2.createAck().send();
				
		Thread.sleep(2000);
		response.getSession().createRequest(SipMethods.BYE).send();
		assertThat(callA.waitForResponse(), isSuccess());
		
		response2.getSession().createRequest(SipMethods.BYE).send();
		assertThat(callA.waitForResponse(), isSuccess());
		
		replyB.assertDone();
		replyC.assertDone();
	}
	
	
	/**
	 * Should be able to create negative response on forked session
	 * using B2bUaHelper.createResponseToOriginalRequest().
	 * 
	 * <pre>
	 * SipUnit     B2bHelper      proxy       bob      carol
	 *  |  INVITE     |             |          |          |
	 *  |------------>|             |          |          |
	 *  |             |INVITE       |          |          |
	 *  |             |------------>|          |          |
	 *  |             |             |INVITE    |          |
	 *  |             |             |--------->|          |
	 *  |             |             |INVITE    |          |
	 *  |             |             |-------------------->|
	 *  |             |             |      180 |          |
	 *  |             |             |<---------|          |
	 *  |             |         180 |          |          |
	 *  |             |<------------|          |          |
	 *  |         180 |             |          |          |
	 *  |<------------|             |          |          |
	 *  |             |             |          |      180 |
	 *  |             |             |<--------------------|
	 *  |             |         180 |          |          |
	 *  |             |<------------|          |          |
	 *  |         180 |             |          |          |
	 *  |<------------|             |          |          |
	 *  |             |             |      404 |          |
	 *  |             |             |<---------|          |
	 *  |             |             |ACK       |          |
	 *  |             |             |--------->|          |
	 *  |             |             |          |      603 |
	 *  |             |             |<--------------------|
	 *  |             |             | ACK      |          |
	 *  |             |             |-------------------->|
	 *  |             |         603 |          |          |
	 *  |             |<------------|          |          |
	 *  |             | ACK         |          |          |
	 *  |             |------------>|          |          |
	 *  |         603 |             |          |          |
	 *  |<------------|             |          |          |
	 *  | ACK         |             |          |          |
	 *  |------------>|             |          |          |
	 *  </pre>
	 */
	@Test
	public void testNegativeOnForkedSessionResponse() throws Throwable 
	{
		Endpoint bob = createEndpoint("bob");
		UaRunnable replyB = new NegativeReply(bob.getUserAgent(), 10, SipServletResponse.SC_NOT_FOUND);
		Endpoint carol = createEndpoint("bob", "carol");
		UaRunnable replyC = new NegativeReply(carol.getUserAgent(), 200, SipServletResponse.SC_DECLINE);
		replyB.start();
		replyC.start();
		Thread.sleep(10);
		
		SipServletRequest request = _ua.createRequest(SipMethods.INVITE,  bob.getUri());
		request.addAddressHeader(B2bHelperProxyServlet.PROXY_URIS, bob.getContact(), true);
		request.addAddressHeader(B2bHelperProxyServlet.PROXY_URIS, carol.getContact(), true);
		
		Call callA = _ua.createCall(request);
		
		SipServletResponse response = callA.waitForResponse();
		assertThat(response, hasStatus(SipServletResponse.SC_RINGING));
		SipSession session1 = response.getSession();
		
		response = callA.waitForResponse();
		assertThat(response, hasStatus(SipServletResponse.SC_RINGING));
		SipSession session2 = response.getSession();
		assertNotSame(session1.getId(), session2.getId());
		
		response = callA.waitForResponse();
        assertThat(response, hasStatus(SipServletResponse.SC_DECLINE));
       		
		replyB.assertDone();
		replyC.assertDone();
	}
	
	
	/**
	 * <pre>
	 * SipUnit     B2bHelper      proxy       bob      carol
	 *  |  INVITE     |             |          |          |
	 *  |------------>|             |          |          |
	 *  |             |INVITE       |          |          |
	 *  |             |------------>|          |          |
	 *  |             |             |INVITE    |          |
	 *  |             |             |--------->|          |
	 *  |             |             |INVITE    |          |
	 *  |             |             |-------------------->|
	 *  |             |             |      180 |          |
	 *  |             |             |<---------|          |
	 *  |             |         180 |          |          |
	 *  |             |<------------|          |          |
	 *  |         180 |             |          |          |
	 *  |<------------|             |          |          |
	 *  |             |             |          |      180 |
	 *  |             |             |<--------------------|
	 *  |             |         180 |          |          |
	 *  |             |<------------|          |          |
	 *  |         180 |             |          |          |
	 *  |<------------|             |          |          |
	 *  |             |             |      200 |          |
	 *  |             |             |<---------|          |
	 *  |             |         200 |          |          |
	 *  |             |<------------|          |          |
	 *  |         200 |             |          |          |
	 *  |<------------|             |          |          |
	 *  |             |             |CANCEL    |          |
	 *  |             |             |-------------------->|
	 *  |             |             |          |200/CANCEL|
	 *  |             |             |<--------------------|
	 *  |             |             |          |487/INVITE|
	 *  |             |             |<--------------------|
	 *  |             |             |ACK       |          |
	 *  |             |             |-------------------->|
	 *  | ACK         |             |          |          |
	 *  |------------>|             |          |          |
	 *  |             |ACK          |          |          |
	 *  |             |----------------------->|          |
	 *  </pre>
	 *  
	 *  
	 */
	@Test
	public void testMultipleProvisionalResponse2() throws Throwable 
	{
		
		Endpoint bob = createEndpoint("bob");
		UaRunnable replyB = new RingingReply(bob.getUserAgent());
		Endpoint carol = createEndpoint("bob", "carol");
		UaRunnable replyC = new CancelReply(carol.getUserAgent());
		replyB.start();
		replyC.start();
		Thread.sleep(10);
		
		SipServletRequest request = _ua.createRequest(SipMethods.INVITE,  bob.getUri());
		request.addAddressHeader(B2bHelperProxyServlet.PROXY_URIS, bob.getContact(), true);
		request.addAddressHeader(B2bHelperProxyServlet.PROXY_URIS, carol.getContact(), true);
		
		Call callA = _ua.createCall(request);
		
		SipServletResponse response = callA.waitForResponse();
		assertThat(response, hasStatus(SipServletResponse.SC_RINGING));
		SipSession session1 = response.getSession();
		
		response = callA.waitForResponse();
		assertThat(response, hasStatus(SipServletResponse.SC_RINGING));
		SipSession session2 = response.getSession();
		assertNotSame(session1.getId(), session2.getId());
		
		response = callA.waitForResponse();
        assertThat(response, isSuccess());
        response.createAck().send();
        
     				
		Thread.sleep(2000);
		response.getSession().createRequest(SipMethods.BYE).send();
		assertThat(callA.waitForResponse(), isSuccess());
				
		replyB.assertDone();
		replyC.assertDone();
	}
	
	/**
	 * <pre>
	 * SipUnit     B2bHelper      proxy       bob      carol
	 *  |  INVITE     |             |          |          |
	 *  |------------>|             |          |          |
	 *  |             |INVITE       |          |          |
	 *  |             |------------>|          |          |
	 *  |             |             |INVITE    |          |
	 *  |             |             |--------->|          |
	 *  |             |             |INVITE    |          |
	 *  |             |             |-------------------->|
	 *  |             |             |      183 |          |
	 *  |             |             |<---------|          |
	 *  |             |         183 |          |          |
	 *  |             |<------------|          |          |
	 *  |         183 |             |          |          |
	 *  |<------------|             |          |          |
	 *  |  PRACK      |             |          |          |
	 *  |------------>|             |          |          |
	 *  |             |PRACK        |          |          |
	 *  |             |----------------------->|          |
	 *  |             |             |200/PRACK |          |
	 *  |             |<-----------------------|          |
	 *  |   200/PRACK |             |          |          |
	 *  |<------------|             |          |          |
	 *  |             |             |          |      183 |
	 *  |             |             |<--------------------|
	 *  |             |         183 |          |          |
	 *  |             |<------------|          |          |
	 *  |         183 |             |          |          |
	 *  |<------------|             |          |          |
	 *  |  PRACK      |             |          |          |
	 *  |------------>|             |          |          |
	 *  |             |PRACK        |          |          |
	 *  |             |---------------------------------->|
	 *  |             |             |          |200/PRACK |
	 *  |             |<----------------------------------|
	 *  |   200/PRACK |             |          |          |
	 *  |<------------|             |          |          |
	 *  |             |             |      200 |          |
	 *  |             |             |<---------|          |
	 *  |             |         200 |          |          |
	 *  |             |<------------|          |          |
	 *  |         200 |             |          |          |
	 *  |<------------|             |          |          |
	 *  |             |             |          |      200 |
	 *  |             |             |<--------------------|
	 *  |             |         200 |          |          |
	 *  |             |<------------|          |          |
	 *  |         200 |             |          |          |
	 *  |<------------|             |          |          |
	 *  | ACK         |             |          |          |
	 *  |------------>|             |          |          |
	 *  |             |ACK          |          |          |
	 *  |             |----------------------->|          |
	 *  | ACK         |             |          |          |
	 *  |------------>|             |          |          |
	 *  |             |ACK          |          |          |
	 *  |             |---------------------------------->|
	 *  </pre>
	 *  
	 */
	@Test
	public void testMultipleResponse100rel() throws Throwable 
	{
		Endpoint bob = createEndpoint("bob");
		UaRunnable replyB = new SessionProgressReply(bob.getUserAgent());
		Endpoint carol = createEndpoint("bob", "carol");
		UaRunnable replyC = new SessionProgressReply(carol.getUserAgent());
		replyB.start();
		replyC.start();
		Thread.sleep(10);
		
		SipServletRequest request = _ua.createRequest(SipMethods.INVITE,  bob.getUri());
		request.addAddressHeader(B2bHelperProxyServlet.PROXY_URIS, bob.getContact(), true);
		request.addAddressHeader(B2bHelperProxyServlet.PROXY_URIS, carol.getContact(), true);
		request.addHeader(SipHeaders.SUPPORTED, "100rel");
		
		Call callA = _ua.createCall(request);
		
		SipServletResponse response1 = callA.waitForResponse();
		assertThat(response1, hasStatus(SipServletResponse.SC_SESSION_PROGRESS));
		SipSession session1 = response1.getSession();
		
		SipServletResponse response2 = callA.waitForResponse();
		assertThat(response2, hasStatus(SipServletResponse.SC_SESSION_PROGRESS));
		SipSession session2 = response2.getSession();

		assertNotSame(session1.getId(), session2.getId());		
		
		_ua.decorate(response1.createPrack()).send();
		_ua.decorate(response2.createPrack()).send();
		
		response1 = callA.waitForResponse();
	    assertThat(response1, isSuccess());
	    assertThat(response1.getMethod(), is(SipMethods.PRACK));
	    
	    response2 = callA.waitForResponse();
	    assertThat(response2, isSuccess());
	    assertThat(response2.getMethod(), is(SipMethods.PRACK));
	    
		
		response1 = callA.waitForResponse();
        assertThat(response1, isSuccess());
	    assertThat(response1.getMethod(), is(SipMethods.INVITE));
	    _ua.decorate(response1.createAck()).send();
        
    	response2 = callA.waitForResponse();
        assertThat(response2, isSuccess());
	    assertThat(response2.getMethod(), is(SipMethods.INVITE));
	    _ua.decorate(response2.createAck()).send();
        
     				
		Thread.sleep(2000);
		_ua.decorate(session1.createRequest(SipMethods.BYE)).send();
		assertThat(callA.waitForResponse(), isSuccess());
		
		_ua.decorate(session2.createRequest(SipMethods.BYE)).send();
		assertThat(callA.waitForResponse(), isSuccess());
				
		replyB.assertDone();
		replyC.assertDone();
	}
		
	/**
	 * <pre>
	 * SipUnit     B2bHelper      proxy       bob      carol
	 *  |  INVITE     |             |          |          |
	 *  |------------>|             |          |          |
	 *  |             |INVITE       |          |          |
	 *  |             |------------>|          |          |
	 *  |             |             |INVITE    |          |
	 *  |             |             |--------->|          |
	 *  |             |             |INVITE    |          |
	 *  |             |             |-------------------->|
	 *  |             |             |      180 |          |
	 *  |             |             |<---------|          |
	 *  |             |         180 |          |          |
	 *  |             |<------------|          |          |
	 *  |         180 |             |          |          |
	 *  |<------------|             |          |          |
	 *  |             |             |          |      180 |
	 *  |             |             |<--------------------|
	 *  |             |         180 |          |          |
	 *  |             |<------------|          |          |
	 *  |         180 |             |          |          |
	 *  |<------------|             |          |          |
	 *  |  CANCEL     |             |          |          |
	 *  |------------>|             |          |          |
	 *  |  200/CANCEL |             |          |          |
	 *  |<------------|             |          |          |
	 *  |             |CANCEL       |          |          |
	 *  |             |------------>|          |          |
	 *  |  487/INVITE |             |          |          |
	 *  |<------------|             |          |          |
	 *  | ACK         |             |          |          |
	 *  |------------>|             |          |          |
	 *  |             |             |CANCEL    |          |
	 *  |             |             |--------->|          |
	 *  |             |  200/CANCEL |          |          |
	 *  |             |<------------|          |          |
	 *  |             |             |CANCEL    |          |
	 *  |             |             |-------------------->|
	 *  |             |             |          |200/CANCEL|
	 *  |             |             |<--------------------|
	 *  |             |             |          |487/INVITE|
	 *  |             |             |<--------------------|
	 *  |             |             |ACK       |          |
	 *  |             |             |-------------------->|
	 *  |             |             |200/CANCEL|          |
	 *  |             |             |<---------|          |
	 *  |             |             |487/INVITE|          |
	 *  |             |             |<---------|          |
	 *  |             |             |ACK       |          |
	 *  |             |             |--------->|          |
	 *  |             |  487/INVITE |          |          |
	 *  |             |<------------|          |          |
	 *  |             |ACK          |          |          |
	 *  |             |------------>|          |          |
	 * </pre>
	 * 
	 */
	@Test
	public void testCancel() throws Throwable
	{		
		Endpoint bob = createEndpoint("bob");
		UaRunnable replyB = new CancelReply(bob.getUserAgent());
		Endpoint carol = createEndpoint("bob", "carol");
		UaRunnable replyC = new CancelReply(carol.getUserAgent());
		replyB.start();
		replyC.start();
		Thread.sleep(10);
		
		SipServletRequest request = _ua.createRequest(SipMethods.INVITE,  bob.getUri());
		request.addAddressHeader(B2bHelperProxyServlet.PROXY_URIS, bob.getContact(), true);
		request.addAddressHeader(B2bHelperProxyServlet.PROXY_URIS, carol.getContact(), true);

		request.getApplicationSession().setInvalidateWhenReady(false);
		request.getSession().setInvalidateWhenReady(false);
		Call callA = _ua.createCall(request);
		
		SipServletResponse response = callA.waitForResponse();
		assertThat(response, hasStatus(SipServletResponse.SC_RINGING));
		SipSession session1 = response.getSession();
		
		response = callA.waitForResponse();
		assertThat(response, hasStatus(SipServletResponse.SC_RINGING));
		SipSession session2 = response.getSession();
		assertNotSame(session1.getId(), session2.getId());
	
		SipServletRequest cancel = callA.createCancel();
		cancel.send();
		
		response = callA.waitForResponse();

		assertThat(response, hasStatus(SipServletResponse.SC_REQUEST_TERMINATED));
		// 200/CANCEL are filtered by container
               
        replyB.assertDone();
        replyC.assertDone();
	}
	
	class CancelReply extends UaRunnable
	{		
		public CancelReply(TestAgent ua)
		{
			super(ua);
		}
		
		@Override
		public void doTest() throws Throwable
		{	        
	        SipServletRequest request = waitForInitialRequest();
			assertThat(request.getMethod(), is(SipMethods.INVITE));
			_ua.createResponse(request, SipServletResponse.SC_RINGING, "Ringing " + getUserName()).send();
			
			SipServletRequest cancel = _dialog.waitForRequest();
			assertThat(cancel.getMethod(), is(SipMethods.CANCEL));

			// 200/CANCEL and 487/INVITE is sent by container
		}	
	}
	
	class NoProvisionalReply extends UaRunnable
	{		
		public NoProvisionalReply(TestAgent ua)
		{
			super(ua);
		}
		
		@Override
		public void doTest() throws Throwable
		{
			SipServletRequest request = waitForInitialRequest();
			assertThat(request.getMethod(), is(SipMethods.INVITE));
			_ua.createResponse(request, SipServletResponse.SC_OK, "OK " + getUserName()).send();
			
			request = _dialog.waitForRequest();
			assertThat(request.getMethod(), is(SipMethods.ACK));
						
			request = _dialog.waitForRequest();
			assertThat(request.getMethod(), is(SipMethods.BYE)); // TODO ACK could be retransmit ???
			_ua.createResponse(request, SipServletResponse.SC_OK, "OK " + getUserName()).send();
		}	
	}
	
	class RingingReply extends UaRunnable
	{		
		public RingingReply(TestAgent ua)
		{
			super(ua);
		}
		
		@Override
		public void doTest() throws Throwable
		{
			SipServletRequest request = waitForInitialRequest();
			assertThat(request.getMethod(), is(SipMethods.INVITE));
			
			ApplicationSessionScope scope = openScope(request.getApplicationSession());
			try
			{
				_ua.createResponse(request, SipServletResponse.SC_RINGING, "Ringing " + getUserName()).send();
				Thread.sleep(1000);
				_ua.createResponse(request, SipServletResponse.SC_OK, "OK " + getUserName()).send();
			}
			finally
			{
				scope.close();
			}
			
			request = _dialog.waitForRequest();
			assertThat(request.getMethod(), is(SipMethods.ACK));
						
			request = _dialog.waitForRequest();
			assertThat(request.getMethod(), is(SipMethods.BYE)); // TODO ACK could be retransmit ???
			_ua.createResponse(request, SipServletResponse.SC_OK, "OK " + getUserName()).send();
		}	
	}
	
	class NegativeReply extends UaRunnable
	{	
		private long _ringingWait;
		private int _status;
		
		public NegativeReply(TestAgent ua, long ringingWait, int status)
		{
			super(ua);
			_status = status;
		}
		
		@Override
		public void doTest() throws Throwable
		{
			SipServletRequest request = waitForInitialRequest();
			assertThat(request.getMethod(), is(SipMethods.INVITE));
			
			Thread.sleep(_ringingWait);
			_ua.createResponse(request, SipServletResponse.SC_RINGING, "Ringing " + getUserName()).send();
			Thread.sleep(500);
			_ua.createResponse(request, _status, "Decline " + getUserName()).send();
		}	
	}
	
	class SessionProgressReply extends UaRunnable
	{
		
		public SessionProgressReply(TestAgent ua)
		{
			super(ua);
		}
		
		@Override
		public void doTest() throws Throwable
		{
			SipServletRequest request = waitForInitialRequest();
			assertThat(request.getMethod(), is(SipMethods.INVITE));
			
			SipServletResponse response = _ua.createResponse(
					request, SipServletResponse.SC_SESSION_PROGRESS, "Session progress " + getUserName());
			response.addHeader(SipHeaders.REQUIRE, "100rel");
			response.sendReliably();
			
			SipServletRequest prack = _dialog.waitForRequest();
			
			ApplicationSessionScope scope = openScope(prack.getApplicationSession());
			try
			{
				assertThat(prack.getMethod(), is(SipMethods.PRACK));
				
				_ua.createResponse(prack, SipServletResponse.SC_OK, "OK " + getUserName()).send();
				
				Thread.sleep(1000);
				_ua.createResponse(request, SipServletResponse.SC_OK, "OK " + getUserName()).send();
			}
			finally
			{
				scope.close();
			}
			
			request = _dialog.waitForRequest();
			assertThat(request.getMethod(), is(SipMethods.ACK));
			
			request = _dialog.waitForRequest();
			assertThat(request.getMethod(), is(SipMethods.BYE));
			_ua.createResponse(request, SipServletResponse.SC_OK, "OK " + getUserName()).send();
		}	
	}
}
