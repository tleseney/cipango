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
package org.cipango.callflow.jmx;

import javax.management.ListenerNotFoundException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.NotificationBroadcaster;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;

import org.eclipse.jetty.jmx.ObjectMBean;

public class JmxMessageLogMBean extends ObjectMBean implements NotificationBroadcaster
{

	public JmxMessageLogMBean(Object managedObject)
	{
		super(managedObject);
	}
	
	private NotificationBroadcaster getNb()
	{
		return (NotificationBroadcaster) getManagedObject();
	}

	@Override
	public MBeanInfo getMBeanInfo()
	{
		MBeanInfo mBeanInfo = super.getMBeanInfo();
		MBeanNotificationInfo[] notifications = getNb().getNotificationInfo() ;
		
		return new MBeanInfo(mBeanInfo.getClassName(), 
				mBeanInfo.getDescription(), mBeanInfo.getAttributes(), 
				mBeanInfo.getConstructors(), mBeanInfo.getOperations(), 
				notifications);
	}

	public void addNotificationListener(NotificationListener listener, NotificationFilter filter,
			Object handback) throws IllegalArgumentException
	{
		getNb().addNotificationListener(listener, filter, handback);
	}

	public void removeNotificationListener(NotificationListener listener) throws ListenerNotFoundException
	{
		getNb().removeNotificationListener(listener);
	}

	public MBeanNotificationInfo[] getNotificationInfo()
	{
		return getNb().getNotificationInfo();
	}

}
