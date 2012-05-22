package org.cipango.server.processor;

import javax.servlet.sip.Address;

import org.cipango.server.SipMessage;
import org.cipango.server.SipProcessor;
import org.cipango.server.SipRequest;
import org.cipango.sip.Via;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

/**
 * Performs transport related operations such as Via, Route handling.
 */
public class TransportProcessor extends SipProcessorWrapper
{
	private Logger LOG = Log.getLogger(TransportProcessor.class);
	
	public TransportProcessor(SipProcessor processor)
	{
		super(processor);
	}
	
	protected Address popLocalRoute(SipRequest request)
	{
		Address route = request.getTopRoute();
		
		if (route != null && getServer().isLocalURI(route.getURI()))
		{
			request.removeTopRoute();
			// TODO DDR
		}
		
		return route;
	}
	
	public void doProcess(SipMessage message) throws Exception
	{
		LOG.debug("handling message " + message.getMethod());
		if (message.isRequest())
		{
			SipRequest request = (SipRequest) message;
			// via
			
			Via via = message.getTopVia();
			String remoteAddress = message.getRemoteAddr();
			
			if (!via.getHost().equals(remoteAddress))
				via.setReceived(remoteAddress);
			
			if (via.hasRPort())
				via.setRPort(message.getRemotePort());
			
			// route
			
			Address route = popLocalRoute(request);
			
			if (route != null)
				request.setPoppedRoute(route);
			
			
		}
		super.doProcess(message);
	}
}
