// ========================================================================
// Copyright 2009 NEXCOM Systems
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

import java.io.ByteArrayInputStream;

import javax.sip.ServerTransaction;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.EventHeader;
import javax.sip.header.MinExpiresHeader;
import javax.sip.header.SubscriptionStateHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.cafesip.sipunit.RegisterSession;
import org.cafesip.sipunit.SipResponse;
import org.cafesip.sipunit.SubscribeSession;
import org.cipango.kaleo.location.event.ReginfoDocument;
import org.cipango.kaleo.location.event.ContactDocument.Contact;
import org.cipango.kaleo.location.event.ContactDocument.Contact.Event;
import org.cipango.kaleo.location.event.ReginfoDocument.Reginfo;
import org.cipango.kaleo.location.event.RegistrationDocument.Registration;
import org.cipango.kaleo.location.event.RegistrationDocument.Registration.State;

public class RegEventTest extends UaTestCase
{
	/**
	 * <pre>
	 *  Alice               Kaleo              SipUnit
          |                   |(1) SUBSCRIBE      |
          |                   |Event:reg          |
          |                   |<------------------|
          |                   |(2) 200 OK         |
          |                   |------------------>|
          |                   |(3) NOTIFY         |
          |                   |------------------>|
          |                   |(4) 200 OK         |
          |                   |<------------------|
          |(5) REGISTER       |                   |
          |Expires: 1800      |                   |
          |------------------>|                   |
          |(6) 200 OK         |                   |
          |<------------------|                   |
          |                   |(7) NOTIFY         |
          |                   |------------------>|
          |                   |(8) 200 OK         |
          |                   |<------------------|
          |(9) REGISTER       |                   |
          |Expires: 0         |                   |
          |------------------>|                   |
          |(10) 200 OK        |                   |
          |<------------------|                   |
          |                   |(11) NOTIFY        |
          |                   |------------------>|
          |                   |(12) 200 OK        |
          |                   |<------------------|
          |                   |(13) SUBSCRIBE     |
          |                   |Expires: 0         |
          |                   |<------------------|
          |                   |(14) 200 OK        |
          |                   |------------------>|
          |                   |(15) NOTIFY        |
          |                   |------------------>|
          |                   |(16) 200 OK        |
          |                   |<------------------|
     * </pre>
	 */
	public void testSubscription()
	{
		// Ensure Alice is not registered
		RegisterSession registerSession = new RegisterSession(getAlicePhone());
		registerSession.register(null, 0);
		
		SubscribeSession session = new SubscribeSession(getAlicePhone(), "reg");
		Request subscribe = session.newInitialSubscribe(100, getAliceUri()); // 1
		session.sendRequest(subscribe, Response.OK); // 2
		
		ServerTransaction tx = session.waitForNotify();
		Request notify = tx.getRequest(); // 3
		//System.out.println(notify);
		session.sendResponse(Response.OK, tx); // 4
		SubscriptionStateHeader subState = (SubscriptionStateHeader) notify.getHeader(SubscriptionStateHeader.NAME);
		assertEquals(SubscriptionStateHeader.ACTIVE.toLowerCase(), subState.getState().toLowerCase());
		assertBetween(95, 100, subState.getExpires());
		assertEquals("reg", ((EventHeader) notify.getHeader(EventHeader.NAME)).getEventType());
		Reginfo regInfo = getRegInfo(notify);
		int version = regInfo.getVersion().intValue();
		Registration registration = regInfo.getRegistrationArray(0);
		assertEquals(State.INIT, registration.getState());
		assertEquals(getAliceUri(), registration.getAor());
		assertEquals(0, registration.getContactArray().length);
		
		registerSession.register(null, 1800); // 5 and 6
		
		tx = session.waitForNotify(); 
		notify = tx.getRequest(); // 7
		//System.out.println(notify);
		session.sendResponse(Response.OK, tx); // 8
		regInfo = getRegInfo(notify);
		registration = regInfo.getRegistrationArray(0);
		assertEquals(1, registration.getContactArray().length);
		assertEquals(version + 1, regInfo.getVersion().intValue());
		assertEquals(State.ACTIVE, registration.getState());
		Contact contact = registration.getContactArray(0);
		assertBetween(1795, 1800, contact.getExpires().intValue());
		assertEquals(Event.REGISTERED, contact.getEvent());
		
		registerSession.register(null, 0); // 9 and  10
		tx = session.waitForNotify(); 
		notify = tx.getRequest(); // 11
		//System.out.println(notify);
		session.sendResponse(Response.OK, tx); // 12
		regInfo = getRegInfo(notify);
		registration = regInfo.getRegistrationArray(0);
		assertEquals(1, registration.getContactArray().length);
		assertEquals(version + 2, regInfo.getVersion().intValue());
		assertEquals(State.TERMINATED, registration.getState());
		contact = registration.getContactArray(0);
		assertEquals(0, contact.getExpires().intValue());
		assertEquals(Event.UNREGISTERED, contact.getEvent());
		
		subscribe = session.newSubsequentSubscribe(0); // 13
		session.sendRequest(subscribe, Response.OK); // 14
		
		tx = session.waitForNotify();
		notify = tx.getRequest(); // 15
		//System.out.println(notify);
		session.sendResponse(Response.OK, tx); // 16
		subState = (SubscriptionStateHeader) notify.getHeader(SubscriptionStateHeader.NAME);
		assertEquals(SubscriptionStateHeader.TERMINATED.toLowerCase(), 
				subState.getState());
		regInfo = getRegInfo(notify);
		registration = regInfo.getRegistrationArray(0);
		assertEquals(State.TERMINATED, registration.getState());
		assertEquals(0, registration.getContactArray().length);
		assertEquals(version + 3, regInfo.getVersion().intValue());
	}
	
	/**
	 * <pre>
	 *  Alice               Kaleo              SipUnit

          |(1) REGISTER       |                   |
          |Expires: 1800      |                   |
          |------------------>|                   |
          |(2) 200 OK         |                   |
          |<------------------|                   |
          |                   |(3) SUBSCRIBE      |
          |                   |Event:reg          |
          |                   |<------------------|
          |                   |(4) 200 OK         |
          |                   |------------------>|
          |                   |(5) NOTIFY         |
          |                   |------------------>|
          |                   |(6) 200 OK         |
          |                   |<------------------|
          |(9) REGISTER       |                   |
          |Expires: 0         |                   |
          |------------------>|                   |
          |(10) 200 OK        |                   |
          |<------------------|                   |
          |                   |(11) NOTIFY        |
          |                   |------------------>|
          |                   |(12) 200 OK        |
          |                   |<------------------|
          |                   |(13) SUBSCRIBE     |
          |                   |Expires: 0         |
          |                   |<------------------|
          |                   |(14) 200 OK        |
          |                   |------------------>|
          |                   |(15) NOTIFY        |
          |                   |------------------>|
          |                   |(16) 200 OK        |
          |                   |<------------------|
     * </pre>
	 */
	public void testSubscription2()
	{
		getAlicePhone().register(null, 1500);
		assertLastOperationSuccess(getAlicePhone());
		
		SubscribeSession session = new SubscribeSession(getAlicePhone(), "reg");
		Request subscribe = session.newInitialSubscribe(100, getAliceUri());
		session.sendRequest(subscribe, Response.OK);
		
		ServerTransaction tx = session.waitForNotify();
		Request notify = tx.getRequest();
		//System.out.println(notify);
		session.sendResponse(Response.OK, tx);
		SubscriptionStateHeader subState = (SubscriptionStateHeader) notify.getHeader(SubscriptionStateHeader.NAME);
		assertEquals(SubscriptionStateHeader.ACTIVE.toLowerCase(), subState.getState().toLowerCase());
		assertBetween(95, 100, subState.getExpires());
		assertEquals("reg", ((EventHeader) notify.getHeader(EventHeader.NAME)).getEventType());
		Reginfo regInfo = getRegInfo(notify);
		Registration registration = regInfo.getRegistrationArray(0);
		assertEquals(0, regInfo.getVersion().intValue());
		assertEquals(State.ACTIVE, registration.getState());
		assertEquals(getAliceUri(), registration.getAor());
		assertEquals(1, registration.getContactArray().length);
		
		
		getAlicePhone().unregister(null, 2000);
		assertLastOperationSuccess(getAlicePhone());
		tx = session.waitForNotify();
		notify = tx.getRequest();
		session.sendResponse(Response.OK, tx);
		regInfo = getRegInfo(notify);
		registration = regInfo.getRegistrationArray(0);
		assertEquals(1, registration.getContactArray().length);
		assertEquals(1, regInfo.getVersion().intValue());
		assertEquals(State.TERMINATED, registration.getState());
		Contact contact = registration.getContactArray(0);
		assertEquals(0, contact.getExpires().intValue());
		assertEquals(Event.UNREGISTERED, contact.getEvent());
				
		subscribe = session.newSubsequentSubscribe(0);
		session.sendRequest(subscribe, Response.OK);
		
		tx = session.waitForNotify();
		notify = tx.getRequest();
		//System.out.println(notify);
		session.sendResponse(Response.OK, tx);
		subState = (SubscriptionStateHeader) notify.getHeader(SubscriptionStateHeader.NAME);
		assertEquals(SubscriptionStateHeader.TERMINATED.toLowerCase(), 
				subState.getState());
		regInfo = getRegInfo(notify);
		registration = regInfo.getRegistrationArray(0);
		assertEquals(State.TERMINATED, registration.getState());
		assertEquals(0, registration.getContactArray().length);
		assertEquals(2, regInfo.getVersion().intValue());
	}
	
	private Reginfo getRegInfo(Request request)
	{
		ContentTypeHeader contentType = (ContentTypeHeader) request.getHeader(ContentTypeHeader.NAME);
		assertEquals("application", contentType.getContentType());
		assertEquals("reginfo+xml", contentType.getContentSubType());
		try
		{
			return ReginfoDocument.Factory.parse(new ByteArrayInputStream(request.getRawContent())).getReginfo();
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
    public void testMinExpires() throws Exception
    {       
        SubscribeSession session = new SubscribeSession(getAlicePhone(), "reg");
        Request request = session.newInitialSubscribe(1, getAliceUri());
        Response response = session.sendRequest(request,SipResponse.INTERVAL_TOO_BRIEF);
        MinExpiresHeader minExpiresHeader = (MinExpiresHeader) response.getHeader(MinExpiresHeader.NAME);
        assertNotNull(minExpiresHeader);
    }
}
