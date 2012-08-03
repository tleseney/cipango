package org.cipango.console;

import java.io.IOException;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.cipango.console.data.Table;
import org.cipango.console.util.ObjectNameFactory;

public class SnmpManager
{
	public static final ObjectName AGENT = ObjectNameFactory.create("org.cipango.snmp:type=snmpagent,id=0");
		
	
	private MBeanServerConnection _mbsc;
	
	public SnmpManager(MBeanServerConnection mbsc)
	{
		_mbsc = mbsc;
	}
	
	public boolean isRegistered() throws IOException
	{
		return _mbsc.isRegistered(AGENT);
	}

	
	public Table getConnectors() throws Exception
	{
		ObjectName[] connectors = (ObjectName[]) _mbsc.getAttribute(AGENT, "connectors");
		return new Table(_mbsc, connectors, "snmp.connectors");
	}

	public Table getTrapReceivers() throws Exception
	{
		ObjectName[] traps = (ObjectName[]) _mbsc.getAttribute(AGENT, "trapReceivers");
		return new Table(_mbsc, traps, "snmp.trap");
	}
}
