// ========================================================================
// Copyright 2008-2012 NEXCOM Systems
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

package org.cipango.server.session.scoped;

import java.io.Serializable;

import javax.servlet.sip.ServletTimer;
import javax.servlet.sip.SipApplicationSession;

import org.cipango.server.session.ApplicationSession;
import org.cipango.server.session.SessionManager.ApplicationSessionScope;

public class ScopedTimer extends ScopedObject implements ServletTimer
{
	private ServletTimer _timer;
	private ApplicationSession _appSession;

	public ScopedTimer(ApplicationSession session, long delay, boolean isPersistent, Serializable info)
	{
		_appSession = session;
		ApplicationSessionScope scope = openScope();
		try
		{
			_timer = new ApplicationSession.Timer(session, delay, isPersistent, info);
		}
		finally
		{
			scope.close();
		}
	}

	public ScopedTimer(ApplicationSession session, long delay, long period, boolean fixedDelay,
			boolean isPersistent, Serializable info)
	{
		_appSession = session;
		ApplicationSessionScope scope = openScope();
		try
		{
			_timer = new ApplicationSession.Timer(session, delay, period, fixedDelay, isPersistent, info);
		}
		finally
		{
			scope.close();
		}
	}

	public ScopedTimer(ServletTimer timer)
	{
		_timer = timer;
		_appSession = (ApplicationSession) timer.getApplicationSession();
	}

	public void cancel()
	{
		ApplicationSessionScope scope = openScope();
		try
		{
			_timer.cancel();
		}
		finally
		{
			scope.close();
		}
	}

	public SipApplicationSession getApplicationSession()
	{
		return new ScopedAppSession((ApplicationSession) _timer.getApplicationSession());
	}

	public String getId()
	{
		return _timer.getId();
	}

	public Serializable getInfo()
	{
		return _timer.getInfo();
	}

	public long getTimeRemaining()
	{
		return _timer.getTimeRemaining();
	}

	public long scheduledExecutionTime()
	{
		return _timer.scheduledExecutionTime();
	}

	@Override
	public String toString()
	{
		return _timer.toString();
	}

	@Override
	public boolean equals(Object o)
	{
		return _timer.equals(o);
	}

	@Override
	public int hashCode()
	{
		return _timer.hashCode();
	}

	@Override
	protected ApplicationSession getAppSession()
	{
		return _appSession;
	}
}
