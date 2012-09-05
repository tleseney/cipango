package org.cipango.console.data;

import java.util.Map;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.cipango.console.menu.MenuImpl;

public class DiameterConsoleLogger extends ConsoleLogger
{
	
	private DiameterMessageLog[] _messages;
		
	public DiameterConsoleLogger(MBeanServerConnection mbsc, ObjectName objectName, Map<String, String> filters) throws Exception
	{
		super(mbsc, MenuImpl.DIAMETER_LOGS, objectName, filters);
	}
		
	public String getFilterTitle()
	{
		return getMessageFilter();
	}

	public DiameterMessageLog[] getMessages()
	{
		return _messages;
	}

	public void setMessages(Object[][] messagesLogs)
	{
		_messages = new DiameterMessageLog[messagesLogs.length];
		for (int i = 0; i < _messages.length; i++)
			_messages[i] = new DiameterMessageLog(messagesLogs[i]);
	}
	
	public static class DiameterMessageLog
	{
		private Object[] _array;

		public DiameterMessageLog(Object[] array)
		{
			_array = array;
		}

		public String getInfoLine()
		{
			return _array[0].toString();
		}

		public Object getMessage()
		{
			return  _array[1];
		}
		
		public String getRemote()
		{
			return (String) _array[2];
		}

	}


	
}
