package org.cipango.server.handler;


import org.cipango.server.SipHandler;
import org.cipango.server.SipServer;
import org.eclipse.jetty.util.component.ContainerLifeCycle;

public abstract class AbstractSipHandler extends ContainerLifeCycle implements SipHandler
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
