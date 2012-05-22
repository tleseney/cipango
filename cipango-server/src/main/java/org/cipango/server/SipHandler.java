package org.cipango.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.sip.SipServletMessage;

import org.eclipse.jetty.util.component.LifeCycle;

public interface SipHandler extends LifeCycle
{
	public void handle(SipMessage message) throws IOException, ServletException;
	
	public void setServer(SipServer server);
	public SipServer getServer();
}
