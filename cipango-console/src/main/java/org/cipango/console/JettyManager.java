package org.cipango.console;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.management.Attribute;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.servlet.http.HttpServletRequest;

import org.cipango.console.data.Property;
import org.cipango.console.data.PropertyList;
import org.cipango.console.data.Row.Header;
import org.cipango.console.data.Table;
import org.cipango.console.menu.MenuImpl;
import org.cipango.console.util.ObjectNameFactory;
import org.cipango.console.util.PrinterUtil;

public class JettyManager
{
	
	public static final ObjectName 
		SERVER = ObjectNameFactory.create("org.eclipse.jetty.server:type=server,id=0"),
		HTTP_LOG = ObjectNameFactory.create("org.eclipse.jetty:type=ncsarequestlog,id=0");
	
	public static final Action ENABLE_STATS = Action.add(new Action(MenuImpl.STATISTICS_HTTP, "enable-statistics")
	{
		@Override
		public void doProcess(HttpServletRequest request, MBeanServerConnection mbsc) throws Exception
		{
			ObjectName[] connectors = (ObjectName[]) mbsc.getAttribute(SERVER, "connectors");
			for (ObjectName objectName : connectors)
				mbsc.setAttribute(objectName, new Attribute("statsOn", Boolean.TRUE));
		}	
	});
	
	public static final Action DISABLE_STATS = Action.add(new Action(MenuImpl.STATISTICS_HTTP, "disable-statistics")
	{
		@Override
		public void doProcess(HttpServletRequest request, MBeanServerConnection mbsc) throws Exception
		{
			ObjectName[] connectors = (ObjectName[]) mbsc.getAttribute(SERVER, "connectors");
			for (ObjectName objectName : connectors)
				mbsc.setAttribute(objectName, new Attribute("statsOn", Boolean.FALSE));
		}	
	});
	
	public static final Action RESET_STATS = Action.add(new Action(MenuImpl.STATISTICS_HTTP, "reset-statistics")
	{
		@Override
		public void doProcess(HttpServletRequest request, MBeanServerConnection mbsc) throws Exception
		{
			ObjectName[] connectors = (ObjectName[]) mbsc.getAttribute(SERVER, "connectors");
			for (ObjectName objectName : connectors)
				mbsc.invoke(objectName, "statsReset", null, null);
		}	
	});
	

	private MBeanServerConnection _mbsc;
	
	public JettyManager(MBeanServerConnection mbsc)
	{
		_mbsc = mbsc;
	}
	
	public ObjectName[] getConnectors() throws Exception
	{
		return (ObjectName[]) _mbsc.getAttribute(SERVER, "connectors");
	}
	
	public PropertyList getHttpThreadPool() throws Exception
	{
		ObjectName threadPool = (ObjectName) _mbsc.getAttribute(SERVER, "threadPool");
		PropertyList properties = new PropertyList(_mbsc, threadPool, "http.threadPool");
		for (Property property : properties)
		{
			String name = property.getName();
			int index = Math.max(name.indexOf("in pool"), name.indexOf("in the pool"));
			if (index != -1)
				property.setName(name.substring(0, index));
		}
		return properties;
	}
	
	public Table getConnectorsConfig() throws Exception
	{
		return new Table(_mbsc, getConnectors(), "http.connectors")
		{

			@Override
			protected Header getHeader(String param, MBeanAttributeInfo[] attrInfos, String propertyName)
			{
				String name = param.substring(1);
				return new Header(param, Character.toUpperCase(param.charAt(0)) + name);
			}
			
		};
	}
	
	public List<PropertyList> getConnectorsStatistics() throws Exception
	{
		ObjectName[] connectors = (ObjectName[]) _mbsc.getAttribute(SERVER, "connectors");
		List<PropertyList> l = new ArrayList<PropertyList>();
		for (int i = 0; i < connectors.length; i++)
		{
			final ObjectName objectName = connectors[i];
			
			PropertyList propertyList = new PropertyList(_mbsc, objectName, "http.statistics");
			propertyList.setTitle("Connector: " + (String) _mbsc.getAttribute(objectName, "name"));
			Iterator<Property> it = propertyList.iterator();
			while (it.hasNext())
			{
				Property property = (Property) it.next();
				String name = property.getName();
				int index = name.indexOf("since statsReset()");
				if (index != -1)
					property.setName(name.substring(0, index));
			}
			l.add(propertyList);
		}
		return l;
	}
	
	public boolean isStatsEnabled() throws Exception
	{
		return  (Boolean) _mbsc.getAttribute(getConnectors()[0], "statsOn");
	}
	
	public String getStatsDuration() throws Exception
	{
		return  PrinterUtil.getDuration((Long) _mbsc.getAttribute(getConnectors()[0], "statsOnMs"));
	}	
	
	public String dump() throws Exception
	{
		return (String) _mbsc.invoke(SERVER, "dump", null, new String[0]);
	}
}
