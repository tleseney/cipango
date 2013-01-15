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
package org.cipango.server.processor;

import org.cipango.server.SipProcessor;
import org.cipango.server.SipServer;
import org.eclipse.jetty.util.component.ContainerLifeCycle;

public abstract class AbstractSipProcessor extends ContainerLifeCycle implements SipProcessor 
{
	private SipServer _server;
	
	public void setServer(SipServer server)
	{
		_server = server;
	}
	
	public SipServer getServer()
	{
		return _server;
	}
}
