package org.cipango.server;

import org.eclipse.jetty.util.component.LifeCycle;

public interface SipProcessor extends LifeCycle
{
	public void doProcess(SipMessage message) throws Exception;
	
	public void setServer(SipServer server);
	public SipServer getServer();
}
