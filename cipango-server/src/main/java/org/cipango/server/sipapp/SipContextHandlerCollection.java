package org.cipango.server.sipapp;

import java.io.IOException;

import javax.servlet.ServletException;

import org.cipango.server.SipMessage;
import org.cipango.server.SipRequest;
import org.cipango.server.handler.AbstractSipHandler;
import org.eclipse.jetty.util.LazyList;

public class SipContextHandlerCollection extends AbstractSipHandler
{
	private SipAppContext[] _sipContexts;
	
	@Override
	protected void doStart()
	{
		_sipContexts = LazyList.addToArray(_sipContexts, new SipAppContext(), SipAppContext.class);
	}
	
	public void handle(SipMessage message) throws IOException, ServletException 
	{
		if (message.isRequest())
		{
			SipRequest request = (SipRequest) message;
			if (request.isInitial())
			{
				_sipContexts[0].handle(message);
			}
			else
			{
				String appId = request.getParameter("appid");
				if (appId != null)
				{
					getContext(appId).handle(message);
				}
			}
		}
	}
	
	protected SipAppContext getContext(String appId)
	{
		return _sipContexts[0];
	}
}
