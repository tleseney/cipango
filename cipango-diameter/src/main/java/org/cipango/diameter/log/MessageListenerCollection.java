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
import org.eclipse.jetty.util.ArrayUtil;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.component.ContainerLifeCycle;

@ManagedObject
public class MessageListenerCollection extends ContainerLifeCycle implements DiameterMessageListener
{
	
	private DiameterMessageListener[] _listeners;
	
	public void messageReceived(DiameterMessage message, DiameterConnection connection)
	{
		for (int i = 0; _listeners != null && i < _listeners.length; i++)
			_listeners[i].messageReceived(message, connection);
	}

	public void messageSent(DiameterMessage message, DiameterConnection connection)
	{
		for (int i = 0; _listeners != null && i < _listeners.length; i++)
			_listeners[i].messageSent(message, connection);
	}

	@ManagedAttribute(readonly=true)
	public DiameterMessageListener[] getMessageListeners()
	{
		return _listeners;
	}

	public void setMessageListeners(DiameterMessageListener[] loggers)
	{
		updateBean(_listeners, loggers);
		_listeners = loggers;
	}
	
	public void addMessageListener(DiameterMessageListener accessLog)
    {
        setMessageListeners((DiameterMessageListener[])ArrayUtil.addToArray(getMessageListeners(), accessLog, DiameterMessageListener.class));
    }
    
    public void removeMessageListener(DiameterMessageListener accessLog)
    {
    	DiameterMessageListener[] loggers = getMessageListeners();
        
        if (loggers!=null && loggers.length>0 )
            setMessageListeners((DiameterMessageListener[])ArrayUtil.removeFromArray(loggers, accessLog));
    }

}
