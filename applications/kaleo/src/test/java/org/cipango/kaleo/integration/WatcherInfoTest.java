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
import static org.cipango.client.test.matcher.SipMatchers.hasStatus;
import static org.cipango.client.test.matcher.SipMatchers.isSuccess;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.cipango.client.Dialog;
import org.cipango.client.Publisher;
import org.cipango.client.SipHeaders;
import org.cipango.client.SipMethods;
import org.cipango.client.Subscriber;
import org.cipango.kaleo.presence.PresenceEventPackage;
import org.cipango.kaleo.presence.pidf.Basic;
import org.cipango.kaleo.presence.pidf.Presence;
import org.cipango.kaleo.presence.pidf.PresenceDocument;
import org.cipango.kaleo.presence.watcherinfo.WatcherDocument.Watcher;
import org.cipango.kaleo.presence.watcherinfo.WatcherDocument.Watcher.Event;
import org.cipango.kaleo.presence.watcherinfo.WatcherDocument.Watcher.Status;
import org.cipango.kaleo.presence.watcherinfo.WatcherListDocument.WatcherList;
import org.cipango.kaleo.presence.watcherinfo.WatcherinfoDocument;
import org.cipango.kaleo.presence.watcherinfo.WatcherinfoDocument.Watcherinfo;

public class WatcherInfoTest extends UaTestCase
{
	
	private static final String ALICE_PRES_RULES_URI = 
		"/org.openmobilealliance.pres-rules/users/sip:alice@cipango.org/pres-rules/~~/cr:ruleset/cr:rule%5b@id=%22wp_prs_allow_own%22%5d/cr:conditions/cr:identity";
	private static final String BOB_PRES_RULES_URI = 
		"/org.openmobilealliance.pres-rules/users/sip:bob@cipango.org/pres-rules/~~/cr:ruleset/cr:rule%5b@id=%22wp_prs_allow_own%22%5d/cr:actions";
	
	public void setUp() throws Exception
	{
		super.setUp();
		setContent("/org.openmobilealliance.pres-rules/users/sip:alice@" + getDomain() + "/pres-rules");
		setContent("/org.openmobilealliance.pres-rules/users/sip:bob@" + getDomain() + "/pres-rules");
		setContent("/resource-lists/users/sip:alice@" + getDomain() + "/index");
	}
	
	/**
	 * <pre>
	    Alice               Kaleo              SipUnit
          |                   |(1) SUBSCRIBE        |
          |                   |Event:presence.winfo |
          |                   |<--------------------|
          |                   |(2) 200 OK           |
          |                   |-------------------->|
          |                   |(3) NOTIFY           |
          |                   |-------------------->|
          |                   |(4) 200 OK           |
          |                   |<--------------------|
          |(5) SUBSCRIBE      |                     |
          |Event:presence     |                     |
          |------------------>|                     |
          |(6) 200 OK         |                     |
          |<------------------|                     |
          |                   |(7) NOTIFY           |
          |                   |-------------------->|
          |                   |(8) 200 OK           |
          |                   |<--------------------|
          |(9) NOTIFY         |                     |
          |<------------------|                     |
          |(10) 200 OK        |                     |
          |------------------>|                     |
          |(11) SUBSCRIBE     |                     |
          |Expires: 0         |                     |
          |------------------>|                     |
          |(12) 200 OK        |                     |
          |<------------------|                     |
          |                   |(13) NOTIFY          |
          |                   |-------------------->|
          |                   |(14) 200 OK          |
          |                   |<--------------------|
          |(15) NOTIFY        |                     |
          |<------------------|                     |
          |(16) 200 OK        |                     |
          |------------------>|                     |
          |                   |(17) SUBSCRIBE       |
          |                   |Expires: 0           |
          |                   |<--------------------|
          |                   |(18) 200 OK          |
          |                   |-------------------->|
          |                   |(19) NOTIFY          |
          |                   |-------------------->|
          |                   |(20) 200 OK          |
          |                   |<--------------------|
     * </pre>
	 */
	public void testSubscription() throws Exception
	{		
		Subscriber winfo = new Subscriber("presence.winfo", getAlice().customize(new Dialog()));
		SipServletResponse response = winfo.startSubscription(getBob().getAor(), getBob().getAor(), 100); //1
    	assertThat(response, isSuccess()); // 2
    	
    	SipServletRequest notify = winfo.waitForNotify(); //3
    	notify.createResponse(SipServletResponse.SC_OK).send(); // 4
		
    	assertThat(notify, hasHeader(SipHeaders.SUBSCRIPTION_STATE));
    	assertThat(winfo.getSubscriptionState(), is("active"));
    	assertThat(winfo.getExpires(), is(both(lessThanOrEqualTo(100)).and(greaterThan(95))));	
    	assertThat(notify.getHeader(SipHeaders.EVENT), is("presence.winfo"));

		Watcherinfo watcherinfo = getWatcherinfo(notify);
		assertEquals(0, watcherinfo.getVersion().intValue());
		assertEquals(Watcherinfo.State.FULL, watcherinfo.getState());
		assertEquals(1, watcherinfo.getWatcherListArray().length);
		WatcherList watcherList = watcherinfo.getWatcherListArray(0);
		assertEquals(getBob().getAor().getURI().toString(), watcherList.getResource());
		assertEquals(PresenceEventPackage.NAME, watcherList.getPackage());
		assertEquals(0, watcherList.getWatcherArray().length);
			
		Subscriber presence = new Subscriber("presence", getAlice().customize(new Dialog()));
		response = presence.startSubscription(getAlice().getAor(), getBob().getAor(), 60); //5
    	assertThat(response, isSuccess());  //6
    	
    	notify = winfo.waitForNotify(); //7
    	notify.createResponse(SipServletResponse.SC_OK).send(); // 8
		watcherinfo = getWatcherinfo(notify);
		assertEquals(1, watcherinfo.getVersion().intValue());
		watcherList = watcherinfo.getWatcherListArray(0);
		assertEquals(1, watcherList.sizeOfWatcherArray());
		Watcher watcher = watcherList.getWatcherArray(0);
		assertEquals(Event.SUBSCRIBE, watcher.getEvent());
		assertEquals(getAlice().getAor().getURI().toString(), watcher.getStringValue());
		assertEquals(Status.ACTIVE, watcher.getStatus());
    	
    	notify = presence.waitForNotify(); // 9
    	notify.createResponse(SipServletResponse.SC_OK).send(); // 10
    	
    	response = presence.stopSubscription(); //11
    	assertThat(response, isSuccess()); //12
		
    	notify = winfo.waitForNotify(); //13
    	notify.createResponse(SipServletResponse.SC_OK).send(); // 14
		watcherinfo = getWatcherinfo(notify);
		assertEquals(2, watcherinfo.getVersion().intValue());
		watcherList = watcherinfo.getWatcherListArray(0);
		assertEquals(1, watcherList.sizeOfWatcherArray());
		watcher = watcherList.getWatcherArray(0);
		assertEquals(Event.TIMEOUT, watcher.getEvent());
		assertEquals(getAlice().getAor().getURI().toString(), watcher.getStringValue());
		assertEquals(Status.TERMINATED, watcher.getStatus());
		
		
		notify = presence.waitForNotify(); // 15
    	notify.createResponse(SipServletResponse.SC_OK).send(); // 16

    	response = winfo.stopSubscription(); //17
    	assertThat(response, isSuccess()); //18
    	
    	notify = winfo.waitForNotify(); // 19
    	notify.createResponse(SipServletResponse.SC_OK).send(); // 20

		watcherinfo = getWatcherinfo(notify);
		assertEquals(3, watcherinfo.getVersion().intValue());
		watcherList = watcherinfo.getWatcherListArray(0);
		assertEquals(0, watcherList.sizeOfWatcherArray());
	}
	
	/**
	 * <pre>
	    Alice               Kaleo              SipUnit
          |(1) SUBSCRIBE      |                     |
          |Event:presence     |                     |
          |------------------>|                     |
          |(2) 200 OK         |                     |
          |<------------------|                     |
          |(3) NOTIFY         |                     |
          |<------------------|                     |
          |(4) 200 OK         |                     |
          |------------------>|                     |
          |                   |(5) SUBSCRIBE        |
          |                   |Event:presence.winfo |
          |                   |<--------------------|
          |                   |(6) 200 OK           |
          |                   |-------------------->|
          |                   |(7) NOTIFY           |
          |                   |-------------------->|
          |                   |(8) 200 OK           |
          |                   |<--------------------|
          |(9) SUBSCRIBE      |                     |
          |Expires: 0         |                     |
          |------------------>|                     |
          |(10) 200 OK        |                     |
          |<------------------|                     |
          |(11) NOTIFY        |                     |
          |<------------------|                     |
          |(12) 200 OK        |                     |
          |------------------>|                     |
     * </pre>
	 */
	public void testSubscription2() throws Exception
	{		
		Subscriber presence = new Subscriber("presence", getAlice().customize(new Dialog()));
		SipServletResponse response = presence.startSubscription(getAlice().getAor(), getBob().getAor(), 60); //1
    	assertThat(response, isSuccess());  //2
    	
    	SipServletRequest notify = presence.waitForNotify(); // 3
    	notify.createResponse(SipServletResponse.SC_OK).send(); // 4
		
		Subscriber winfo = new Subscriber("presence.winfo", getAlice().customize(new Dialog()));
		response = winfo.startSubscription(getBob().getAor(), getBob().getAor(), 0); //5
    	assertThat(response, isSuccess()); // 6
    	
    	notify = winfo.waitForNotify(); //7
    	notify.createResponse(SipServletResponse.SC_OK).send(); // 8
		
    	assertThat(notify, hasHeader(SipHeaders.SUBSCRIPTION_STATE));
    	assertThat(winfo.getSubscriptionState(), is("terminated"));
    	assertThat(winfo.getExpires(), is(0));	
    	assertThat(notify.getHeader(SipHeaders.EVENT), is("presence.winfo"));

		Watcherinfo watcherinfo = getWatcherinfo(notify);
		watcherinfo = getWatcherinfo(notify);
		assertEquals(0, watcherinfo.getVersion().intValue());
		WatcherList watcherList = watcherinfo.getWatcherListArray(0);
		assertEquals(1, watcherList.sizeOfWatcherArray());
		Watcher watcher = watcherList.getWatcherArray(0);
		assertEquals(Event.SUBSCRIBE, watcher.getEvent());
		assertEquals(getAlice().getAor().getURI().toString(), watcher.getStringValue());
		assertEquals(Status.ACTIVE, watcher.getStatus());
    	
    	response = presence.stopSubscription(); //9
    	assertThat(response, isSuccess()); //10
			
		notify = presence.waitForNotify(); // 11
    	notify.createResponse(SipServletResponse.SC_OK).send(); // 12		
	}
	
	/**
	 * <pre>
	    Alice               Kaleo              SipUnit
          |                   |(1) PUBLISH          |
          |                   |<--------------------|
          |                   |(2) 200 OK           |
          |                   |-------------------->|
          |(3) SUBSCRIBE      |                     |
          |Event:presence     |                     |
          |------------------>|                     |
          |(4) 200 OK         |                     |
          |<------------------|                     |
          |(5) NOTIFY         |                     |
          |<------------------|                     |
          |(6) 200 OK         |                     |
          |------------------>|                     |
          |                   |(7) SUBSCRIBE        |
          |                   |Event:presence.winfo |
          |                   |<--------------------|
          |                   |(8) 200 OK           |
          |                   |-------------------->|
          |                   |(9) NOTIFY           |
          |                   |-------------------->|
          |                   |(10) 200 OK          |
          |                   |<--------------------|
          |                   |(11) HTTP PUT        | Change subscription state from 
          |                   |<--------------------| allow to polite-block
          |                   |(12) 200 OK          |
          |                   |-------------------->|
          |(13) NOTIFY        |                     | Send NOTIFY with neutral state
          |<------------------|                     |
          |(14) 200 OK        |                     |
          |------------------>|                     |
          |                   |(15) NOTIFY          |
          |                   |-------------------->| 
          |                   |(16) 200 OK          |
          |                   |<--------------------|
          |                   |(17) SUBSCRIBE       |
          |                   |Expires = 0          |
          |                   |<--------------------|
          |                   |(18) 200 OK          |
          |                   |-------------------->|
          |                   |(19) NOTIFY          |
          |                   |-------------------->|
          |                   |(20) 200 OK          |
          |                   |<--------------------|
          |(21) SUBSCRIBE     |                     |
          |Expires: 0         |                     |
          |------------------>|                     |
          |(22) 200 OK        |                     |
          |<------------------|                     |
          |(23) NOTIFY        |                     |
          |<------------------|                     |
          |(24) 200 OK        |                     |
          |------------------>|                     |
          |                   |(25) PUBLISH         |
          |                   |Expires = 0          |
          |                   |<--------------------|
          |                   |(26) 200 OK          |
          |                   |-------------------->|
     * </pre>
	 */
	public void testSubscription3() throws Exception
	{
		Publisher publisher = new Publisher(getBob().getAor());
		getBob().customize(publisher);
		SipServletRequest request = publisher.newPublish(getClass().getResourceAsStream("publish1.xml"), 60);
		publisher.start(request); // 1
		assertThat(publisher.waitForResponse(), isSuccess()); //2
		
		Subscriber presence = new Subscriber("presence", getAlice().customize(new Dialog()));
		SipServletResponse response = presence.startSubscription(getAlice().getAor(), getBob().getAor(), 100); //3
    	assertThat(response, isSuccess());  //4
    	
    	SipServletRequest notify = presence.waitForNotify(); // 5
    	notify.createResponse(SipServletResponse.SC_OK).send(); // 6

		Presence presenceDoc = getPresence(notify);
		assertEquals(Basic.OPEN, presenceDoc.getTupleArray()[0].getStatus().getBasic());
		
		Subscriber winfo = new Subscriber("presence.winfo", getBob().customize(new Dialog()));
		response = winfo.startSubscription(getBob().getAor(), getBob().getAor(), 60); //7
    	assertThat(response, isSuccess()); // 8
    	
    	notify = winfo.waitForNotify(); //9
    	notify.createResponse(SipServletResponse.SC_OK).send(); //10

		Watcherinfo watcherinfo = getWatcherinfo(notify);
		assertEquals(0, watcherinfo.getVersion().intValue());
		assertEquals(Watcherinfo.State.FULL, watcherinfo.getState());
		assertEquals(1, watcherinfo.getWatcherListArray().length);
		WatcherList watcherList = watcherinfo.getWatcherListArray(0);
		assertEquals(getUri(getBob()), watcherList.getResource());
		assertEquals(PresenceEventPackage.NAME, watcherList.getPackage());
		assertEquals(1, watcherList.getWatcherArray().length);
		Watcher watcher = watcherList.getWatcherArray(0);
		assertEquals(Event.SUBSCRIBE, watcher.getEvent());
		assertEquals(getUri(getAlice()), watcher.getStringValue());
		assertEquals(Status.ACTIVE, watcher.getStatus());
		
		HttpClient httpClient = new HttpClient();
		PutMethod put = new PutMethod(getHttpXcapUri() + BOB_PRES_RULES_URI); // 11
		
		InputStream is = WatcherInfoTest.class.getResourceAsStream("/xcap-root/pres-rules/users/put/elementPoliteBlock.xml");
		RequestEntity entity = new InputStreamRequestEntity(is, "application/xcap-el+xml"); 
		put.setRequestEntity(entity); 
		
		int result = httpClient.executeMethod(put);
		assertEquals(200, result); // 12
		put.releaseConnection();
		
		notify = presence.waitForNotify(); //13
    	notify.createResponse(SipServletResponse.SC_OK).send(); //14

		presenceDoc = getPresence(notify);
		assertEquals(Basic.CLOSED, presenceDoc.getTupleArray()[0].getStatus().getBasic());
		
		notify = winfo.waitForNotify(); //15
    	notify.createResponse(SipServletResponse.SC_OK).send(); //16

		watcherinfo = getWatcherinfo(notify);
		assertEquals(1, watcherinfo.getVersion().intValue());
		assertEquals(Watcherinfo.State.FULL, watcherinfo.getState());
		assertEquals(1, watcherinfo.getWatcherListArray().length);
		watcherList = watcherinfo.getWatcherListArray(0);
		assertEquals(getUri(getBob()), watcherList.getResource());
		assertEquals(PresenceEventPackage.NAME, watcherList.getPackage());
		assertEquals(1, watcherList.getWatcherArray().length);
		watcher = watcherList.getWatcherArray(0);
		assertEquals(Event.SUBSCRIBE, watcher.getEvent());
		assertEquals(getUri(getAlice()), watcher.getStringValue());
		assertEquals(Status.ACTIVE, watcher.getStatus());
		
		response = winfo.stopSubscription(); //17
    	assertThat(response, isSuccess()); //18
    	
    	notify = winfo.waitForNotify(); // 19
    	notify.createResponse(SipServletResponse.SC_OK).send(); // 20
    	
    	
    	response = presence.stopSubscription(); //21
    	assertThat(response, isSuccess()); //22
    	
    	notify = presence.waitForNotify(); // 23
    	notify.createResponse(SipServletResponse.SC_OK).send(); // 24

    	publisher.newUnPublish().send();
    	assertThat(publisher.waitForResponse(), isSuccess());
	}
	
	/**
	 * <pre>
	     Bob                Kaleo                 Alice
          |                   |(1) PUBLISH          |
          |                   |<--------------------|
          |                   |(2) 200 OK           |
          |                   |-------------------->|
          |(3) SUBSCRIBE      |                     |
          |Event:presence     |                     |
          |Expires: 0         |                     |
          |------------------>|                     |
          |(4) 200 OK         |                     |
          |<------------------|                     |
          |(5) NOTIFY         |                     | In pending state, so basic status is closed
          |<------------------|                     |
          |(6) 200 OK         |                     |
          |------------------>|                     |
          |                   |(7) SUBSCRIBE        |
          |                   |Event:presence.winfo |
          |                   |<--------------------|
          |                   |(8) 200 OK           |
          |                   |-------------------->|
          |                   |(9) NOTIFY           |
          |                   |-------------------->|
          |                   |(10) 200 OK          |
          |                   |<--------------------|
          |                   |(11) HTTP PUT        | Change subscription state from 
          |                   |<--------------------| allow to polite-block
          |                   |(12) 200 OK          |
          |                   |-------------------->|
          |(13) SUBSCRIBE     |                     |
          |Event:presence     |                     |
          |Expires: 0         |                     |
          |------------------>|                     |
          |(14) 200 OK        |                     |
          |<------------------|                     |
          |(15) NOTIFY        |                     |
          |<------------------|                     |
          |(16) 200 OK        |                     |
          |------------------>|                     |
          |                   |(17) PUBLISH         |
          |                   |Expires = 0          |
          |                   |<--------------------|
          |                   |(18) 200 OK          |
          |                   |-------------------->|
     * </pre>
     * Note: Alice and Bob are inverted in this test.
	 */
	public void testWaitingState() throws Exception
	{
		Publisher publisher = new Publisher(getAlice().getAor());
		getAlice().customize(publisher);
		SipServletRequest request = publisher.newPublish(getClass().getResourceAsStream("publish1.xml"), 60);
		publisher.start(request); // 1
		assertThat(publisher.waitForResponse(), isSuccess()); //2
		
		Subscriber presence = new Subscriber("presence", getBob().customize(new Dialog()));
		SipServletResponse response = presence.startSubscription(getBob().getAor(), getAlice().getAor(), 0); //3
    	assertThat(response, isSuccess());  //4
    	
    	SipServletRequest notify = presence.waitForNotify(); // 5
    	notify.createResponse(SipServletResponse.SC_OK).send(); // 6
		
		Presence presenceDoc = getPresence(notify);
		assertEquals(Basic.CLOSED, presenceDoc.getTupleArray()[0].getStatus().getBasic());
		
		Subscriber winfo = new Subscriber("presence.winfo", getAlice().customize(new Dialog()));
		response = winfo.startSubscription(getAlice().getAor(), getAlice().getAor(), 60); //7
    	assertThat(response, isSuccess()); // 8
    	
    	notify = winfo.waitForNotify(); //9
    	notify.createResponse(SipServletResponse.SC_OK).send(); //10

		Watcherinfo watcherinfo = getWatcherinfo(notify);
		assertEquals(0, watcherinfo.getVersion().intValue());
		assertEquals(Watcherinfo.State.FULL, watcherinfo.getState());
		assertEquals(1, watcherinfo.getWatcherListArray().length);
		WatcherList watcherList = watcherinfo.getWatcherListArray(0);
		assertEquals(getUri(getAlice()), watcherList.getResource());
		assertEquals(PresenceEventPackage.NAME, watcherList.getPackage());
		assertEquals(1, watcherList.getWatcherArray().length);
		Watcher watcher = watcherList.getWatcherArray(0);
		assertEquals(Event.TIMEOUT, watcher.getEvent());
		assertEquals(getUri(getBob()), watcher.getStringValue());
		assertEquals(Status.WAITING, watcher.getStatus());	
		
		HttpClient httpClient = new HttpClient();
		PutMethod put = new PutMethod(getHttpXcapUri() + ALICE_PRES_RULES_URI); // 11
		
		InputStream is = WatcherInfoTest.class.getResourceAsStream("/xcap-root/pres-rules/users/put/elementCondAliceBob.xml");
		RequestEntity entity = new InputStreamRequestEntity(is, "application/xcap-el+xml"); 
		put.setRequestEntity(entity); 
		
		int result = httpClient.executeMethod(put);
		assertEquals(200, result); // 12
		put.releaseConnection();
		
		presence = new Subscriber("presence", getBob().customize(new Dialog()));
		response = presence.startSubscription(getBob().getAor(), getAlice().getAor(), 0); // 13
    	assertThat(response, isSuccess());  //14
    	
    	notify = presence.waitForNotify(); // 15
    	notify.createResponse(SipServletResponse.SC_OK).send(); // 16

		presenceDoc = getPresence(notify);
		assertEquals(Basic.OPEN, presenceDoc.getTupleArray()[0].getStatus().getBasic());
		
		publisher.newUnPublish().send(); // 17
    	assertThat(publisher.waitForResponse(), isSuccess()); //18
	}
		
	private Watcherinfo getWatcherinfo(SipServletRequest request)
	{
		assertThat(request.getContentType(), is("application/watcherinfo+xml"));
		try
		{
			return WatcherinfoDocument.Factory.parse(new ByteArrayInputStream(request.getRawContent())).getWatcherinfo();
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	private Presence getPresence(SipServletRequest request)
	{
		assertThat(request.getContentType(), is("application/pidf+xml"));
		try
		{
			return PresenceDocument.Factory.parse(new ByteArrayInputStream(request.getRawContent())).getPresence();
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
    public void testMinExpires() throws Exception
    {    
    	SipServletRequest request = getAlice().createRequest(SipMethods.SUBSCRIBE, getAlice().getAor());
    	request.setHeader(SipHeaders.EVENT, "presence.winfo");
    	request.setExpires(1);
    	SipServletResponse response = getAlice().sendSynchronous(request);
    	assertThat(response, hasStatus(SipServletResponse.SC_INTERVAL_TOO_BRIEF));
    	assertThat(response, hasHeader(SipHeaders.MIN_EXPIRES));
    }
    
}
