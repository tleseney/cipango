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
package org.cipango.kaleo.integration;

import static org.cipango.client.test.matcher.SipMatchers.hasHeader;
import static org.cipango.client.test.matcher.SipMatchers.hasMethod;
import static org.cipango.client.test.matcher.SipMatchers.hasStatus;
import static org.cipango.client.test.matcher.SipMatchers.isSuccess;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.io.ByteArrayInputStream;

import javax.servlet.sip.Parameterable;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.cipango.client.Dialog;
import org.cipango.client.SipHeaders;
import org.cipango.client.SipMethods;
import org.cipango.kaleo.location.event.ContactDocument.Contact;
import org.cipango.kaleo.location.event.ContactDocument.Contact.Event;
import org.cipango.kaleo.location.event.ReginfoDocument;
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
	public void testSubscription() throws Exception
	{
		Dialog subscription = getAlice().customize(new Dialog());
    	SipServletRequest subscribe = subscription.createInitialRequest(SipMethods.SUBSCRIBE, getAlice().getAor(), getAlice().getAor());
    	subscribe.setHeader(SipHeaders.EVENT, "reg");
    	subscribe.setExpires(100);
    	subscription.start(subscribe); // 1
		
    	SipServletResponse response = subscription.waitForResponse(); // 2
    	assertThat(response, isSuccess());
				
    	SipServletRequest notify = subscription.waitForRequest(); // 3
    	notify.createResponse(SipServletResponse.SC_OK).send(); //4
    	assertThat(notify, hasMethod(SipMethods.NOTIFY));
    	
    	Parameterable subscriptionState = notify.getParameterableHeader(SipHeaders.SUBSCRIPTION_STATE);
    	assertThat(subscriptionState, is(notNullValue()));
    	assertThat(subscriptionState.getValue(), is("active"));
		assertBetween(95, 100, Integer.parseInt(subscriptionState.getParameter("expires")));
		
		assertThat(notify.getHeader(SipHeaders.EVENT), is("reg"));
		
		Reginfo regInfo = getRegInfo(notify);
		int version = regInfo.getVersion().intValue();
		Registration registration = regInfo.getRegistrationArray(0);
		assertEquals(State.INIT, registration.getState());
		assertEquals(getAlice().getAor().getURI().toString(), registration.getAor());
		assertEquals(0, registration.getContactArray().length);
		
		getAlice().register(1800); // 5 and 6
		
		notify = subscription.waitForRequest(); // 7
		notify.createResponse(SipServletResponse.SC_OK).send(); //8
    	assertThat(notify, hasMethod(SipMethods.NOTIFY));
		regInfo = getRegInfo(notify);
		registration = regInfo.getRegistrationArray(0);
		assertEquals(1, registration.getContactArray().length);
		assertEquals(version + 1, regInfo.getVersion().intValue());
		assertEquals(State.ACTIVE, registration.getState());
		Contact contact = registration.getContactArray(0);
		assertBetween(1795, 1800, contact.getExpires().intValue());
		assertEquals(Event.REGISTERED, contact.getEvent());
		
		getAlice().unregister(); // 9 and  10
		
		notify = subscription.waitForRequest(); // 11
		notify.createResponse(SipServletResponse.SC_OK).send(); //12
    	assertThat(notify, hasMethod(SipMethods.NOTIFY));
		regInfo = getRegInfo(notify);
		registration = regInfo.getRegistrationArray(0);
		assertEquals(1, registration.getContactArray().length);
		assertEquals(version + 2, regInfo.getVersion().intValue());
		assertEquals(State.TERMINATED, registration.getState());
		contact = registration.getContactArray(0);
		assertEquals(0, contact.getExpires().intValue());
		assertEquals(Event.UNREGISTERED, contact.getEvent());
		
		subscribe = subscription.createRequest(SipMethods.SUBSCRIBE); // 13
    	subscribe.setHeader(SipHeaders.EVENT, "reg");
    	subscribe.setExpires(0);
    	subscribe.send();
    	assertThat(subscription.waitForResponse(), isSuccess()); //14
		
    	notify = subscription.waitForRequest(); //15
    	notify.createResponse(SipServletResponse.SC_OK).send(); //16
    	assertThat(notify, hasMethod(SipMethods.NOTIFY));
    	//System.out.println(notify);
    	
    	subscriptionState = notify.getParameterableHeader(SipHeaders.SUBSCRIPTION_STATE);
    	assertThat(notify.getHeader(SipHeaders.SUBSCRIPTION_STATE), is("terminated"));
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
	public void testSubscription2() throws Exception
	{
		getAlice().register(1800); // 1 and 2
		
		Dialog subscription = getAlice().customize(new Dialog());
    	SipServletRequest subscribe = subscription.createInitialRequest(SipMethods.SUBSCRIBE, getAlice().getAor(), getAlice().getAor());
    	subscribe.setHeader(SipHeaders.EVENT, "reg");
    	subscribe.setExpires(100);
    	subscription.start(subscribe); // 3
		
    	SipServletResponse response = subscription.waitForResponse(); // 4
    	assertThat(response, isSuccess());
				
    	SipServletRequest notify = subscription.waitForRequest(); // 5
    	notify.createResponse(SipServletResponse.SC_OK).send(); //6
    	assertThat(notify, hasMethod(SipMethods.NOTIFY));
    	
    	Parameterable subscriptionState = notify.getParameterableHeader(SipHeaders.SUBSCRIPTION_STATE);
    	assertThat(subscriptionState, is(notNullValue()));
    	assertThat(subscriptionState.getValue(), is("active"));
		assertBetween(95, 100, Integer.parseInt(subscriptionState.getParameter("expires")));
		
		assertThat(notify.getHeader(SipHeaders.EVENT), is("reg"));
		
		Reginfo regInfo = getRegInfo(notify);
		Registration registration = regInfo.getRegistrationArray(0);
		assertEquals(0, regInfo.getVersion().intValue());
		assertEquals(State.ACTIVE, registration.getState());
		assertEquals(getAlice().getAor().getURI().toString(), registration.getAor());
		assertEquals(1, registration.getContactArray().length);
		
		getAlice().unregister(); // 9 and  10
		
		notify = subscription.waitForRequest(); // 11
		notify.createResponse(SipServletResponse.SC_OK).send(); //12
    	assertThat(notify, hasMethod(SipMethods.NOTIFY));
		regInfo = getRegInfo(notify);
		registration = regInfo.getRegistrationArray(0);
		assertEquals(1, registration.getContactArray().length);
		assertEquals(1, regInfo.getVersion().intValue());
		assertEquals(State.TERMINATED, registration.getState());
		Contact contact = registration.getContactArray(0);
		assertEquals(0, contact.getExpires().intValue());
		assertEquals(Event.UNREGISTERED, contact.getEvent());
				
		subscribe = subscription.createRequest(SipMethods.SUBSCRIBE); // 13
    	subscribe.setHeader(SipHeaders.EVENT, "reg");
    	subscribe.setExpires(0);
    	subscribe.send();
    	assertThat(subscription.waitForResponse(), isSuccess()); //14
		
    	notify = subscription.waitForRequest(); //15
    	notify.createResponse(SipServletResponse.SC_OK).send(); //16
    	assertThat(notify, hasMethod(SipMethods.NOTIFY));
    	//System.out.println(notify);
    	
    	subscriptionState = notify.getParameterableHeader(SipHeaders.SUBSCRIPTION_STATE);
    	assertThat(notify.getHeader(SipHeaders.SUBSCRIPTION_STATE), is("terminated"));
		regInfo = getRegInfo(notify);
		registration = regInfo.getRegistrationArray(0);
		assertEquals(State.TERMINATED, registration.getState());
		assertEquals(0, registration.getContactArray().length);
		assertEquals(2, regInfo.getVersion().intValue());
	}
	
	private Reginfo getRegInfo(SipServletRequest request)
	{
		String contentType = request.getHeader(SipHeaders.CONTENT_TYPE);
		assertThat(contentType, is("application/reginfo+xml"));
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
    	SipServletRequest request = getAlice().createRequest(SipMethods.SUBSCRIBE, getAlice().getAor());
    	request.setHeader(SipHeaders.EVENT, "reg");
    	request.setExpires(1);
    	SipServletResponse response = getAlice().sendSynchronous(request);
    	assertThat(response, hasStatus(SipServletResponse.SC_INTERVAL_TOO_BRIEF));
    	assertThat(response, hasHeader(SipHeaders.MIN_EXPIRES));
    }
}
