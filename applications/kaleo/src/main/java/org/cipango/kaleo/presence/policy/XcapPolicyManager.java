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
package org.cipango.kaleo.presence.policy;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.cipango.kaleo.Resource;
import org.cipango.kaleo.xcap.XcapService;

public class XcapPolicyManager implements PolicyManager
{
	
	private XcapService _xcapService;
	private Map<String, XcapPolicy> _policies = new HashMap<String, XcapPolicy>();
	
	
	public XcapPolicyManager(XcapService xcapService)
	{
		_xcapService = xcapService;
	}

	public SubHandling getPolicy(String subscriberUri, Resource resource)
	{
		return getPolicy(resource).getPolicy(subscriberUri);
	}

	public Policy getPolicy(Resource resource)
	{
		XcapPolicy policy;
		synchronized (_policies)
		{
			policy = _policies.get(resource.getUri());
			if (policy == null)
			{
				policy = new XcapPolicy(resource, _xcapService);
				_policies.put(resource.getUri(), policy);
			}
		}
		return policy;
	}

	public Collection<XcapPolicy> getPolicies()
	{
		return _policies.values();
	}
		

}
