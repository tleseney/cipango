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

import java.text.SimpleDateFormat;
import java.util.Date;

import org.cipango.callflow.diameter.JmxMessageLogger.Direction;
import org.cipango.diameter.node.DiameterConnection;
import org.cipango.diameter.node.DiameterMessage;

public class MessageInfo
{
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private DiameterMessage _message;
	private Direction _direction;
	private DiameterConnection _connection;
	
	private long _date;
	
	public MessageInfo(DiameterMessage message, Direction direction, DiameterConnection connection)
	{
		_message = message;
		_direction = direction;
		_connection = connection;
		_date = System.currentTimeMillis();
		
	}
	
	public long getDate()
	{
		return _date;
	}
	public String getFormatedDate()
	{
		return DATE_FORMAT.format(new Date(_date));
	}
	public DiameterMessage getMessage()
	{
		return _message;
	}

	public Direction getDirection()
	{
		return _direction;
	}

	public DiameterConnection getConnection()
	{
		return _connection;
	}
	public String getLocal()
	{
		return _connection.getLocalAddress().toString();
	}
	public String getRemote()
	{
		return _connection.getRemoteAddress().toString();
	}

	/**
	 * Time since the message has been received or sent in seconds
	 */
	public long getRelativeTime()
	{
		return (System.currentTimeMillis() - _date)/1000;
	}
}
