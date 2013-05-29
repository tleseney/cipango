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
package org.cipango.diameter.node;

import java.io.IOException;
import java.net.InetAddress;

import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.component.LifeCycle;

@ManagedObject
public interface DiameterConnector extends LifeCycle
{
	void setNode(Node node);
	
	void open() throws IOException;
	void close() throws IOException;

	DiameterConnection getConnection(Peer peer) throws IOException;
	
	void setHost(String host);
	
	@ManagedAttribute(value="Host", readonly=true)
	String getHost();
	
	void setPort(int port);
	@ManagedAttribute(value="Port", readonly=true)
	int getPort();
	
	int getLocalPort();
	
	@ManagedAttribute(value="Local address", readonly=true)
	InetAddress getLocalAddress();
}
