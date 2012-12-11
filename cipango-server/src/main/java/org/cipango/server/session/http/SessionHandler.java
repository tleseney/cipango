package org.cipango.server.session.http;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.cipango.server.session.http.ConvergedSessionManager.Session;
import org.eclipse.jetty.server.Request;

public class SessionHandler extends org.eclipse.jetty.server.session.SessionHandler
{
	
	public SessionHandler()
	{
		super(new ConvergedSessionManager());
	}
	
	public SessionHandler(ConvergedSessionManager sessionManager)
	{
		super(sessionManager);
	}

	@Override
	protected void checkRequestedSessionId(Request baseRequest, HttpServletRequest request)
	{
		super.checkRequestedSessionId(baseRequest, request);
		
		HttpSession session = baseRequest.getSession(false);
		if (session != null && session instanceof Session)
		{
			((Session) session).updateSession(request);
		}
	}


}
