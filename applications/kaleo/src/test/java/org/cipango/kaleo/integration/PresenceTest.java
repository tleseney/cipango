/*
 * Created on November 22, 2005
 * 
 * Copyright 2005 CafeSip.org 
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 *
 *	http://www.apache.org/licenses/LICENSE-2.0 
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 *
 */
package org.cipango.kaleo.integration;

import static org.cipango.client.test.matcher.SipMatchers.hasHeader;
import static org.cipango.client.test.matcher.SipMatchers.hasMethod;
import static org.cipango.client.test.matcher.SipMatchers.hasStatus;
import static org.cipango.client.test.matcher.SipMatchers.isSuccess;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.sip.Parameterable;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.cipango.client.Dialog;
import org.cipango.client.Publisher;
import org.cipango.client.SipHeaders;
import org.cipango.client.SipMethods;

public class PresenceTest extends UaTestCase
{

	public void setUp() throws Exception
	{
		super.setUp();
		setContent("/org.openmobilealliance.pres-rules/users/sip:alice@" + getDomain() + "/pres-rules");
		setContent("/org.openmobilealliance.pres-rules/users/sip:bob@" + getDomain() + "/pres-rules");
	}
	
	/**
	 * <pre>
	 *  Alice               Kaleo                  Bob
          |(1) SUBSCRIBE      |                     |
          |Event:presence     |                     |
          |------------------>|                     |
          |(2) 200 OK         |                     |
          |<------------------|                     |
          |(3) NOTIFY         |                     |
          |<------------------|                     |
          |(4) 200 OK         |                     |
          |------------------>|                     |
          |                   |(5) PUBLISH          |
          |                   |<--------------------|
          |                   |(6) 200 OK           |
          |                   |-------------------->|
          |(7) NOTIFY         |                     |
          |<------------------|                     |
          |(8) 200 OK         |                     |
          |------------------>|                     |
          |(9) SUBSCRIBE      |                     |
          |Expires: 0         |                     |
          |------------------>|                     |
          |(10) 200 OK        |                     |
          |<------------------|                     |
          |(11) NOTIFY        |                     |
          |<------------------|                     |
          |(12) 200 OK        |                     |
          |------------------>|                     |
          |                   |(13) PUBLISH         |
          |                   |Expires = 0          |
          |                   |<--------------------|
          |                   |(14) 200 OK          |
          |                   |-------------------->|
     * </pre>
	 */
    public void testBasicSubscription() throws Exception
    {
    	Dialog subscription = getAlice().customize(new Dialog());
    	SipServletRequest subscribe = subscription.createInitialRequest(SipMethods.SUBSCRIBE, getAlice().getAor(), getBob().getAor());
    	subscribe.setHeader(SipHeaders.EVENT, "presence");
    	subscribe.setExpires(60);
    	subscription.start(subscribe);
    	
    	SipServletResponse response = subscription.waitForResponse();
    	assertThat(response, isSuccess());
    	
    	SipServletRequest notify = subscription.waitForRequest();
    	notify.createResponse(SipServletResponse.SC_OK).send();
    	assertThat(notify, hasMethod(SipMethods.NOTIFY));
    	//System.out.println(notify);
    	String content = new String(notify.getRawContent());
    	assertThat(content, containsString("<status><basic>closed</basic></status>"));
    	Parameterable subscriptionState = notify.getParameterableHeader(SipHeaders.SUBSCRIPTION_STATE);
    	assertThat(subscriptionState, is(notNullValue()));
    	assertThat(subscriptionState.getValue(), is("active"));
    	assertThat(Integer.parseInt(subscriptionState.getParameter("expires")), 
    			is(both(lessThanOrEqualTo(60)).and(greaterThan(55))));
    	
        Thread.sleep(50);
        
        Publisher publisher = new Publisher(getBob().getAor());
    	getBob().customize(publisher);
    	SipServletRequest request = publisher.newPublish(getClass().getResourceAsStream("publish1.xml"), 20);
    	publisher.start(request);
    	assertThat(publisher.waitForResponse(), isSuccess());

    	notify = subscription.waitForRequest();
    	notify.createResponse(SipServletResponse.SC_OK).send();
    	assertThat(notify, hasMethod(SipMethods.NOTIFY));
    	//System.out.println(notify);
    	
    	subscriptionState = notify.getParameterableHeader(SipHeaders.SUBSCRIPTION_STATE);
    	assertThat(Integer.parseInt(subscriptionState.getParameter("expires")), is(greaterThan(0)));
    	assertThat(new String(notify.getRawContent()), containsString("<basic>open</basic>"));

    	subscribe = subscription.createRequest(SipMethods.SUBSCRIBE);
    	subscribe.setHeader(SipHeaders.EVENT, "presence");
    	subscribe.setExpires(0);
    	subscribe.send();
    	assertThat(subscription.waitForResponse(), isSuccess());
    	
    	notify = subscription.waitForRequest();
    	notify.createResponse(SipServletResponse.SC_OK).send();
    	assertThat(notify, hasMethod(SipMethods.NOTIFY));
    	//System.out.println(notify);
    	
    	subscriptionState = notify.getParameterableHeader(SipHeaders.SUBSCRIPTION_STATE);
    	assertThat(notify.getHeader(SipHeaders.SUBSCRIPTION_STATE), is("terminated"));

    	publisher.newUnPublish().send();
    	assertThat(publisher.waitForResponse(), isSuccess());
    }

    public void testMinExpires() throws Exception
    {       
    	SipServletRequest request = getAlice().createRequest(SipMethods.SUBSCRIBE, getBob().getAor());
    	request.setHeader(SipHeaders.EVENT, "presence");
    	request.setExpires(1);
    	SipServletResponse response = getAlice().sendSynchronous(request);
    	assertThat(response, hasStatus(SipServletResponse.SC_INTERVAL_TOO_BRIEF));
    	assertThat(response, hasHeader(SipHeaders.MIN_EXPIRES));
    }
    
	public void testBadEvent() throws Exception
    {       
    	SipServletRequest request = getAlice().createRequest(SipMethods.SUBSCRIBE, getBob().getAor());
    	request.setHeader(SipHeaders.EVENT, "unknown");
    	request.setExpires(1500);
    	SipServletResponse response = getAlice().sendSynchronous(request);
    	assertThat(response, hasStatus(SipServletResponse.SC_BAD_EVENT));
    	
        Iterator<String> it = response.getHeaders(SipHeaders.ALLOW_EVENTS);
        List<String> l = new ArrayList<String>();
        while (it.hasNext())
			l.add(it.next());
		
        assertThat(l, containsInAnyOrder("presence", "presence.winfo", "reg"));
    }
    
    public void testBadAcceptHeader() throws Exception
    {       
    	SipServletRequest request = getAlice().createRequest(SipMethods.SUBSCRIBE, getBob().getAor());
    	request.setHeader(SipHeaders.EVENT, "presence");
    	request.setExpires(1500);
        request.setHeader(SipHeaders.ACCEPT, "application/unknown");
        SipServletResponse response = getAlice().sendSynchronous(request);
    	assertThat(response, hasStatus(SipServletResponse.SC_UNSUPPORTED_MEDIA_TYPE));
    }
        
    public void testEtags() throws Exception
    {       
    	Publisher publisher = new Publisher(getBob().getAor());
    	getBob().customize(publisher);
    	
    	SipServletRequest request = publisher.newPublish(getClass().getResourceAsStream("publish1.xml"), 20);
    	publisher.start(request);
    	SipServletResponse response = publisher.waitForResponse();
    	assertThat(response, isSuccess());
    	assertThat(publisher.getEtag(), is(notNullValue()));
        String etag = publisher.getEtag(); 
    	
    	request = publisher.newPublish(getClass().getResourceAsStream("publish2.xml"), 20);         
    	request.send();
    	response = publisher.waitForResponse();
    	assertThat(response, isSuccess());
    	assertThat(publisher.getEtag(), is(notNullValue()));
    	
    	request = publisher.newPublish(getClass().getResourceAsStream("publish2.xml"), 20);
    	request.setHeader(SipHeaders.SIP_IF_MATCH, etag); //Use old etag
    	request.send();
    	response = publisher.waitForResponse();
    	assertThat(response, hasStatus(SipServletResponse.SC_CONDITIONAL_REQUEST_FAILED));
    	assertThat(publisher.getEtag(), is(notNullValue()));
                 
    	request = publisher.newUnPublish();         
    	request.send();
    	response = publisher.waitForResponse();
    	assertThat(response, isSuccess());
    }

}