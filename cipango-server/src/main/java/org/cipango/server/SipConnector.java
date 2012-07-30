package org.cipango.server;

import java.io.IOException;

import javax.servlet.sip.SipURI;

import org.eclipse.jetty.util.component.LifeCycle;

public interface SipConnector extends LifeCycle
{
	Transport getTransport(); 
	
	void open() throws IOException;
	void close() throws IOException;
	
	String getHost();
	
	void setPort(int port);
	int getPort();
	
	SipURI getURI();
	
	/**
	 * @return the actual address on which the connector is bound
	 */
	//InetAddress getAddress();
	
	void setServer(SipServer server);
}

