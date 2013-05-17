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
package org.cipango.kaleo.presence.watcherinfo;

import java.math.BigInteger;
import java.util.List;

import org.cipango.kaleo.event.AbstractEventResource;
import org.cipango.kaleo.event.State;
import org.cipango.kaleo.event.Subscription;
import org.cipango.kaleo.event.SubscriptionListener;
import org.cipango.kaleo.event.Subscription.Reason;
import org.cipango.kaleo.presence.PresenceEventPackage;
import org.cipango.kaleo.presence.Presentity;
import org.cipango.kaleo.presence.watcherinfo.WatcherDocument.Watcher;
import org.cipango.kaleo.presence.watcherinfo.WatcherDocument.Watcher.Event;
import org.cipango.kaleo.presence.watcherinfo.WatcherDocument.Watcher.Status;
import org.cipango.kaleo.presence.watcherinfo.WatcherListDocument.WatcherList;
import org.cipango.kaleo.presence.watcherinfo.WatcherinfoDocument.Watcherinfo;

public class WatcherResource extends AbstractEventResource implements SubscriptionListener
{
	private WatcherinfoDocument _content;
	private State _state;

	public WatcherResource(String uri, Presentity presentity)
	{
		super(uri);
		_content = WatcherinfoDocument.Factory.newInstance();
		Watcherinfo watcherinfo = _content.addNewWatcherinfo();
		watcherinfo.setState(org.cipango.kaleo.presence.watcherinfo.WatcherinfoDocument.Watcherinfo.State.FULL);
		watcherinfo.setVersion(BigInteger.ZERO);
		WatcherList list = watcherinfo.addNewWatcherList();
		list.setResource(uri);
		list.setPackage(PresenceEventPackage.NAME);
		
		if (presentity != null)
		{
			List<Subscription> subscriptions = presentity.getSubscriptions();
			for (Subscription subscription : subscriptions)
			{
				Watcher watcher = list.addNewWatcher();
				watcher.setStringValue(subscription.getUri());
				watcher.setId(subscription.getId());
				watcher.setStatus(getStatus(subscription.getState()));
				watcher.setEvent(getEvent(subscription.getReason()));
			}
		}
		
		// TODO set watchers
		_state = new State(WatcherInfoEventPackage.WATCHERINFO, _content);
	}
	
	private void modifyWatcher(Subscription subscription)
	{
		WatcherList list = _content.getWatcherinfo().getWatcherListArray(0);
		for (int i = 0; i < list.getWatcherArray().length; i++)
		{
			Watcher watcher = list.getWatcherArray(i);
			if (watcher.getStringValue().equals(subscription.getUri()))
			{
				watcher.setStatus(getStatus(subscription.getState()));
				watcher.setEvent(getEvent(subscription.getReason()));
				return;
			}
		}
		Watcher watcher = list.addNewWatcher();
		watcher.setStringValue(subscription.getUri());
		watcher.setId(subscription.getId());
		watcher.setStatus(getStatus(subscription.getState()));
		watcher.setEvent(getEvent(subscription.getReason()));
	}
	
	private Event.Enum getEvent(Reason reason)
	{
		switch (reason)
		{
		case APPROVED:
			return Event.APPROVED;
		case DEACTIVATED:
			return Event.DEACTIVATED;
		case GIVEUP:
			return Event.GIVEUP;
		case NORESOURCE:
			return Event.NORESOURCE;
		case PROBATION:
			return Event.PROBATION;
		case REJECTED:
			return Event.REJECTED;
		case SUBSCRIBE:
			return Event.SUBSCRIBE;
		case TIMEOUT:
			return Event.TIMEOUT;
		default:
			throw new IllegalStateException("Invalid reason: " + reason);
		}
	}
	
	private Status.Enum getStatus(org.cipango.kaleo.event.Subscription.State state)
	{
		switch (state)
		{
		case ACTIVE:
			return Status.ACTIVE;
		case PENDING:
			return Status.PENDING;
		case WAITING:
			return Status.WAITING;
		case TERMINATED:
			return Status.TERMINATED;
		default:
			throw new IllegalStateException("Invalid state: " + state);
		}
	}

	public State getState()
	{
		return _state;
	}

	public boolean isDone()
	{
		// TODO improve algo
		return  _content.getWatcherinfo().getWatcherListArray(0).getWatcherArray().length == 0
			&& !hasSubscribers();
	}

	public void subscriptionStateChanged(Subscription subscription, Subscription.State previousState, Subscription.State newState)
	{
		modifyWatcher(subscription);
		//if (previousState == Subscription.State.PENDING && newState == Subscription.State.WAITING)
		fireStateChanged();	
		
		if (newState == Subscription.State.TERMINATED)
		{
			WatcherList list = _content.getWatcherinfo().getWatcherListArray(0);
			for (int i = 0; i < list.getWatcherArray().length; i++)
			{
				Watcher watcher = list.getWatcherArray(i);
				if (watcher.getStringValue().equals(subscription.getUri()))
				{
					list.removeWatcher(i);
					break;
				}
			}
		}
	}

	public State getNeutralState()
	{
		WatcherinfoDocument document = WatcherinfoDocument.Factory.newInstance();
		Watcherinfo watcherinfo = document.addNewWatcherinfo();
		watcherinfo.setState(org.cipango.kaleo.presence.watcherinfo.WatcherinfoDocument.Watcherinfo.State.FULL);
		watcherinfo.setVersion(BigInteger.ZERO);
		WatcherList list = watcherinfo.addNewWatcherList();
		list.setResource(getUri());
		list.setPackage(PresenceEventPackage.NAME);
		
		return new State(WatcherInfoEventPackage.WATCHERINFO, document);
	}

}
