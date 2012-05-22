package org.cipango.server.session;

import java.io.IOException;

import javax.servlet.ServletException;

import org.cipango.server.SipMessage;
import org.cipango.server.SipRequest;
import org.cipango.server.handler.AbstractSipHandler;

public class SessionHandler extends AbstractSipHandler
{
	private SessionManager _sessionManager;
	
	public void handle(SipMessage message) throws IOException, ServletException 
	{
		if (message.isRequest())
		{
			SipRequest request = (SipRequest) message;
			
			if (request.isInitial())
			{
				
			}
		}
		// TODO Auto-generated method stub
		
	}
	
}
