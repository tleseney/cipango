package org.cipango.console;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.management.Attribute;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.servlet.http.HttpServletRequest;

import org.cipango.console.data.PropertyList;
import org.cipango.console.data.Table;
import org.cipango.console.menu.MenuImpl;
import org.cipango.console.util.ObjectNameFactory;
import org.cipango.console.util.PrinterUtil;

public class DiameterManager
{
	public static final ObjectName 
		NODE = ObjectNameFactory.create("org.cipango.diameter.node:type=node,id=0"),
		PEERS = ObjectNameFactory.create("org.cipango.diameter.node:type=peer,*"),
		FILE_LOG = ObjectNameFactory.create("org.cipango.diameter.log:type=filemessagelogger,id=0"),
		CONSOLE_LOG = ObjectNameFactory.create("org.cipango.callflow.diameter:type=jmxmessagelogger,id=0");
	
	public static final Action ENABLE_STATS = Action.add(new Action(MenuImpl.STATISTICS_DIAMETER, "enable-statistics")
	{
		@Override
		public void doProcess(HttpServletRequest request, MBeanServerConnection mbsc) throws Exception
		{
			mbsc.setAttribute(NODE, new Attribute("statsOn", Boolean.TRUE));
		}	
	});
	
	public static final Action DISABLE_STATS = Action.add(new Action(MenuImpl.STATISTICS_DIAMETER, "disable-statistics")
	{
		@Override
		public void doProcess(HttpServletRequest request, MBeanServerConnection mbsc) throws Exception
		{
			mbsc.setAttribute(NODE, new Attribute("statsOn", Boolean.FALSE));
		}	
	});
	
	public static final Action RESET_STATS = Action.add(new Action(MenuImpl.STATISTICS_DIAMETER, "reset-statistics")
	{
		@Override
		public void doProcess(HttpServletRequest request, MBeanServerConnection mbsc) throws Exception
		{
			mbsc.invoke(NODE, "statsReset", null, null);
		}	
	});
	
	
	private MBeanServerConnection _mbsc;
	
	public DiameterManager(MBeanServerConnection mbsc)
	{
		_mbsc = mbsc;
	}
	
	public boolean isRegistered() throws IOException
	{
		return _mbsc.isRegistered(NODE);
	}
	
	public PropertyList getNodeConfig() throws Exception
	{
		return new PropertyList(_mbsc, NODE, "diameter.node");
	}
	
	public Table getConnectors() throws Exception
	{
		ObjectName[] transports = (ObjectName[]) _mbsc.getAttribute(NODE, "connectors");
		return new Table(_mbsc, transports, "diameter.transport");
	}
	
	public PropertyList getTimers() throws Exception
	{
		return new PropertyList(_mbsc, NODE, "diameter.timers");
	}
	
	public Table getPeers() throws Exception
	{
		Set<ObjectName> peers = _mbsc.queryNames(PEERS, null);
		return new Table(_mbsc, peers, "diameter.peers");
	}
	
	public PropertyList getSessionsStats() throws Exception
	{
		ObjectName objectName = (ObjectName) _mbsc.getAttribute(NODE, "sessionManager");
		return new PropertyList(_mbsc, objectName, "diameter.stats.sessions");
	}
	
	public List<PropertyList> getMessageStats() throws Exception
	{
		ObjectName[] transports = (ObjectName[]) _mbsc.getAttribute(NODE, "connectors");

		List<PropertyList> l = new ArrayList<PropertyList>(transports.length);
		for (int i = 0; i < transports.length; i++)
			l.add(new PropertyList(_mbsc, transports[i], "diameter.stats.msg"));
		return l;
	}
	
	public Table getPendingStats() throws Exception
	{
		Set<ObjectName> peers = _mbsc.queryNames(PEERS, null);
		return new Table(_mbsc, peers, "diameter.stats.pending");
	}
	
	public String getStatsDuration() throws Exception
	{
		Long start = (Long)  _mbsc.getAttribute(NODE, "statsStartedAt");
		return PrinterUtil.getDuration(System.currentTimeMillis() - start);
	}
	
	public boolean isStatsEnabled() throws Exception
	{
		return  (Boolean) _mbsc.getAttribute(NODE, "statsOn");
	}
}
