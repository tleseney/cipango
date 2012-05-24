package org.cipango.server.session;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.cipango.util.StringUtil;
import org.eclipse.jetty.util.component.AbstractLifeCycle;

public class SessionManager extends AbstractLifeCycle
{
	private Random _random = new Random();
	private ConcurrentHashMap<String, ApplicationSession> _appSessions = new ConcurrentHashMap<String, ApplicationSession>();
	
	@Override
	protected void doStart() throws Exception
	{
		super.doStart();
	}
	
	public ApplicationSession createApplicationSession()
	{
		ApplicationSession appSession = new ApplicationSession(newApplicationSessionId());
		_appSessions.put(appSession.getId(), appSession);
		return appSession;
	}
	
	public ApplicationSession getApplicationSession(String id)
	{
		return _appSessions.get(id);
	}
	
	protected String newApplicationSessionId()
	{
		long r = _random.nextInt();
		if (r<0)
			r = -r;
		return StringUtil.toBase62String2(r);
	}
}
