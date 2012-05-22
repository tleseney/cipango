package org.cipango.server.handler;


import org.cipango.server.SipHandler;
import org.cipango.server.SipServer;
import org.eclipse.jetty.util.component.AbstractLifeCycle;

public abstract class AbstractSipHandler extends AbstractLifeCycle implements SipHandler
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
