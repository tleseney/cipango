package org.cipango.console;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.servlet.http.HttpServletRequest;

import org.cipango.console.data.Property;
import org.cipango.console.data.PropertyList;
import org.cipango.console.menu.MenuImpl;
import org.cipango.console.util.ObjectNameFactory;
import org.cipango.console.util.PrinterUtil;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.log.StdErrLog;

public class EnvManager
{
	private MBeanServerConnection _mbsc;
	private NumberFormat _percentFormat;
	
	public static final ObjectName OPERATING_SYSTEM = ObjectNameFactory.create(ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME);
	public static final ObjectName LOGBACK = ObjectNameFactory.create("ch.qos.logback.classic:Name=default,Type=ch.qos.logback.classic.jmx.JMXConfigurator");
	public static final ObjectName JETTY_LOGGER = ObjectNameFactory.create("org.eclipse.jetty.util.log:type=log,id=0");
	
	
	public static final Action RELOAD_lOG_CONF = Action.add(new Action(MenuImpl.SYSTEM_LOGS, "reload-system-logs-conf")
	{
		@Override
		public void doProcess(HttpServletRequest request, MBeanServerConnection mbsc) throws Exception
		{
			if (mbsc.isRegistered(LOGBACK))
			{
				mbsc.invoke(LOGBACK, "reloadDefaultConfiguration", new Object[0], new String[0]);
			}
		}	
	});
	
	public static final Action SET_LOGGER_LEVEL = Action.add(new Action(MenuImpl.SYSTEM_LOGS, "set-logger-level")
	{
		@Override
		public void doProcess(HttpServletRequest request, MBeanServerConnection mbsc) throws Exception
		{
			if (mbsc.isRegistered(LOGBACK))
			{
				String level = request.getParameter("level");
				String logger = request.getParameter("logger");
				mbsc.invoke(LOGBACK, "setLoggerLevel", new Object[] { logger, level } , new String[] { "java.lang.String", "java.lang.String" });
			}
		}	
	});
	
	public static final Action SET_DEBUG_ENABLED = Action.add(new Action(MenuImpl.SYSTEM_LOGS, "set-debug-enabled")
	{
		@Override
		public void doProcess(HttpServletRequest request, MBeanServerConnection mbsc) throws Exception
		{
			if (mbsc.isRegistered(JETTY_LOGGER))
			{
				Boolean enabled = "true".equalsIgnoreCase(request.getParameter("enabled"));
				String logger = request.getParameter("logger");
				mbsc.invoke(JETTY_LOGGER, "setDebugEnabled", new Object[] { logger, enabled } , new String[] { "java.lang.String", "java.lang.Boolean" });
			}
		}	
	});
	
	private static Logger __logger = Log.getLogger(EnvManager.class);
	
	public EnvManager(MBeanServerConnection mbsc)
	{
		_mbsc = mbsc;
		_percentFormat = DecimalFormat.getPercentInstance();
		_percentFormat.setMinimumFractionDigits(1);
	}
	
	
	public PropertyList getVersion() throws Exception
	{
		PropertyList properties = new PropertyList(_mbsc, JettyManager.SERVER, "version");
		properties.addAll(new PropertyList(_mbsc, SipManager.SERVER, "version"));
		properties.add(new Property("Startup Time", new Date(getRuntime().getStartTime())));
		properties.add(new Property("Server Uptime", PrinterUtil.getDuration(getRuntime().getUptime())));
		return properties;
	}

	public PropertyList getJava() throws Exception
	{
		PropertyList env = new PropertyList();
		env.setTitle("Java");
		Map<String, String> properties = getSystemProperties();
		env.add(new Property("Java Runtime", properties.get("java.runtime.name") + " " + properties.get("java.runtime.version")));
		
		MemoryUsage r = getMemory().getHeapMemoryUsage();
		
		String percentage = _percentFormat.format(((float) r.getUsed()) / r.getMax());
		env.add(new Property("Memory", (r.getUsed() >> 20) + " Mb of " + (r.getMax() >> 20) + " Mb (" +
				percentage + ")"));

		env.add(new Property("Threads", getThread().getThreadCount()));

		env.add(new Property("Process CPU time", 
				PrinterUtil.getDuration((Long) _mbsc.getAttribute(OPERATING_SYSTEM, "ProcessCpuTime")/1000000)));
		
		String load = isLoadAvailable() ? _percentFormat.format((Double) _mbsc.getAttribute(OPERATING_SYSTEM, "ProcessCpuLoad"))
				: "Not available";
		env.add(new Property("Process CPU load", load));
		
		StringBuilder sb = new StringBuilder();
		for (String arg : getRuntime().getInputArguments())
			sb.append(arg).append(' ');
		env.add(new Property("VM arguments", sb.toString()));
		env.add(new Property("Jetty Home", properties.get("jetty.home")));
		
		return env;
	}
	
	

	public PropertyList getHardware() throws Exception
	{
		PropertyList env = new PropertyList();
		env.setTitle("Hardware");
		Map<String, String> properties = getSystemProperties();
		
		env.add(new Property("OS / Hardware", properties.get("os.name") + " " + properties.get("os.version")
				+ " - " + properties.get("os.arch")));
				
		OperatingSystemMXBean os = getOperatingSystem();
				
		String load = isLoadAvailable() ? _percentFormat.format((Double) _mbsc.getAttribute(OPERATING_SYSTEM, "SystemCpuLoad"))
				: "Not available";
		env.add(new Property("System CPU load", load));

		if (isHardwareMemoryAvailable())
		{
			long totalMemory = (Long) _mbsc.getAttribute(OPERATING_SYSTEM, "TotalPhysicalMemorySize");
			long freeMemory = (Long) _mbsc.getAttribute(OPERATING_SYSTEM, "FreePhysicalMemorySize");
			long usedMemory = totalMemory - freeMemory;
			String percentage = _percentFormat.format(((float) usedMemory) / totalMemory);
			env.add(new Property("Physical memory", (usedMemory >> 20) + " Mb used of "
					+ (totalMemory >> 20) + " Mb (" + percentage + ")"));
			
			totalMemory = (Long) _mbsc.getAttribute(OPERATING_SYSTEM, "TotalSwapSpaceSize");
			freeMemory = (Long) _mbsc.getAttribute(OPERATING_SYSTEM, "FreeSwapSpaceSize");
			usedMemory = totalMemory - freeMemory;
			percentage = _percentFormat.format(((float) usedMemory) / totalMemory);
			env.add(new Property("Swap memory", (usedMemory >> 20) + " Mb used of "
					+ (totalMemory >> 20) + " Mb (" + percentage + ")"));
		}
		
		env.add(new Property("Available processors", os.getAvailableProcessors()));
		StringBuilder sb = new StringBuilder();
		for (String arg :  getRuntime().getInputArguments())
			sb.append(arg).append(' ');
		
		return env;
	}
	
	private boolean isLoadAvailable() throws Exception
	{
		if (!_mbsc.isRegistered(OPERATING_SYSTEM))
			return false;
		
		MBeanInfo info = _mbsc.getMBeanInfo(OPERATING_SYSTEM);
		for (MBeanAttributeInfo attrInfo : info.getAttributes())
			if ("ProcessCpuLoad".equals(attrInfo.getName()))
				return true;
		return false;
	}
	
	private boolean isHardwareMemoryAvailable() throws Exception
	{
		if (!_mbsc.isRegistered(OPERATING_SYSTEM))
			return false;
		
		MBeanInfo info = _mbsc.getMBeanInfo(OPERATING_SYSTEM);
		for (MBeanAttributeInfo attrInfo : info.getAttributes())
			if ("TotalPhysicalMemorySize".equals(attrInfo.getName()))
				return true;
		return false;
	}
	
	public OperatingSystemMXBean getOperatingSystem() throws IOException
	{
		return ManagementFactory.newPlatformMXBeanProxy(_mbsc, ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME, OperatingSystemMXBean.class);
	}
	
	
	public RuntimeMXBean getRuntime() throws IOException
	{
		return ManagementFactory.newPlatformMXBeanProxy(_mbsc, ManagementFactory.RUNTIME_MXBEAN_NAME, RuntimeMXBean.class);
	}
	
	public MemoryMXBean getMemory() throws IOException
	{
		return ManagementFactory.newPlatformMXBeanProxy(_mbsc, ManagementFactory.MEMORY_MXBEAN_NAME, MemoryMXBean.class);
	}
	
	public ThreadMXBean getThread() throws IOException
	{
		return ManagementFactory.newPlatformMXBeanProxy(_mbsc, ManagementFactory.THREAD_MXBEAN_NAME, ThreadMXBean.class);
	}
	
	public SortedMap<String, String> getSystemProperties() throws AttributeNotFoundException, InstanceNotFoundException, MBeanException, ReflectionException, IOException
	{	
		Map<String, String> properties = getRuntime().getSystemProperties();
		
		SortedMap<String, String> map = new TreeMap<String, String>(properties);
		return map;
	}
	
	public boolean isLogbackAvailable() throws IOException
	{
		return _mbsc.isRegistered(LOGBACK);
	}
	
	public boolean isJettyLogAvailable() throws IOException
	{
		return _mbsc.isRegistered(JETTY_LOGGER);
	}
	
	public String getLogLevel(String logger) throws IOException, InstanceNotFoundException, MBeanException, ReflectionException
	{
		return (String) _mbsc.invoke(LOGBACK, "getLoggerEffectiveLevel", new Object[] { logger }, new String[] { "java.lang.String" });
	}
	
	public boolean isDebugEnabled(String logger) throws IOException, InstanceNotFoundException, MBeanException, ReflectionException
	{
		return (Boolean) _mbsc.invoke(JETTY_LOGGER, "isDebugEnabled", new Object[] { logger }, new String[] { "java.lang.String" });
	}
	
	public boolean isStdErrorLoggerUsed()
	{
		return __logger instanceof StdErrLog;
	}
}
