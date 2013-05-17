// ========================================================================
// Copyright 2007-2009 NEXCOM Systems
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
package org.cipango.example;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.servlet.sip.B2buaHelper;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.UAMode;
import javax.servlet.sip.SipSession.State;

public class Call
{

	
	private SipSession _leg1;
	private SipSession _leg2;
	private B2buaHelper _b2bHelper;

	public Call(SipApplicationSession session, B2buaHelper b2bHelper)
	{
		_b2bHelper = b2bHelper;
		Iterator<?> it = session.getSessions("sip");
		while (it.hasNext())
		{
			SipSession sipSession = (SipSession) it.next();
			if (_leg1 == null)
				_leg1 = sipSession;
			else if (_leg1.getCreationTime() > sipSession.getCreationTime())
			{
				_leg1 = sipSession;
				_leg2 = _leg1;
			}
			else
				_leg2 = sipSession;
		}
	}
	
	public void hangup() throws IOException
	{
		hangup(_leg1);
		if (_leg2 != null)
			hangup(_leg2);
		_leg1.getApplicationSession().invalidate();
	}
	
	public void hangup(SipSession session) throws IOException
	{
		if (session.getState() == State.EARLY || session.getState() == State.INITIAL)
		{
			
			List<SipServletMessage> list = _b2bHelper.getPendingMessages(session, UAMode.UAC);
			if (list.size() == 1)
			{
				SipServletRequest invite = (SipServletRequest) list.get(0);
				invite.createCancel().send();
			}
		}
		else if (session.getState() == State.CONFIRMED)
		{
			session.createRequest("BYE").send();
		}
	}

	
	public String getFrom()
	{
		return _leg1.getLocalParty().getURI().toString();
	}
	
	public String getTo()
	{
		return _leg1.getRemoteParty().getURI().toString();
	}
	
	public String getState() 
	{
		if (!_leg1.isValid())
			return State.TERMINATED.toString();
		
		if (_leg2 == null)
			return "Leg1: " + _leg1.getState().toString();
		else if (_leg2.getState() == State.CONFIRMED)
			return "In call";
		else if (_leg2.getState() != State.TERMINATED)
			return "Leg2: " + _leg2.getState().toString();
		else
			return _leg1.getState().toString() + "-" + _leg2.getState().toString();
	}
	
	public String getId()
	{
		return _leg1.getApplicationSession().getId();
	}
	
}
