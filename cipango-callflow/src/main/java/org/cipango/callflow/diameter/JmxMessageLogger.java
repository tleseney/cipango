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
package org.cipango.callflow.diameter;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import org.apache.commons.jexl.Expression;
import org.apache.commons.jexl.ExpressionFactory;
import org.apache.commons.jexl.JexlContext;
import org.apache.commons.jexl.JexlHelper;
import org.cipango.callflow.JmxMessageLog;
import org.cipango.callflow.diameter.DiameterMessageFormator.Output;
import org.cipango.diameter.log.DiameterMessageListener;
import org.cipango.diameter.node.DiameterConnection;
import org.cipango.diameter.node.DiameterMessage;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.annotation.ManagedOperation;
import org.eclipse.jetty.util.annotation.Name;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

@ManagedObject("Console logger")
public class JmxMessageLogger extends AbstractLifeCycle implements DiameterMessageListener
{
	private static final Logger LOG = Log.getLogger(JmxMessageLog.class);
	
	private static final int DEFAULT_MAX_MESSAGES = 50;
	public enum Direction { IN, OUT };
	
	private MessageInfo[] _messages;
	private int _maxMessages = DEFAULT_MAX_MESSAGES;
	private int _cursor;
	
	@ManagedAttribute("Maximum number of messages saved.")
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
	

	public void messageReceived(DiameterMessage message, DiameterConnection connection)
	{
		doLog(message, Direction.IN, connection);
	}

	public void messageSent(DiameterMessage message, DiameterConnection connection)
	{
		doLog(message, Direction.OUT, connection);
	}

	public void doLog(DiameterMessage message, Direction direction, DiameterConnection connection)
	{
		if (_messages != null)
		{				
			synchronized (this)
			{
				_messages[_cursor] = new MessageInfo(message, direction, connection);
				_cursor = getNextCursor();
			}
			
		}
	}
		
	public Object[][] getMessages(Integer maxMessages) throws Exception
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
	
	@ManagedOperation(value="Returns the last Diameter messages received matching the filter", impact="INFO")
	public Object[][] getMessages(
			@Name(value="maxMessages", description="The maximum number of messages to return") Integer maxMessages, 
			@Name(value="msgFilter", description="a JEXL boolean expression to filter messages to display") String msgFilter) throws Exception
	{
		List<MessageInfo> messages = getMessageList(maxMessages, msgFilter);
		Object[][] tab = new Object[messages.size()][5];
		for (int i = 0; i < tab.length; i++)
		{
			MessageInfo info = (MessageInfo) messages.get(i);
			tab[i][0] = generateInfoLine(info);
			Output output = DiameterMessageFormator.getPretty().newOutput();
			output.add(info.getMessage());
			tab[i][1] = output.toString();
			tab[i][2] = info.getRemote();
		}
		
		return tab;
	}
	
	public String generateInfoLine(MessageInfo messageInfo)
	{
		StringBuilder sb = new StringBuilder();
		sb.append(messageInfo.getFormatedDate());
        if (messageInfo.getDirection() == Direction.IN)
        	sb.append(" IN  ");
		else
			sb.append(" OUT ");
        sb.append(messageInfo.getConnection().getLocalAddress());
        sb.append((messageInfo.getDirection() == Direction.IN ? " < " : " > "));
        sb.append(messageInfo.getConnection().getRemoteAddress());
        sb.append('\n');
        return sb.toString();
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

}




