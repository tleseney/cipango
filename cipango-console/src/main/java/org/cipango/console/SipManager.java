// ========================================================================
// Copyright 2012 NEXCOM Systems
// ------------------------------------------------------------------------
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at 
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ========================================================================
package org.cipango.console;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.servlet.http.HttpServletRequest;

import org.cipango.console.data.ConsoleLogger;
import org.cipango.console.data.ConsoleLogger.ClearConsoleLoggerAction;
import org.cipango.console.data.ConsoleLogger.MessageInMemoryAction;
import org.cipango.console.data.ConsoleLogger.StartConsoleLoggerAction;
import org.cipango.console.data.ConsoleLogger.StopConsoleLoggerAction;
import org.cipango.console.data.FileLogger;
import org.cipango.console.data.FileLogger.DeleteLogsFilesAction;
import org.cipango.console.data.FileLogger.StartFileLoggerAction;
import org.cipango.console.data.FileLogger.StopFileLoggerAction;
import org.cipango.console.data.Property;
import org.cipango.console.data.PropertyList;
import org.cipango.console.data.Row;
import org.cipango.console.data.SessionIds;
import org.cipango.console.data.Row.Header;
import org.cipango.console.data.Row.Value;
import org.cipango.console.data.SipConsoleLogger;
import org.cipango.console.data.Table;
import org.cipango.console.menu.MenuImpl;
import org.cipango.console.util.ConsoleUtil;
import org.cipango.console.util.ObjectNameFactory;
import org.cipango.console.util.Parameters;
import org.cipango.console.util.PrinterUtil;

public class SipManager extends Manager
{
	private static final String[] GET_MSG_SIGNATURE = { Integer.class.getName(), String.class.getName() };
	
	public static final ObjectName 
		SERVER = ObjectNameFactory.create("org.cipango.server:type=sipserver,id=0"),
		CONNECTOR_MANAGER = ObjectNameFactory.create("org.cipango.server:type=connectormanager,id=0"),
		CONSOLE_LOGGER = ObjectNameFactory.create("org.cipango.callflow:type=jmxmessagelog,id=0"),
		FILE_MESSAGE_LOG = ObjectNameFactory.create("org.cipango.server.log:type=filemessagelog,id=0"),
		HANDLER_COLLECTION = ObjectNameFactory.create("org.cipango.server.handler:type=sipcontexthandlercollection,id=0"),
		TRANSACTION_MANAGER = ObjectNameFactory.create("org.cipango.server.transaction:type=transactionmanager,id=0");;
	
	public static final Action CHANGE_TIME_GRAPH = Action.add(new Action(MenuImpl.STATISTICS_GRAPH, "change-time")
	{
		@Override
		public void doProcess(HttpServletRequest request, MBeanServerConnection mbsc) throws Exception
		{
			String time = request.getParameter(Parameters.TIME);
			if (time != null)
				request.getSession().setAttribute(Parameters.TIME, Integer.parseInt(time));
		}		
	});
	
	public static final Action START_GRAPH = Action.add(new Action(MenuImpl.STATISTICS_SIP, "start-graph")
	{
		@Override
		public void doProcess(HttpServletRequest request, MBeanServerConnection mbsc) throws Exception
		{
			getStatisticGraph(request).start();
		}
	});
	
	public static final Action STOP_GRAPH = Action.add(new Action(MenuImpl.STATISTICS_SIP, "stop-graph")
	{
		@Override
		public void doProcess(HttpServletRequest request, MBeanServerConnection mbsc) throws Exception
		{
			getStatisticGraph(request).stop();
		}	
	});
		
	public static final Action RESET_STATS = Action.add(new Action(MenuImpl.STATISTICS_SIP, "reset-statistics")
	{
		@Override
		public void doProcess(HttpServletRequest request, MBeanServerConnection mbsc) throws Exception
		{
			getStatisticGraph(request).reset();
			mbsc.invoke(JettyManager.SERVER, "allStatsReset", null, null);
		}	
	});
	
	
	public static final Map<String, String> FILTERS = ConsoleUtil.getFilters(ResourceBundle.getBundle("org.cipango.console.sip-filters"));
		
	static
	{
		Action.add(new StopFileLoggerAction(MenuImpl.SIP_LOGS, FILE_MESSAGE_LOG));
		Action.add(new StartFileLoggerAction(MenuImpl.SIP_LOGS, FILE_MESSAGE_LOG));
		Action.add(new DeleteLogsFilesAction(MenuImpl.SIP_LOGS)
		{
			@Override
			public void doProcess(HttpServletRequest request, MBeanServerConnection mbsc) throws Exception
			{
				mbsc.invoke(FILE_MESSAGE_LOG, "deleteLogFiles", null, null);
			}	
		});
		Action.add(new StopConsoleLoggerAction(
			MenuImpl.SIP_LOGS, CONSOLE_LOGGER));
		Action.add(new StartConsoleLoggerAction(
			MenuImpl.SIP_LOGS, CONSOLE_LOGGER));
		Action.add(new ClearConsoleLoggerAction(MenuImpl.SIP_LOGS)
		{
			@Override
			public void doProcess(HttpServletRequest request, MBeanServerConnection mbsc) throws Exception
			{
				mbsc.invoke(CONSOLE_LOGGER, "clear", null, null);
			}	
		});
		Action.add(new MessageInMemoryAction(MenuImpl.SIP_LOGS, CONSOLE_LOGGER));
	}
			
	public SipManager(MBeanServerConnection mbsc)
	{
		super(mbsc);
	}
	
	public PropertyList getMessageStats() throws Exception
	{
		return new PropertyList(_mbsc, "sip.stats.messages");
	}
	
	public Table getTransactionStats() throws Exception
	{
		Table table = new Table();
		table.setTitle("Transactions");
		List<Header> headers = new ArrayList<Row.Header>();
		headers.add(new Header(""));
		headers.add(new Header("Current"));
		headers.add(new Header("Max"));
		headers.add(new Header("Total"));
		table.setHeaders(headers);
		Iterator<Header> it = headers.iterator();
		Row row = new Row();
		row.getValues().add(new Value("Client transaction", it.next()));
		row.getValues().add(new Value(_mbsc.getAttribute(TRANSACTION_MANAGER, "clientTransactions"), it.next()));
		row.getValues().add(new Value(_mbsc.getAttribute(TRANSACTION_MANAGER, "clientTransactionsMax"), it.next()));
		row.getValues().add(new Value(_mbsc.getAttribute(TRANSACTION_MANAGER, "clientTransactionsTotal"), it.next()));
		table.add(row);
		it = headers.iterator();
		row = new Row();
		row.getValues().add(new Value("Server transaction", it.next()));
		row.getValues().add(new Value(_mbsc.getAttribute(TRANSACTION_MANAGER, "serverTransactions"), it.next()));
		row.getValues().add(new Value(_mbsc.getAttribute(TRANSACTION_MANAGER, "serverTransactionsMax"), it.next()));
		row.getValues().add(new Value(_mbsc.getAttribute(TRANSACTION_MANAGER, "serverTransactionsTotal"), it.next()));
		table.add(row);
		return table;
	}
	
	
	public Table getAppSessionStats() throws Exception
	{
		return getAppSessionStats("sip.stats.applicationSessions");
	}
	
	public Table getAppSessionTimeStats() throws Exception
	{
		return getAppSessionStats("sip.stats.applicationSessions.time");
	}
	
	public ObjectName[] getSessionManagers() throws Exception
	{
		ObjectName[] contexts =  (ObjectName[]) _mbsc.getAttribute(SipManager.HANDLER_COLLECTION, "sipContexts");
		ObjectName[] sessionManagers = new ObjectName[contexts.length];
		for (int i = 0; i < contexts.length; i++)
		{
			ObjectName sessionHandler = (ObjectName) _mbsc.getAttribute(contexts[i], "sessionHandler");
			sessionManagers[i] = (ObjectName) _mbsc.getAttribute(sessionHandler, "sessionManager");
		}
		return sessionManagers;
	}
	
	public Table getAppSessionStats(String key) throws Exception
	{
		ObjectName[] contexts =  (ObjectName[]) _mbsc.getAttribute(SipManager.HANDLER_COLLECTION, "sipContexts");
		ObjectName[] sessionManagers = new ObjectName[contexts.length];
		for (int i = 0; i < contexts.length; i++)
		{
			ObjectName sessionHandler = (ObjectName) _mbsc.getAttribute(contexts[i], "sessionHandler");
			sessionManagers[i] = (ObjectName) _mbsc.getAttribute(sessionHandler, "sessionManager");
		}
		
		Table table = new Table(_mbsc, sessionManagers, key)
		{					
			@Override
			protected Header getHeader(String param, MBeanAttributeInfo[] attrInfos, String propertyName)
			{
				if ("name".equals(param))
					return new Header("name", "Name");
				Header header = super.getHeader(param, attrInfos, propertyName);
				
				for (String s : new String[] { "amount of time session remained valid",	"application sessions" })
				{
					int index = header.getName().indexOf(s);	
					if (index != -1)
						header.setName(header.getName().substring(0, index));
				}
				return header;
			}
			
			@Override
			protected Value getValue(MBeanServerConnection connection, ObjectName objectName, Header header)
					throws Exception
			{
				if ("name".equals(header.getSimpleName()))
					return new Value(objectName.getKeyProperty("context"), header);
				Value value = super.getValue(connection, objectName, header);
				if (value.getValue() instanceof Double)
				{
					DecimalFormat format = new DecimalFormat();
					format.setMaximumFractionDigits(2);
					value.setValue(format.format(value.getValue()));
				}
				return value;
			}
		};
		return table;
	}
	
	public String getStatsDuration() throws Exception
	{
		Long start = (Long) _mbsc.getAttribute(SERVER, "statsStartedAt");
		return  PrinterUtil.getDuration(System.currentTimeMillis() - start);
	}	
	
	public Table getConnectorsConfig() throws Exception
	{
		ObjectName[] connectors = (ObjectName[]) _mbsc.getAttribute(SERVER, "connectors");
		return new Table(_mbsc, connectors, "sip.connectors");
	}
	
	public PropertyList getThreadPool() throws Exception
	{
		ObjectName threadPool = (ObjectName) _mbsc.getAttribute(SERVER, "threadPool");
		
		PropertyList properties = new PropertyList(_mbsc, threadPool, "sip.threadPool");
		for (Property property : properties)
		{
			String name = property.getName();
			int index = Math.max(name.indexOf("in pool"), name.indexOf("in the pool"));
			if (index != -1)
				property.setName(name.substring(0, index));
		}
		return properties;
	}
	
	public PropertyList getTimers() throws Exception
	{
		return new PropertyList(_mbsc, TRANSACTION_MANAGER, "sip.timers");
	}
		
	public FileLogger getFileLogger() throws Exception
	{
		return new FileLogger(_mbsc, MenuImpl.SIP_LOGS, FILE_MESSAGE_LOG, true);
	}
	
	public SipConsoleLogger getConsoleLogger(HttpServletRequest request) throws Exception
	{
		
		SipConsoleLogger logger = new SipConsoleLogger(_mbsc, CONSOLE_LOGGER, FILTERS);
		logger.setMessageFilter(request.getParameter(Parameters.MESSAGE_FILTER));
		logger.setMaxMessages(ConsoleUtil.getParamValueAsInt(Parameters.MAX_MESSAGES, request, ConsoleLogger.DEFAULT_MAX_MESSAGES));


		if (logger.isRegistered() && logger.isEnabled())
		{
			Object[] params = { new Integer(logger.getMaxMessages()), logger.getMessageFilter() };
			Object[][] messagesLogs = (Object[][]) _mbsc.invoke(
						CONSOLE_LOGGER, "getMessages", params,
						GET_MSG_SIGNATURE);
			logger.setMessages(messagesLogs);
		}
		return logger;
	}
	
	public List<SessionIds> getApplicationIds() throws Exception
	{
		ObjectName[] sessionManagers = getSessionManagers();
		List<SessionIds> sessionIds = new ArrayList<SessionIds>(sessionManagers.length);
		for (ObjectName sessionManager : sessionManagers)
			sessionIds.add(new SessionIds(_mbsc, sessionManager));
		return sessionIds;
	}
	
	public String getSipApplicationSession(String id, String objectName) throws Exception
	{
		ObjectName sessionManager = new ObjectName(objectName);
		if (!_mbsc.isRegistered(sessionManager))
			return "The application with name " + sessionManager.getKeyProperty("context") + " is no more registered";
		
		return (String) _mbsc.invoke(sessionManager, 
				"viewApplicationSession",
				new Object[] { id }, 
				new String[] { "java.lang.String" });
	}
}
