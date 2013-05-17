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

package org.cipango.kaleo.location;


import java.util.Collections;
import java.util.List;

import org.cipango.kaleo.AbstractResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocationService extends AbstractResourceManager<Registration>
{
	private Logger _log = LoggerFactory.getLogger(LocationService.class);
	
	protected Registration newResource(String uri)
	{
		return new Registration(uri);
	}
	
	public List<Binding> getBindings(String uri)
	{
		ResourceHolder holder = getHolder(uri);
		if (holder == null)
			return Collections.emptyList();
		else
			return holder.getResource().getBindings();
	}
}
