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
package org.cipango.kaleo.presence.winfo;

import org.cipango.kaleo.event.EventResource;
import org.cipango.kaleo.event.EventResourceListener;
import org.cipango.kaleo.event.Subscription;
import org.cipango.kaleo.event.Subscription.Reason;
import org.cipango.kaleo.presence.PresenceEventPackage;
import org.cipango.kaleo.presence.Presentity;
import org.cipango.kaleo.presence.watcherinfo.WatcherResource;
import org.cipango.kaleo.presence.watcherinfo.WatcherinfoDocument;
import org.cipango.kaleo.presence.watcherinfo.WatcherDocument.Watcher;
import org.cipango.kaleo.presence.watcherinfo.WatcherDocument.Watcher.Event;
import org.cipango.kaleo.presence.watcherinfo.WatcherDocument.Watcher.Status;
import org.cipango.kaleo.presence.watcherinfo.WatcherListDocument.WatcherList;
import org.cipango.kaleo.presence.watcherinfo.WatcherinfoDocument.Watcherinfo;
import org.cipango.kaleo.presence.watcherinfo.WatcherinfoDocument.Watcherinfo.State;

import junit.framework.TestCase;

public class WatcherResourceTest extends TestCase
{
	private static final String AOR = "sip:alice@cipango.org";
	private static final String SUBSCRIBER_AOR = "sip:bob@cipango.org";

	public void testGetState() throws Exception
	{
		Presentity presentity = new Presentity(AOR);
		WatcherResource resource = new WatcherResource(AOR, presentity);
		EventListener eventListener = new EventListener();
		resource.addListener(eventListener);
		//System.out.println(resource.getState().getContent());
		Watcherinfo watcherinfo = ((WatcherinfoDocument) resource.getState().getContent()).getWatcherinfo();
		assertEquals(State.FULL, watcherinfo.getState());
		assertEquals(1, watcherinfo.getWatcherListArray().length);
		WatcherList watcherList = watcherinfo.getWatcherListArray(0);
		assertEquals(AOR, watcherList.getResource());
		assertEquals(PresenceEventPackage.NAME, watcherList.getPackage());
		assertEquals(0, watcherList.getWatcherArray().length);
		
		Subscription subscription = new Subscription(presentity, null, 100, SUBSCRIBER_AOR);
		subscription.addListener(resource);
		subscription.setState(Subscription.State.PENDING, Reason.SUBSCRIBE);
		//System.out.println(resource.getState().getContent());
		assertEquals(1, eventListener._nbNotif);
		
		subscription.setState(Subscription.State.ACTIVE, Reason.APPROVED);
		assertEquals(2, eventListener._nbNotif);
		
		subscription.setState(Subscription.State.TERMINATED, Reason.TIMEOUT);
		assertEquals(3, eventListener._nbNotif);
		assertEquals(0, watcherList.getWatcherArray().length);
	}
	
	class EventListener implements EventResourceListener
	{
		private int _nbNotif = 0;

		public void stateChanged(EventResource resource)
		{
			_nbNotif++;
			Watcherinfo watcherinfo = ((WatcherinfoDocument) resource.getState().getContent()).getWatcherinfo();
			WatcherList watcherList = watcherinfo.getWatcherListArray(0);
			if (_nbNotif == 1)
			{
				assertEquals(1, watcherList.getWatcherArray().length);
				Watcher watcher1 = watcherList.getWatcherArray(0);
				assertEquals(SUBSCRIBER_AOR, watcher1.getStringValue());
				assertEquals(Event.SUBSCRIBE, watcher1.getEvent());
				assertEquals(Status.PENDING, watcher1.getStatus());
			} 
			else if (_nbNotif == 2)
			{
				assertEquals(1, watcherList.getWatcherArray().length);
				Watcher watcher1 = watcherList.getWatcherArray(0);
				assertEquals(SUBSCRIBER_AOR, watcher1.getStringValue());
				assertEquals(Event.APPROVED, watcher1.getEvent());
				assertEquals(Status.ACTIVE, watcher1.getStatus());
			}
			else if (_nbNotif == 3)
			{
				assertEquals(1, watcherList.getWatcherArray().length);
				Watcher watcher1 = watcherList.getWatcherArray(0);
				assertEquals(SUBSCRIBER_AOR, watcher1.getStringValue());
				assertEquals(Event.TIMEOUT, watcher1.getEvent());
				assertEquals(Status.TERMINATED, watcher1.getStatus());
			}
			else
			{
				fail("Unexpected notification: " + _nbNotif + "\n" + watcherinfo);
			}
		}

		public void subscriptionExpired(Subscription subscription)
		{
		}
		
	}
}
