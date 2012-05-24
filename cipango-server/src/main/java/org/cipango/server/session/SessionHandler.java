package org.cipango.server.session;

import java.io.IOException;

import javax.servlet.ServletException;

import org.cipango.server.SipHandler;
import org.cipango.server.SipMessage;
import org.cipango.server.SipRequest;
import org.cipango.server.handler.AbstractSipHandler;
import org.cipango.server.servlet.SipServletHandler;

public class SessionHandler extends AbstractSipHandler
{
	private SessionManager _sessionManager;
	private SipHandler _handler;
	
	public SessionHandler()
	{
		_sessionManager = new SessionManager();
	}
	
	public void setHandler(SipHandler handler)
	{
		_handler = handler;
	}
	
	@Override
	protected void doStart() throws Exception
	{
		_sessionManager.start();
		_handler.start();
		
		super.doStart();
	}
	
	public void handle(SipMessage message) throws IOException, ServletException 
	{
		if (message.isRequest())
		{
			SipRequest request = (SipRequest) message;
			
			Session session = null;
			
			if (request.isInitial())
			{
				ApplicationSession appSession = _sessionManager.createApplicationSession();
				session = appSession.createSession(request);
			}
			else
			{
				String appId = request.getParameter("appid");
				ApplicationSession appSession = _sessionManager.getApplicationSession(appId);
				session = appSession.getSession(request);
			}
			request.setSession(session);
			
			
			System.out.println("handling request " + request.getMethod() + " for session: " + session);
			
			_handler.handle(request);
		}
		// TODO Auto-generated method stub
		
	}
	
	protected String getApplicationId(String s)
	{
		return s.substring(s.lastIndexOf('-'));
	}
	
}
