// ========================================================================
// Copyright 2006-2013 NEXCOM Systems
// ------------------------------------------------------------------------
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at 
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ========================================================================
package org.cipango.server.processor;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import javax.servlet.sip.Address;
import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.URI;

import org.cipango.server.SipConnection;
import org.cipango.server.SipConnector;
import org.cipango.server.SipMessage;
import org.cipango.server.SipProcessor;
import org.cipango.server.SipRequest;
import org.cipango.server.SipResponse;
import org.cipango.server.Transport;
import org.cipango.server.dns.BlackList;
import org.cipango.server.dns.DnsResolver;
import org.cipango.server.dns.EmptyBlackList;
import org.cipango.server.dns.Hop;
import org.cipango.server.dns.Rfc3263DnsResolver;
import org.cipango.server.session.SessionHandler;
import org.cipango.sip.SipGrammar;
import org.cipango.sip.SipHeader;
import org.cipango.sip.Via;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

/**
 * Performs transport related operations such as Via, Route handling.
 */
@ManagedObject
public class TransportProcessor extends SipProcessorWrapper
{
	private Logger LOG = Log.getLogger(TransportProcessor.class);
	
	private DnsResolver _dnsResolver;
	private BlackList _blackList;
	
	public TransportProcessor(SipProcessor processor)
	{
		super(processor);
	}
	
	@Override
	protected void doStart() throws Exception
	{
		if (_dnsResolver == null)
			setDnsResolver(new Rfc3263DnsResolver());
		if (_blackList == null)
			setBlackList(new EmptyBlackList());
		
		List<Transport> transports = new ArrayList<Transport>();
		for (SipConnector connector : getServer().getConnectors())
		{
			if (!transports.contains(connector.getTransport()))
				transports.add(connector.getTransport());
		}
		_dnsResolver.setEnableTransports(transports);
		
		super.doStart();
	}
	
	public Address popLocalRoute(SipRequest request) throws ServletParseException
	{
		Address route = request.getTopRoute();
		
		if (route != null && getServer().isLocalURI(route.getURI()))
		{
			request.removeTopRoute();
			
			if (route.getURI().getParameter(SipGrammar.DRR) != null)
			{
				// Case double record route, see RFC 5658
				Address route2 = request.getTopRoute();
				String appId = route.getURI().getParameter(SessionHandler.APP_ID);
				if (route2 != null && appId != null
						&& appId.equals(route2.getURI().getParameter(SessionHandler.APP_ID))
						&& getServer().isLocalURI(route2.getURI()))
				{
					LOG.debug("Remove second top route {} due to RFC 5658", route2);
					request.removeTopRoute();
					if ("2".equals(route.getParameter(SipGrammar.DRR)))
						route = route2;
				}
			}
		}
		
		return route;
	}
	
	public void doProcess(SipMessage message) throws Exception
	{
		if (LOG.isDebugEnabled())
			LOG.debug("handling message {}", message.toStringCompact());
		
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
			else if (message.getTopVia() == null || message.getCSeq() == null)
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
			if (!valid && message instanceof SipRequest && !((SipRequest) message).isAck()
					&& message.getTopVia() != null)
			{
				// send response stateless
				SipRequest request = (SipRequest) message;
				SipResponse response = (SipResponse) request
						.createResponse(SipServletResponse.SC_BAD_REQUEST);
				getServer().sendResponse(response, request.getConnection());
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
		ListIterator<Hop> it = request.getHops();
		List<Hop> hops = null;
		
		if (it == null)
		{
			URI uri = null;
			
			Address route;
			try
			{
				route = request.getTopRoute();
			}
			catch (ServletParseException e)
			{
				throw new IOException("Invalid top route", e);
			}
			
			if (route != null && !request.isNextHopStrictRouting())
				uri = route.getURI();
			else
				uri = request.getRequestURI();
			
			if (!uri.isSipURI())
				throw new IOException("Cannot route on URI: " + uri);
			
			SipURI target = (SipURI) uri;
			
			Hop hop = new Hop();
			
			if (target.getMAddrParam() != null)
				hop.setHost(target.getMAddrParam());
			else
				hop.setHost(target.getHost());
			
			if (transport == null)
			{
				if (target.getTransportParam() != null)
				{
					try
					{
						transport = Transport.valueOf(target.getTransportParam().toUpperCase()); // TODO opt
					}
					catch (Exception e)
					{
						LOG.debug("Unknown transport: " + target.getTransportParam(), e);
					}
				}
			}
			hop.setTransport(transport);
			hop.setPort(target.getPort());
			
			hops = _dnsResolver.getHops(hop);
			LOG.debug("Physical hops are {} for hop {}", hops, hop);
			it = hops.listIterator();
			request.setHops(it);
		}
		
		while (it.hasNext())
		{
			Hop bestHop = it.next();
			if (!_blackList.isBlacklisted(bestHop))
				return getConnection(request, bestHop.getTransport(), bestHop.getAddress(), bestHop.getPort());
			else
				LOG.debug("Do no send request to hop {} as it is blacklisted", bestHop);
		}
		
		// If all black listed and request has not been sent yet, send it to first hop
		// This ensures that all requests are tried to be sent
		if (hops != null)
		{
			Hop firstHop = hops.get(0);
			return getConnection(request, firstHop.getTransport(), firstHop.getAddress(), firstHop.getPort());
		}
		throw new IOException("All remaining hops are backlisted");
	}
	
	public SipConnection getConnection(SipRequest request, Transport transport, InetAddress address, int port)
			throws IOException
	{
		SipConnector connector = findConnector(transport, address);
		
		Via via = request.getTopVia();
		
		via.setTransport(connector.getTransport().getName());
		via.setHost(connector.getURI().getHost());
		via.setPort(connector.getURI().getPort());
		
		SipConnection connection = connector.getConnection(address, port);
		if (connection == null)
			throw new IOException("Could not find connection to " + address + ":" + port + "/"
					+ connector.getTransport());
		
		return connection;
	}
	
	public SipConnector findConnector(Transport transport, InetAddress addr)
	{
		boolean ipv4 = addr instanceof Inet4Address;
		SipConnector[] connectors = getServer().getConnectors();
		for (int i = 0; i < connectors.length; i++)
		{
			SipConnector c = connectors[i];
			if (c.getTransport() == transport
					&& (addr == null || (c.getAddress() instanceof Inet4Address) == ipv4))
				return c;
		}
		return connectors[0];
	}
	
	@ManagedAttribute(value = "DNS resolver", readonly = true)
	public DnsResolver getDnsResolver()
	{
		return _dnsResolver;
	}
	
	public void setDnsResolver(DnsResolver dnsResolver)
	{
		updateBean(_dnsResolver, dnsResolver);
		_dnsResolver = dnsResolver;
	}
	
	@ManagedAttribute(value = "Black list", readonly = true)
	public BlackList getBlackList()
	{
		return _blackList;
	}
	
	public void setBlackList(BlackList blackList)
	{
		updateBean(_blackList, blackList);
		_blackList = blackList;
	}
	
}
