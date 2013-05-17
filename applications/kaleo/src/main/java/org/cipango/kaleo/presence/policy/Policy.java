// ========================================================================
// Copyright 2010 NEXCOM Systems
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

import org.cipango.kaleo.presence.policy.PolicyManager.SubHandling;

/**
 * Policy used authorizing subscriptions.
 *
 *
 * @see  <A href="http://tools.ietf.org/rfc/rfc5025.txt">RFC 5025: Presence Authorization Rules</A>
 */
public interface Policy
{

	public String getResourceUri();

	public SubHandling getPolicy(String subscriberUri);

	public void addListener(PolicyListener l);
	
	public void removeListener(PolicyListener l);

}