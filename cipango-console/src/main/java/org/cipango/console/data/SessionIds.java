// ========================================================================
// Copyright 2012 NEXCOM Systems
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
package org.cipango.console.data;

import java.util.List;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

public class SessionIds
{
	private ObjectName _sessionManager;
	private List<String> _sessionIds;
	
	@SuppressWarnings("unchecked")
	public SessionIds(MBeanServerConnection mbsc, ObjectName sessionManager) throws Exception
	{
		_sessionManager = sessionManager;
		_sessionIds = (List<String>) mbsc.getAttribute(_sessionManager, "applicationSessionIds");
	}

	public ObjectName getSessionManager()
	{
		return _sessionManager;
	}

	public List<String> getSessionIds()
	{
		return _sessionIds;
	}
	
	public String getName()
	{
		return _sessionManager.getKeyProperty("context");
	}
	
	
}
