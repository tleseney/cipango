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
package org.cipango.diameter.log;

import org.cipango.diameter.node.DiameterConnection;
import org.cipango.diameter.node.DiameterMessage;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class BasicMessageLog implements DiameterMessageListener
{
	private static final Logger LOG = Log.getLogger(BasicMessageLog.class);
	
	enum Direction { IN, OUT }
	
	protected void doLog(Direction direction, DiameterMessage message, DiameterConnection connection)
	{
		StringBuilder sb = new StringBuilder();
		sb.append(connection.getLocalAddress());
		sb.append((direction == Direction.IN ? " < " : " > "));
		sb.append(connection.getRemoteAddress());
		sb.append(' ');
		sb.append(message);
		
		LOG.info(sb.toString());
	}
	
	public void messageReceived(DiameterMessage message, DiameterConnection connection) 
	{
		doLog(Direction.IN, message, connection);
	}

	public void messageSent(DiameterMessage message, DiameterConnection connection) 
	{
		doLog(Direction.OUT, message, connection);
	}
}
