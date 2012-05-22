package org.cipango.server.processor;

import org.cipango.server.SipProcessor;
import org.cipango.server.SipServer;
import org.eclipse.jetty.util.component.AbstractLifeCycle;

public abstract class AbstractSipProcessor extends AbstractLifeCycle implements SipProcessor 
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
