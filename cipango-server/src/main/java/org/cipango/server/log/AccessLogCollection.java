// ========================================================================
// Copyright 2010-2012 NEXCOM Systems
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
package org.cipango.server.log;

import org.cipango.server.SipConnection;
import org.cipango.server.SipMessage;
import org.eclipse.jetty.util.ArrayUtil;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.component.ContainerLifeCycle;

@ManagedObject("Access log collection")
public class AccessLogCollection extends ContainerLifeCycle implements AccessLog
{
	private AccessLog[] _loggers;

	public void messageReceived(SipMessage message, SipConnection connection)
	{
		for (int i = 0; _loggers != null && i < _loggers.length; i++)
			_loggers[i].messageReceived(message, connection);
	}

	public void messageSent(SipMessage message, SipConnection connection)
	{
		for (int i = 0; _loggers != null && i < _loggers.length; i++)
			_loggers[i].messageSent(message, connection);
	}

	@ManagedAttribute(value="logggers", readonly=true)
	public AccessLog[] getLoggers()
	{
		return _loggers;
	}

	public void setLoggers(AccessLog[] loggers)
	{
		updateBeans(_loggers, loggers);
		_loggers = loggers;
	}
	
	public void addLogger(AccessLog accessLog)
    {
        setLoggers(ArrayUtil.addToArray(getLoggers(), accessLog, AccessLog.class));
    }
    
    public void removeLogger(AccessLog accessLog)
    {
    	AccessLog[] loggers = getLoggers();
        
        if (loggers!=null && loggers.length>0 )
            setLoggers(ArrayUtil.removeFromArray(loggers, accessLog));
    }
}
