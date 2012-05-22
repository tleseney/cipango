package org.cipango.server.processor;

import javax.servlet.sip.SipServletResponse;

import org.cipango.server.SipMessage;
import org.cipango.server.SipRequest;
import org.cipango.server.session.ApplicationSession;
import org.cipango.server.session.Session;

public class SipSessionProcessor extends SipProcessorWrapper
{
	@Override
	public void doProcess(SipMessage message) throws Exception 
	{
		if (!message.isRequest())
			throw new IllegalArgumentException("requests only");
		
		SipRequest request = (SipRequest) message;
		
		Session session = null;
		
		if (request.isInitial())
		{
			ApplicationSession applicationSession = request.getCallSession().createApplicationSession();
			session = applicationSession.createSession();
		}
		else
		{
			session = null;
		}
		
		try
		{
			session.handleRequest(request);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			if (!request.isAck() && !request.isCommitted())
			{
				request.createResponse(SipServletResponse.SC_SERVER_INTERNAL_ERROR).send();
			}
		}
	}	
}
