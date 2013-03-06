package org.cipango.websocket.jmx;

import org.cipango.server.SipConnector;
import org.eclipse.jetty.jmx.ObjectMBean;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;

@ManagedObject
public class WebSocketConnectorMBean extends ObjectMBean
{
	
	
	public WebSocketConnectorMBean(Object managedObject)
	{
		super(managedObject);
	}
	
	@ManagedAttribute("Secure")
	public boolean isSecure()
	{
		return getConnector().getTransport().isSecure();
	}

	@ManagedAttribute("Reliable")
	public boolean isReliable()
	{
		return getConnector().getTransport().isReliable();
	}

	@ManagedAttribute("Transport")
	public String getTransport()
	{
		return getConnector().getTransport().getName();
	}
	
	@ManagedAttribute(value="Acceptors", readonly=true)
	public String getAcceptors()
	{
		return "N/A";
	}
	
	private SipConnector getConnector()
	{
		return (SipConnector) getManagedObject();
	}

}
