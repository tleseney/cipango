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
import java.util.Collections;
import java.util.List;

import javax.servlet.sip.SipSession;

import org.cipango.kaleo.Constants;
import org.cipango.kaleo.event.AbstractEventPackage;
import org.cipango.kaleo.event.ContentHandler;
import org.cipango.kaleo.event.State;
import org.cipango.kaleo.event.Subscription;
import org.cipango.kaleo.event.SubscriptionListener;
import org.cipango.kaleo.presence.PresenceEventPackage;
import org.cipango.kaleo.presence.Presentity;
import org.cipango.kaleo.presence.watcherinfo.WatcherinfoDocument.Watcherinfo;

public class WatcherInfoEventPackage extends AbstractEventPackage<WatcherResource>
{
	
	public static final String NAME = "presence.winfo";
	public static final String WATCHERINFO = "application/watcherinfo+xml";
	
	private PresenceEventPackage _presenceEventPackage;
	private WatcherinfoHandler _handler = new WatcherinfoHandler();
	private SubscriptionListener _subscriptionListener = new SubListener();

	public WatcherInfoEventPackage(PresenceEventPackage presence)
	{
		_presenceEventPackage = presence;
	}
	
	
	protected WatcherResource newResource(String uri)
	{
		Presentity presentity = _presenceEventPackage.get(uri);
		try
		{
			WatcherResource watcherResource = new WatcherResource(uri, presentity);
			watcherResource.addListener(getEventNotifier());
			return watcherResource;
		}
		finally
		{
			_presenceEventPackage.put(presentity);
		}
	}

	public List<String> getSupportedContentTypes() 
	{
		return Collections.singletonList(WATCHERINFO);
	}
	
	public ContentHandler<?> getContentHandler(String contentType)
	{
		if (WATCHERINFO.equals(contentType))
			return _handler;
		return null;
	}

	public String getName()
	{
		return NAME;
	}
	
	public SubscriptionListener getSubscriptionListener()
	{
		return _subscriptionListener;
	}
	
	@Override
	protected void preprocessState(SipSession session, State state)
	{
		BigInteger version = (BigInteger) session.getAttribute(Constants.VERSION_ATTRIBUTE);
		if (version == null)
			version = BigInteger.ZERO;
		else
			version = version.add(BigInteger.ONE);
		Watcherinfo watcherinfo = ((WatcherinfoDocument) state.getContent()).getWatcherinfo();
		
		watcherinfo.setVersion(version);
		session.setAttribute(Constants.VERSION_ATTRIBUTE, version);
	}

	class SubListener implements SubscriptionListener
	{
		public void subscriptionStateChanged(Subscription subscription, 
				Subscription.State previousState, Subscription.State newState)
		{
			WatcherResource resource = get(subscription.getResource().getUri());
			try
			{
				resource.subscriptionStateChanged(subscription, previousState, newState);
			}
			finally
			{
				put(resource);
			}
		}
		
	}

}
