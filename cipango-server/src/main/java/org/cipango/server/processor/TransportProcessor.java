package org.cipango.server.processor;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Iterator;

import javax.servlet.sip.Address;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.URI;

import org.cipango.server.SipConnection;
import org.cipango.server.SipConnector;
import org.cipango.server.SipMessage;
import org.cipango.server.SipProcessor;
import org.cipango.server.SipRequest;
import org.cipango.server.SipResponse;
import org.cipango.server.Transport;
import org.cipango.sip.SipHeader;
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
	
    
	public boolean preValidateMessage(SipMessage message)
	{
		boolean valid = true;
		try 
		{
			if (!isUnique(SipHeader.FROM, message)
					|| !isUnique(SipHeader.TO, message)
					|| !isUnique(SipHeader.CALL_ID, message)
					|| !isUnique(SipHeader.CSEQ, message))
			{
				valid = false;
			}
			else if (message.getTopVia() == null
					|| message.getCSeq() == null)
			{
				LOG.info("Received bad message: unparsable required headers");
				valid = false;
			}
			message.getAddressHeader("contact");
				
			if (message instanceof SipRequest)
			{
				SipRequest request = (SipRequest) message;
				if (request.getRequestURI() == null)
					valid = false;
				request.getTopRoute();
				if (!request.getCSeq().getMethod().equals(request.getMethod()))
				{
					LOG.info("Received bad request: CSeq method does not match");
					valid = false;
				}
			}
			else
			{
				int status = ((SipResponse) message).getStatus();
				if (status < 100 || status > 699)
				{
					LOG.info("Received bad response: Invalid status code: " + status);
					valid = false;
				}
			}
		}
		catch (Exception e) 
		{
			LOG.info("Received bad message: Some headers are not parsable: {}", e);
			LOG.debug("Received bad message: Some headers are not parsable", e);
			valid = false;
		}
				
		try 
		{
			if (!valid 
					&& message instanceof SipRequest 
					&& !((SipRequest) message).isAck()
					&& message.getTopVia() != null)
			{
				// TODO send response stateless
//	FIXME			SipResponse response = 
//					(SipResponse) ((SipRequest) message).createResponse(SipServletResponse.SC_BAD_REQUEST);
//				sendResponse(response);
			}
		}
		catch (Exception e) 
		{
			LOG.ignore(e);
		}
		
		return valid;
	}
	
	private boolean isUnique(SipHeader header, SipMessage message)
	{
		String name = header.asString();
		Iterator<String> it = message.getFields().getValues(name);
		if (!it.hasNext())
		{
			LOG.info("Received bad message: Missing required header: " + name);
			return false;
		}
		it.next();
		if (it.hasNext())
			LOG.info("Received bad message: Duplicate header: " + name);
		return !it.hasNext();
	}
	
    public SipConnection getConnection(SipRequest request, Transport transport) throws IOException
    {
		URI uri = null;
		
		Address route = request.getTopRoute();
		
		if (route != null /* && !_request.isNextHopStrictRouting() */)
			uri = route.getURI();
		else
			uri = request.getRequestURI();
		
		if (!uri.isSipURI()) 
			throw new IOException("Cannot route on URI: " + uri);
		
		SipURI target = (SipURI) uri;
		
		InetAddress address;
		if (target.getMAddrParam() != null)
			address = InetAddress.getByName(target.getMAddrParam());
		else
			address = InetAddress.getByName(target.getHost()); // TODO 3263
		
		if (transport == null)
		{
			if (target.getTransportParam() != null)
				transport =Transport.valueOf(target.getTransportParam()); // TODO opt
			else
				transport = Transport.UDP;
		}
		
		int port = target.getPort();
		if (port == -1) 
			port = transport.getDefaultPort();
		
		return getConnection(request, transport, address, port);
    }
	
    public SipConnection getConnection(SipRequest request, Transport transport, InetAddress address, int port) throws IOException
    {   
    	SipConnector connector = findConnector(transport, address);
    	
        Via via = request.getTopVia();
        
        via.setTransport(connector.getTransport().getName());
        via.setHost(connector.getURI().getHost());
        via.setPort(connector.getURI().getPort());
                
        SipConnection connection = connector.getConnection(address, port);
        if (connection == null)
        	throw new IOException("Could not find connection to " + address + ":" + port + "/" + connector.getTransport());
        
        return connection;
    }
    
    public SipConnector findConnector(Transport transport, InetAddress addr)
    {
    	boolean ipv4 = addr instanceof Inet4Address;
    	SipConnector[] connectors = getServer().getConnectors();
        for (int i = 0; i < connectors.length; i++) 
        {
            SipConnector c = connectors[i];
            if (c.getTransport() == transport && (addr == null || (c.getAddress() instanceof Inet4Address) == ipv4)) 
                return c;
        }
        return connectors[0];
    }
}
