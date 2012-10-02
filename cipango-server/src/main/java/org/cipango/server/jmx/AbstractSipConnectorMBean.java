package org.cipango.server.jmx;

import org.cipango.server.SipConnector;
import org.eclipse.jetty.jmx.ObjectMBean;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;

@ManagedObject
public class AbstractSipConnectorMBean extends ObjectMBean
{
	
	
	public AbstractSipConnectorMBean(Object managedObject)
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
	
	private SipConnector getConnector()
	{
		return (SipConnector) getManagedObject();
	}

}
