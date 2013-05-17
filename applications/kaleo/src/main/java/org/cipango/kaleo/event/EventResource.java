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

package org.cipango.kaleo.event;

import java.util.List;
import org.cipango.kaleo.Resource;

public interface EventResource extends Resource
{
	State getState();
	State getNeutralState();
	
	void addSubscription(Subscription subscription);
	Subscription getSubscription(String id);
	Subscription removeSubscription(String id);
	List<Subscription> getSubscriptions();
	
	void addListener(EventResourceListener listener);
	List<EventResourceListener> getListeners();
}
