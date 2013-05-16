// ========================================================================
// Copyright 2011 NEXCOM Systems
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

import javax.management.Notification;
import javax.management.NotificationFilter;

import org.apache.commons.jexl.Expression;
import org.apache.commons.jexl.ExpressionFactory;
import org.apache.commons.jexl.JexlContext;
import org.apache.commons.jexl.JexlHelper;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class CallflowNotificationFilter implements NotificationFilter
{
	private static final Logger LOG = Log.getLogger(CallflowNotificationFilter.class);
	
	private String _filter;
	
	public CallflowNotificationFilter(String filter)
	{
		_filter = filter;
	}
	

	@SuppressWarnings("unchecked")
	public synchronized boolean isNotificationEnabled(Notification notification)
	{
		if (_filter == null || "".equals(_filter))
			return true;
		
		if (notification instanceof CallflowNotification)
		{
			try
			{
				CallflowNotification notif = (CallflowNotification) notification;
				JexlContext jc = JexlHelper.createContext();
				Expression msgExpression = null;
				msgExpression = ExpressionFactory.createExpression("log." + _filter);

				jc.getVars().put("log", notif.getMessageInfo());
				jc.getVars().put("message", notif.getMessageInfo().getMessage());

				return ((Boolean) msgExpression.evaluate(jc));
			}
			catch (Exception e)
			{
				LOG.ignore(e);
			}
		}
		return false;
	}


	public String getFilter()
	{
		return _filter;
	}


	public synchronized void setFilter(String filter)
	{
		_filter = filter;
	}

}
