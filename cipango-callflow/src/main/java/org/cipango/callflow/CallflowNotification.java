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

import javax.management.Notification;

public class CallflowNotification extends Notification
{
	private transient MessageInfo _messageInfo;
	
	public CallflowNotification(MessageInfo messageInfo, long sequenceNumber, String infoLine)
	{
		super("SIP", infoLine, sequenceNumber);
		_messageInfo = messageInfo;
	}

	@Override
	public Object getUserData()
	{
		Object[] userData = new Object[3];
		userData[0] = getSource();
		if (_messageInfo != null)
		{
			userData[1] = _messageInfo.getMessage();
			userData[2] = _messageInfo.getRemote();
		}
		return userData;
	}

	public MessageInfo getMessageInfo()
	{
		return _messageInfo;
	}
	
	
}
