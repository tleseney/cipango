package org.cipango.server.handler;


import org.cipango.server.SipHandler;
import org.cipango.server.SipServer;
import org.eclipse.jetty.util.component.ContainerLifeCycle;

public abstract class AbstractSipHandler extends ContainerLifeCycle implements SipHandler
{
	private SipServer _server;
	
	public void setServer(SipServer server) 
	{
		if (_server==server)
            return;
        if (isStarted())
            throw new IllegalStateException(STARTED);
        _server = server;
	}

	public SipServer getServer() 
	{
		return _server;
	}
		
}
