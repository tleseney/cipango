package org.cipango.server.sipapp;

import java.io.IOException;

import javax.servlet.ServletException;

import org.cipango.server.SessionManagerTest;
import org.cipango.server.SipMessage;
import org.cipango.server.handler.AbstractSipHandler;
import org.cipango.server.session.SessionManager;

public class SipAppContext extends AbstractSipHandler 
{
	private SessionHandler _sessionHandler;
	
	public void handle(SipMessage message) throws IOException, ServletException 
	{
		// TODO Auto-generated method stub
		
	}
}
