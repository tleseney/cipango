package org.cipango.console.data;

import java.util.Map;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;

import org.cipango.console.menu.MenuImpl;

public class SipConsoleLogger extends ConsoleLogger
{
	
	public static final String 
		CALL_ID_FILTER = "message.callId",
		BRANCH_FILTER = "message.topVia.branch",
		TO_FILTER = "message.to.uRI.toString()",
		FROM_FILTER = "message.from.uRI.toString()",
		REMOTE_FILTER = "remote",
		REQUEST_URI_FILTER = "message.requestURI != null and message.requestURI.toString()";


	private SipMessageLog[] _messages;
		
	public SipConsoleLogger(MBeanServerConnection mbsc, ObjectName objectName, Map<String, String> filters) throws Exception
	{
		super(mbsc, MenuImpl.SIP_LOGS, objectName, filters);
	}
		
	public String getFilterTitle()
	{
		String filterValue = getMessageFilter().substring(getMessageFilter().lastIndexOf('(') + 1);
		filterValue = filterValue.substring(0, filterValue.length() - 1);
		if (getMessageFilter().startsWith(CALL_ID_FILTER))
			 return "Call-ID is " + filterValue;
		else if (getMessageFilter().startsWith(BRANCH_FILTER))
			 return "Branch is " + filterValue;
		else if (getMessageFilter().startsWith(TO_FILTER))
			 return "To URI is " + filterValue;
		else if (getMessageFilter().startsWith(FROM_FILTER))
			 return "From URI is " + filterValue;
		else if (getMessageFilter().startsWith(REQUEST_URI_FILTER))
			 return "Request-URI is " + filterValue;
		else if (getMessageFilter().startsWith(REMOTE_FILTER))
			 return "Remote host is " + filterValue;
		else
			 return getMessageFilter();
	}

	public SipMessageLog[] getMessages()
	{
		return _messages;
	}

	public void setMessages(Object[][] messagesLogs)
	{
		_messages = new SipMessageLog[messagesLogs.length];
		for (int i = 0; i < _messages.length; i++)
			_messages[i] = new SipMessageLog(messagesLogs[i]);
	}
	
	public static class SipMessageLog
	{
		private Object[] _array;

		public SipMessageLog(Object[] array)
		{
			_array = array;
		}

		public String getInfoLine()
		{
			return _array[0].toString();
		}

		public SipServletMessage getMessage()
		{
			return (SipServletMessage) _array[1];
		}
		
		public String getRemote()
		{
			return (String) _array[2];
		}

		public boolean isRequest()
		{
			return getMessage() instanceof SipServletRequest;
		}
	}


	
}
