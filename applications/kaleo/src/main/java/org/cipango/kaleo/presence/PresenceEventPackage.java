// ========================================================================
// Copyright 2008-2009 NEXCOM Systems
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

package org.cipango.kaleo.presence;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.cipango.kaleo.event.AbstractEventPackage;
import org.cipango.kaleo.event.ContentHandler;
import org.cipango.kaleo.event.Subscription;
import org.cipango.kaleo.event.Subscription.Reason;
import org.cipango.kaleo.event.Subscription.State;
import org.cipango.kaleo.presence.pidf.PidfHandler;
import org.cipango.kaleo.presence.policy.Policy;
import org.cipango.kaleo.presence.policy.PolicyListener;
import org.cipango.kaleo.presence.policy.PolicyManager;
import org.cipango.kaleo.presence.policy.PolicyManager.SubHandling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Presence Event Package
 */
public class PresenceEventPackage extends AbstractEventPackage<Presentity>
{
	public Logger _log = LoggerFactory.getLogger(PresenceEventPackage.class);
	
	public static final String NAME = "presence";
	public static final String PIDF = "application/pidf+xml";
	
	private PidfHandler _pidfHandler = new PidfHandler();
	
	public int _minStateExpires = 1;
	public int _maxStateExpires = 3600;
	public int _defaultStateExpires = 3600;
	
	private PolicyListener _policyListener = new PolicyUpdater();
	
	private PolicyManager _policyManager;
	
	public PresenceEventPackage()
	{
	}

	public String getName()
	{
		return NAME;
	}

	public int getMinStateExpires()
	{
		return _minStateExpires;
	}
	
	public int getMaxStateExpires()
	{
		return _maxStateExpires;
	}
	
	public int getDefaultStateExpires()
	{
		return _defaultStateExpires;
	}
	
	protected Presentity newResource(String uri)
	{
		Presentity presentity = new Presentity(uri);
		presentity.addListener(getEventNotifier());
		Policy policy = _policyManager.getPolicy(presentity);
		policy.addListener(_policyListener);
		return presentity;
	}
	
	@Override
	protected void removeResource(Presentity presentity)
	{
		super.removeResource(presentity);
		Policy policy = _policyManager.getPolicy(presentity);
		policy.removeListener(_policyListener);
	}
	
	public List<String> getSupportedContentTypes()
	{
		return Collections.singletonList(PIDF);
	}
	
	public ContentHandler<?> getContentHandler(String contentType)
	{
		if (PIDF.equals(contentType))
			return _pidfHandler;
		else
			return null;
	}
	
	public PolicyListener getPolicyListener()
	{
		return _policyListener;
	}
	
	public PolicyManager getPolicyManager()
	{
		return _policyManager;
	}

	public void setPolicyManager(PolicyManager policyManager)
	{
		_policyManager = policyManager;
	}
	
	class PolicyUpdater implements PolicyListener
	{

		public void policyHasChanged(Policy policy)
		{
		
			Presentity presentity = (Presentity) get(policy.getResourceUri());

			try
			{
				Iterator<Subscription> it = presentity.getSubscriptions().iterator();
				while (it.hasNext())
				{
					Subscription subscription = it.next();
					SubHandling subHandling = policy.getPolicy(subscription.getUri());
					
					State state = subscription.getState();
					boolean authorised = subscription.isAuthorized();
					switch (subHandling)
					{
					case ALLOW:
						subscription.setState(State.ACTIVE, Reason.APPROVED, true);
						break;
					case CONFIRM:
						subscription.setState(State.PENDING, Reason.SUBSCRIBE, true);
						break;
					case POLITE_BLOCK:
						subscription.setState(State.ACTIVE, Reason.SUBSCRIBE, false);
						break;
					case BLOCK:
						subscription.setState(State.TERMINATED, Reason.REJECTED, false);
						break;
					default:
						break;
					}
					
					// send NOTIFY if state has changed.
					if (state != subscription.getState() || authorised != subscription.isAuthorized())
					{
						PresenceEventPackage.this.notify(subscription);
					}
				}
			}
			finally
			{
				put(presentity);
			}
		}
		
	}


}
