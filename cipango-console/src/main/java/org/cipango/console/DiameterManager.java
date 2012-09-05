package org.cipango.console;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javax.management.Attribute;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.servlet.http.HttpServletRequest;

import org.cipango.console.data.ConsoleLogger;
import org.cipango.console.data.DiameterConsoleLogger;
import org.cipango.console.data.FileLogger;
import org.cipango.console.data.PropertyList;
import org.cipango.console.data.SipConsoleLogger;
import org.cipango.console.data.Table;
import org.cipango.console.data.ConsoleLogger.ClearConsoleLoggerAction;
import org.cipango.console.data.ConsoleLogger.MessageInMemoryAction;
import org.cipango.console.data.ConsoleLogger.StartConsoleLoggerAction;
import org.cipango.console.data.ConsoleLogger.StopConsoleLoggerAction;
import org.cipango.console.data.FileLogger.DeleteLogsFilesAction;
import org.cipango.console.data.FileLogger.StartFileLoggerAction;
import org.cipango.console.data.FileLogger.StopFileLoggerAction;
import org.cipango.console.menu.MenuImpl;
import org.cipango.console.util.ConsoleUtil;
import org.cipango.console.util.ObjectNameFactory;
import org.cipango.console.util.Parameters;
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
	
	public static final Map<String, String> FILTERS = ConsoleUtil.getFilters(ResourceBundle.getBundle("org.cipango.console.diameter-filters"));
	
	
	static
	{
		Action.add(new StopFileLoggerAction(MenuImpl.DIAMETER_LOGS, FILE_LOG));
		Action.add(new StartFileLoggerAction(MenuImpl.DIAMETER_LOGS, FILE_LOG));
		Action.add(new DeleteLogsFilesAction(MenuImpl.DIAMETER_LOGS)
		{
			@Override
			public void doProcess(HttpServletRequest request, MBeanServerConnection mbsc) throws Exception
			{
				mbsc.invoke(FILE_LOG, "deleteLogFiles", null, null);
			}	
		});
		Action.add(new StopConsoleLoggerAction(MenuImpl.DIAMETER_LOGS, CONSOLE_LOG));
		Action.add(new StartConsoleLoggerAction(MenuImpl.DIAMETER_LOGS, CONSOLE_LOG));
		Action.add(new ClearConsoleLoggerAction(MenuImpl.DIAMETER_LOGS)
		{
			@Override
			public void doProcess(HttpServletRequest request, MBeanServerConnection mbsc) throws Exception
			{
				mbsc.invoke(CONSOLE_LOG, "clear", null, null);
			}	
		});
		Action.add(new MessageInMemoryAction(MenuImpl.DIAMETER_LOGS, CONSOLE_LOG));
	}
	
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
	
	public FileLogger getFileLogger() throws Exception
	{
		return new FileLogger(_mbsc, MenuImpl.DIAMETER_LOGS, FILE_LOG, true);
	}
	
	public DiameterConsoleLogger getConsoleLogger(HttpServletRequest request) throws Exception
	{
		DiameterConsoleLogger logger = new DiameterConsoleLogger(_mbsc, CONSOLE_LOG, FILTERS);
		logger.setMessageFilter(request.getParameter(Parameters.MESSAGE_FILTER));
		logger.setMaxMessages(ConsoleUtil.getParamValueAsInt(Parameters.MAX_MESSAGES, request, ConsoleLogger.DEFAULT_MAX_MESSAGES));


		if (logger.isRegistered() && logger.isEnabled())
		{
			Object[] params = { new Integer(logger.getMaxMessages()), logger.getMessageFilter() };
			Object[][] messagesLogs = (Object[][]) _mbsc.invoke(
						CONSOLE_LOG, "getMessages", params,
						new String[] { Integer.class.getName(), String.class.getName() });
			logger.setMessages(messagesLogs);
		}
		return logger;
	}
}
