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

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.sip.SipServletRequest;

import org.cipango.server.SipConnection;
import org.cipango.server.SipMessage;
import org.cipango.server.SipRequest;
import org.cipango.server.SipResponse;
import org.cipango.server.log.AbstractMessageLog;

public class MessageInfo
{
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private SipMessage _sipMessage;
	private int _direction;
	private SipConnection _connection;
	private long _date;
	
	public MessageInfo(SipMessage message, int direction, SipConnection connection)
	{
		if (direction == AbstractMessageLog.IN)
			_sipMessage = clone(message);
		else
			_sipMessage = message;
		_direction = direction;
		_connection = connection;
		_date = System.currentTimeMillis();
		
	}
	
	private SipMessage clone(SipMessage toClone)
	{
		if (toClone instanceof SipRequest)
			return new SipRequest((SipRequest) toClone);
		return new SipResponse((SipResponse) toClone);
	}
	
	public long getDate()
	{
		return _date;
	}
	public String getFormatedDate()
	{
		return DATE_FORMAT.format(new Date(_date));
	}
	public SipMessage getMessage()
	{
		return _sipMessage;
	}

	public int getDirection()
	{
		return _direction;
	}

	public SipConnection getConnection()
	{
		return _connection;
	}
	public String getLocal()
	{
		return _connection.getLocalAddress() + ":" + _connection.getLocalPort();
	}
	
	public String getLocalKey()
	{
		return _connection.getLocalAddress().getHostAddress() + ":" + _connection.getLocalPort();
	}
	
	public String getRemote()
	{
		return _connection.getRemoteAddress() + ":" + _connection.getRemotePort();
	}
	
	public String getRemoteKey()
	{
		return _connection.getRemoteAddress().getHostAddress() + ":" + _connection.getRemotePort();
	}
	
	public String getShortName()
	{
		if (_sipMessage.isRequest())
			return _sipMessage.getMethod() + " " + ((SipServletRequest) _sipMessage).getRequestURI();
		else
		{
			SipResponse response = (SipResponse) _sipMessage;
			return response.getStatus() + " " + response.getReasonPhrase();
		}
	}
	/**
	 * Time since the message has been received or sent in seconds
	 */
	public long getRelativeTime()
	{
		return (System.currentTimeMillis() - _date)/1000;
	}
}
