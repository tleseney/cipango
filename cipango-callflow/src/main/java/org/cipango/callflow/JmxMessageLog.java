// ========================================================================
// Copyright 2006-2013 NEXCOM Systems
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
package org.cipango.callflow;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.management.ListenerNotFoundException;
import javax.management.MBeanNotificationInfo;
import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;

import org.apache.commons.jexl.Expression;
import org.apache.commons.jexl.ExpressionFactory;
import org.apache.commons.jexl.JexlContext;
import org.apache.commons.jexl.JexlHelper;
import org.cipango.server.SipConnection;
import org.cipango.server.SipMessage;
import org.cipango.server.log.AbstractMessageLog;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.annotation.ManagedOperation;
import org.eclipse.jetty.util.annotation.Name;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

@ManagedObject("JMX message logger")
public class JmxMessageLog extends AbstractMessageLog implements NotificationEmitter
{
	private static final Logger LOG = Log.getLogger(JmxMessageLog.class);
	
	private static final int DEFAULT_MAX_MESSAGES = 100;
	
	private MessageInfo[] _messages;
	private int _maxMessages = DEFAULT_MAX_MESSAGES;
	private int _cursor;
	private long _messageId = 0;
	
	private Map<String, String> _alias = new HashMap<String, String>();
	
	private List<ListenerInfo> _listeners = new ArrayList<JmxMessageLog.ListenerInfo>();
	
	@ManagedAttribute
	public int getMaxMessages()
	{
		return _maxMessages;
	}

	public void setMaxMessages(int maxMessages)
	{
		if (maxMessages <= 0)
			throw new IllegalArgumentException("Max message must be greater than 0");
		synchronized (this)
		{
			if (isRunning() && maxMessages != _maxMessages)
			{
				MessageInfo[] messages = new MessageInfo[maxMessages];
				ListIterator<MessageInfo> it = iterate(false);
				int index = maxMessages;
				while (it.hasPrevious())
				{
					messages[--index] = it.previous();
					if (index == 0)
						break;
				}	
				_cursor = 0;
				_messages = messages;
			}
			_maxMessages = maxMessages;
		}
	}

	protected void doStart() throws Exception
	{
		_messages = new MessageInfo[_maxMessages];
		_cursor = 0;
		super.doStart();
	}

	protected void doStop() throws Exception
	{
		_messages = null;
		super.doStop();
	}

	public void doLog(SipMessage message, int direction, SipConnection connection)
	{
		if (_messages != null)
		{
			// Log only once message to loopback
			if (direction == OUT
					&& connection.getLocalAddress().equals(connection.getRemoteAddress())
					&& connection.getLocalPort() == connection.getRemotePort())
				return;
				
			MessageInfo messageInfo = new MessageInfo(message, direction, connection);
			synchronized (this)
			{
				_messages[_cursor] = messageInfo;
				_cursor = getNextCursor();
				_messageId++;
			}
			if (!_listeners.isEmpty())
			{
				String infoLine;
				synchronized (this)
				{
					infoLine = generateInfoLine(direction, connection, System.currentTimeMillis());
				}
				CallflowNotification notification = new CallflowNotification(messageInfo, _messageId, infoLine);
				sendNotification(notification);
			}
		}
	}
	
	@ManagedOperation(value="Returns the last SIP messages received", impact="INFO")
	public Object[][] getMessages(@Name("maxMessages") Integer maxMessages) throws Exception
	{
		return getMessages(maxMessages, null);
	}
	
	private ListIterator<MessageInfo> iterate(boolean start)
	{
		return new LogIterator(start);
	}
	
	
	private int getNextCursor()
	{
		return _cursor + 1 == _maxMessages ? 0 : _cursor + 1;
	}
	
	@ManagedOperation(value="Clear all messages in the logger", impact="ACTION")
	public void clear()
	{
		if (_messages == null)
			return;
		
		synchronized (this)
		{
			for (int i = 0; i < _messages.length; i++)
				_messages[i] = null;
			_cursor = 0;
		}
	}
	
	@ManagedOperation(value="Returns the last SIP messages received matching the filter", impact="INFO")
	public Object[][] getMessages(
			@Name(value="maxMessages", description="The maximum number of messages to return") Integer maxMessages, 
			@Name(value="msgFilter", description="a JEXL boolean expression to filter messages to display") String msgFilter) throws Exception
	{
		List<MessageInfo> messages = getMessageList(maxMessages, msgFilter);
		Object[][] tab = new Object[messages.size()][3];
		for (int i = 0; i < tab.length; i++)
		{
			MessageInfo info = (MessageInfo) messages.get(i);
			tab[i][0] = generateInfoLine(info.getDirection(), info.getConnection(), info.getDate());
			tab[i][1] = info.getMessage();
			tab[i][2] = info.getRemote();
		}
		
		return tab;
	}
	
	@SuppressWarnings("unchecked")
	private List<MessageInfo> getMessageList(Integer maxMessages, String msgFilter) throws Exception
	{
		if (_messages == null)
			return null;
		
		synchronized (this)
		{
			JexlContext jc = JexlHelper.createContext();
			Expression msgExpression = null;
			if (msgFilter != null && !msgFilter.trim().equals(""))
			{
				LOG.debug("Get messages with filter: " + msgFilter);
				msgExpression = ExpressionFactory.createExpression("log." + msgFilter);
			}
		
			List<MessageInfo> result = new ArrayList<MessageInfo>();
			ListIterator<MessageInfo> it = iterate(false);
			
			int i = 0;
			while (it.hasPrevious() && i < maxMessages)
			{
				MessageInfo info = it.previous();
				jc.getVars().put("log", info);
				jc.getVars().put("message", info.getMessage());
			
				if (msgExpression == null || ((Boolean) msgExpression.evaluate(jc)).booleanValue())
				{
					result.add(0, info);
					i++;
				}
			}
			return result;
		}
	}
	
	public byte[] generateGraph(Integer maxMessages, String msgFilter, String xslUri) throws Exception
	{
		return generateGraph(getMessageList(maxMessages, msgFilter), xslUri, false);
	}
	
	@ManagedOperation(value="Returns the data for generating SVG graph received matching the filter", impact="INFO")
	public byte[] generateGraph(
			@Name(value="maxMessages", description="The maximum number of messages to return") Integer maxMessages, 
			@Name(value="msgFilter", description="a JEXL boolean expression to filter messages to display") String msgFilter,
			@Name(value="xslUri", description="URI pointing to the XSL document") String xslUri, 
			@Name(value="includeMsg", description="Include the SIP message") Boolean includeMsg) throws Exception
	{
		return generateGraph(getMessageList(maxMessages, msgFilter), xslUri, includeMsg);
	}
	
	protected byte[] generateGraph(List<MessageInfo> messages, String xslUri, boolean includeMsg) throws IOException
	{
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		OutputStreamWriter out = new OutputStreamWriter(os);
		out.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
		if (xslUri != null)
			out.write("<?xml-stylesheet href=\"" + xslUri + "\" type=\"text/xsl\"?>\n");
		out.write("<data>\n");
		out.write("\t<hosts>\n");
		Map<String, Integer> hostsMap = new HashMap<String, Integer>();
		Iterator<MessageInfo> it = messages.iterator();
		int indexLocal = -1;
		int index = 1;
		while (it.hasNext())
		{
			MessageInfo info = it.next();
			if (!hostsMap.containsKey(info.getLocalKey()))
			{
				if (indexLocal == -1)
				{
					out.write("\t\t<host>");
					String alias = _alias.get(info.getLocalKey());
					out.write(alias == null ? "Cipango" : alias);
					out.write("</host>\n");
					indexLocal = index++;
				}
				hostsMap.put(info.getLocalKey(), indexLocal);
			}

			if (!hostsMap.containsKey(info.getRemoteKey()))
			{
				out.write("\t\t<host>");
				String alias = _alias.get(info.getRemoteKey());
				out.write(alias == null ? info.getRemote() : alias);
				out.write("</host>\n");
				hostsMap.put(info.getRemoteKey(), index++);
			}
		}
		out.write("\t</hosts>\n");
		
		it = messages.iterator();
		out.write("\t<messages>\n");
		while (it.hasNext())
		{
			MessageInfo info = it.next();
			out.write("\t\t<message from=\"");
			if (info.getDirection() == IN)
			{
				out.write(String.valueOf(hostsMap.get(info.getRemoteKey())));
				out.write("\" to=\"");
				out.write(String.valueOf(hostsMap.get(info.getLocalKey())));
				out.write("\">\n");
			}
			else
			{
				out.write(String.valueOf(hostsMap.get(info.getLocalKey())));
				out.write("\" to=\"");
				out.write(String.valueOf(hostsMap.get(info.getRemoteKey())));
				out.write("\">\n");
			}
			out.write("\t\t\t<name>");
			out.write(info.getShortName());
			out.write("</name>\n");
			out.write("\t\t\t<date>");
			out.write(info.getFormatedDate());
			out.write("</date>\n");
			if (includeMsg)
			{
				StringBuilder sb = new StringBuilder(info.getMessage().toString());
				replaceAll(sb, "<", "&lt;");
				replaceAll(sb, ">", "&gt;");
				String msg = sb.toString();
				int nbLines = 1;
				for (int i = 0; i < msg.length(); i++)
					if (msg.charAt(i) == '\n')
						nbLines++;
				out.write("\t\t\t<content nbLines=\"" + nbLines + "\">");
				out.write(msg);
				out.write("</content>\n");
			}
			out.write("\t\t</message>\n");
		}
		out.write("\t</messages>\n");
		out.write("</data>\n");
		out.flush();
		return os.toByteArray();
	}
	
	protected void replaceAll(StringBuilder sb, String toFind, Object toSet)
	{
		int index = 0;
		while ((index = sb.indexOf(toFind)) != -1)
			sb.replace(index, index + toFind.length(), toSet.toString());
	}
	
	public void addAlias(String host, int port, String name) throws UnknownHostException
	{
		InetAddress addr = InetAddress.getByName(host);
		_alias.put(addr.getHostAddress() + ":" + port, name);
	}
			
	private class LogIterator implements ListIterator<MessageInfo>
	{
		private int _itCursor;
		private boolean _start = true;
		
		public LogIterator(boolean start)
		{
			if (start)
				_itCursor = _messages[getNextCursor()] == null ? 0 : getNextCursor();
			else
				_itCursor = _cursor;
		}
		
		private int getNextItCursor()
		{
			return _itCursor + 1 == _maxMessages ? 0 : _itCursor + 1;
		}
		private int getPreviousItCursor()
		{
			return _itCursor == 0 ? _maxMessages - 1 : _itCursor - 1;
		}
		
		public boolean hasNext()
		{
			return _itCursor != _cursor && _messages[getNextItCursor()] != null;
		}

		public MessageInfo next()
		{
			if (!hasNext())
				throw new NoSuchElementException("No next");
			_itCursor = getNextItCursor();
			return _messages[_itCursor];
		}

		public void remove()
		{
			throw new UnsupportedOperationException("Read-only");
		}

		public void add(MessageInfo arg0)
		{
			throw new UnsupportedOperationException("Read-only");
		}

		public boolean hasPrevious()
		{
			return (_start || _itCursor != _cursor) && _messages[getPreviousItCursor()] != null;
		}

		public int nextIndex()
		{
			return 0;
		}

		public MessageInfo previous()
		{
			if (!hasPrevious())
				throw new NoSuchElementException("No previous");
			_start = false;
			_itCursor = getPreviousItCursor();
			return _messages[_itCursor];
		}

		public int previousIndex()
		{
			return 0;
		}

		public void set(MessageInfo arg0)
		{
			throw new UnsupportedOperationException("Read-only");
		}
	}

	public void addNotificationListener(NotificationListener listener, NotificationFilter filter,
			Object handback) throws IllegalArgumentException
	{
		synchronized (_listeners)
		{
			_listeners.add(new ListenerInfo(listener, filter, handback));
		}
	}

	public void removeNotificationListener(NotificationListener listener) throws ListenerNotFoundException
	{
		synchronized (_listeners)
		{
			Iterator<ListenerInfo> it = _listeners.iterator();
			while (it.hasNext())
			{
				JmxMessageLog.ListenerInfo info = it.next();
				if (info.listener.equals(listener))
				{
					it.remove();
					return;
				}
			}
		}
		throw new ListenerNotFoundException();
	}
	
	public void removeNotificationListener(NotificationListener listener, NotificationFilter filter,
			Object handback) throws ListenerNotFoundException
	{
		synchronized (_listeners)
		{
			Iterator<ListenerInfo> it = _listeners.iterator();
			while (it.hasNext())
			{
				JmxMessageLog.ListenerInfo info = it.next();
				if (info.listener.equals(listener) && info.filter == filter && info.handback == handback)
				{
					it.remove();
					return;
				}
			}
		}
		throw new ListenerNotFoundException();
	}

	public MBeanNotificationInfo[] getNotificationInfo()
	{
		String[] types = new String[] { "SIP" };
		String name = MBeanNotificationInfo.class.getName();
		String description = "SIP message notification";
		MBeanNotificationInfo info = new MBeanNotificationInfo(types, name, description);

		return new MBeanNotificationInfo[] { info };
	}

	private void sendNotification(Notification notification)
	{
		if (notification == null || _listeners.isEmpty())
			return;
		
		synchronized (_listeners)
		{
			for (ListenerInfo info : _listeners)
			{
				if (info.filter == null || info.filter.isNotificationEnabled(notification))
				{
					try
					{
						info.listener.handleNotification(notification, info.handback);
					}
					catch (Exception e)
					{
						LOG.warn(e);
					}
				}
			}
		}
	}
	
	
	private class ListenerInfo
	{
		public NotificationListener listener;
		NotificationFilter filter;
		Object handback;

		public ListenerInfo(NotificationListener listener, NotificationFilter filter, Object handback)
		{
			this.listener = listener;
			this.filter = filter;
			this.handback = handback;
		}
	}

}




