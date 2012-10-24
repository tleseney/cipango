package org.cipango.server;

import java.io.IOException;
import java.net.InetAddress;

import javax.servlet.sip.SipURI;

import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.component.LifeCycle;

@ManagedObject("SIP connector")
public interface SipConnector extends LifeCycle
{

	Transport getTransport(); 
	
	void open() throws IOException;
	void close() throws IOException;

	int getDefaultPort();
	boolean isReliable();
	boolean isSecure();
	
	@ManagedAttribute(value="Host", readonly=true)
	String getHost();
	
	void setPort(int port);

	@ManagedAttribute(value="Port", readonly=true)
	int getPort();
	
	SipURI getURI();
		
	SipConnection getConnection(InetAddress address, int port) throws IOException;
	
	/**
	 * @return the actual address on which the connector is bound
	 */
	InetAddress getAddress();
}

