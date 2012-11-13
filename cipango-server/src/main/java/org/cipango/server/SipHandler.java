package org.cipango.server;

import java.io.IOException;

import javax.servlet.ServletException;

import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.component.LifeCycle;

@ManagedObject("SIP handler")
public interface SipHandler extends LifeCycle
{
	public void handle(SipMessage message) throws IOException, ServletException;
	
	public void setServer(SipServer server);
	
	@ManagedAttribute(value="the SIP server for this handler", readonly=true)
	public SipServer getServer();
}
