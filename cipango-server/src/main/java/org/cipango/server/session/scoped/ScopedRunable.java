package org.cipango.server.session.scoped;

import org.cipango.server.session.ApplicationSession;
import org.cipango.server.session.SessionManager.ApplicationSessionScope;

public abstract class ScopedRunable extends ScopedObject implements Runnable
{
	protected ApplicationSession _session;
	
	public ScopedRunable(ApplicationSession applicationSession)
	{
		_session = applicationSession;
	}
	
	@Override
	public final void run()
	{
		ApplicationSessionScope scope = openScope();
		try
		{
			doRun();
		}
		finally
		{
			scope.close();
		}
	}
	
	protected abstract void doRun();

	@Override
	public ApplicationSession getAppSession()
	{
		return _session;
	}

}
