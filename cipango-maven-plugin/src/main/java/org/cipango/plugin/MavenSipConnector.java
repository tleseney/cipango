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
package org.cipango.plugin;

import java.io.IOException;
import java.net.InetAddress;

import javax.servlet.sip.SipURI;

import org.cipango.server.SipConnection;
import org.cipango.server.SipConnector;
import org.cipango.server.SipServer;
import org.cipango.server.Transport;
import org.eclipse.jetty.util.component.AbstractLifeCycle;

public abstract class MavenSipConnector extends AbstractLifeCycle implements SipConnector
{
	private SipServer server;
	private String host;
	private int port;
	private SipConnector delegate;
	
	
	@Override
	protected void doStart() throws Exception
	{
		
		if (getServer() == null)
			throw new IllegalStateException("Server not set for MavenServerConnector");
		
		this.delegate = newDelegate();
		this.delegate.setPort(getPort());
		this.delegate.setHost(getHost());

		this.delegate.start();
		
		super.doStart();
	}
	
	@Override
	protected void doStop() throws Exception
	{
		this.delegate.stop();
		super.doStop();
		this.delegate = null;
	}
	
	protected abstract SipConnector newDelegate();
	
	public void setServer(SipServer server)
    {
        this.server = server;
    }
	
	public SipServer getServer()
	{
		return server;
	}
	
	@Override
	public void open() throws IOException
	{
		delegate.open();
	}

	@Override
	public void close() throws IOException
	{
		delegate.close();
	}

	@Override
	public String getHost()
	{
		return host;
	}
	
	@Override
	public void setHost(String host)
	{
		this.host = host;
	}

	@Override
	public void setPort(int port)
	{
		this.port = port;
	}

	@Override
	public int getPort()
	{
		return port;
	}

	@Override
	public SipURI getURI()
	{
		return delegate.getURI();
	}

	@Override
	public SipConnection getConnection(InetAddress address, int port) throws IOException
	{
		return delegate.getConnection(address, port);
	}

	@Override
	public InetAddress getAddress()
	{
		return delegate.getAddress();
	}
	
	
	
}
