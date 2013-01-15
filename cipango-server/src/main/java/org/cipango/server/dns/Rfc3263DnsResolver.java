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
package org.cipango.server.dns;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.cipango.dns.DnsService;
import org.cipango.dns.Name;
import org.cipango.dns.record.NaptrRecord;
import org.cipango.dns.record.Record;
import org.cipango.dns.record.SrvRecord;
import org.cipango.dns.util.Inet6Util;
import org.cipango.server.Transport;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.component.ContainerLifeCycle;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

@ManagedObject("RFC 3263 DNS resolver")
public class Rfc3263DnsResolver extends ContainerLifeCycle implements DnsResolver
{	
	private static final Logger LOG = Log.getLogger(Rfc3263DnsResolver.class);
	
	private DnsService _dnsService;
	private final List<String> _enableNaptrTransports = new ArrayList<String>();
	private final List<Transport> _enableTransports = new ArrayList<Transport>();

	public Rfc3263DnsResolver()
	{
		setEnableTransports(Arrays.asList(Transport.TCP, Transport.UDP, Transport.TLS));
	}
	

	@Override
	protected void doStart() throws Exception
	{
		if (_dnsService == null)
			setDnsService(new DnsService());
		super.doStart();
	}
	
	
	@Override
	public List<Hop> getHops(Hop hop) throws UnknownHostException
	{
		Transport transport = hop.getTransport();
		if (Inet6Util.isValidIPV4Address(hop.getHost()) ||  Inet6Util.isValidIP6Address(hop.getHost()))
		{
			hop.setAddress(InetAddress.getByName(hop.getHost()));
			hop.setTransport(hop.isSecure() ? Transport.TLS : Transport.UDP);
			if (!hop.isPortSet())
				hop.setPort(hop.getTransport().getDefaultPort());
			return Arrays.asList(hop);
		}
		
		List<Hop> hops = null;
		
		if (hop.isPortSet())
		{
			return lookupAllHostAddr(hop, transport);
		}
				
		if (transport == null)
		{
			// if no transport protocol or port is specified, and the
			// target is not a numeric IP address, the client SHOULD perform a NAPTR
			// query for the domain in the URI.
			List<Record> records = null;
			try
			{
				records = _dnsService.lookup(new NaptrRecord(hop.getHost()));
			}
			catch (IOException e)
			{
				LOG.debug("Could not get NAPTR records for name {}, SRV resolution will be done.", hop.getHost());
			}
			
			if (records != null && !records.isEmpty())
			{
				SortedSet<NaptrRecord> sipRecords = new TreeSet<NaptrRecord>();
				int minOrder = Integer.MAX_VALUE;
				for (Record record : records)
				{
					NaptrRecord naptrRecord = (NaptrRecord) record;
					String service = naptrRecord.getService();
					if (_enableNaptrTransports.contains(service) && (!hop.isSecure() || Transport.TLS.getService().equals(service)))
					{
						if (naptrRecord.getOrder() < minOrder)
						{
							sipRecords.clear();
							minOrder = naptrRecord.getOrder();
							sipRecords.add(naptrRecord);
						}
						else if (naptrRecord.getOrder() == minOrder)
							sipRecords.add(naptrRecord);
					}
				}
				
				for (NaptrRecord record : sipRecords)
				{
					Transport transport2 = getTransport(record);
					List<Hop> tmpHosts = resolveSrv(record.getReplacement(), transport2);
					if (hops == null)
						hops = tmpHosts;
					else
						hops.addAll(tmpHosts);
				}
			}
			else
			{
				// If no NAPTR records are found, the client constructs SRV queries for
				// those transport protocols it supports, and does a query for each.
				for (Transport transport2 : _enableTransports)
				{
					if (transport2.isSecure() == hop.isSecure())
					{
						List<Hop> tmpHosts = resolveSrv(hop, transport2);
						if (hops == null)
							hops = tmpHosts;
						else
							hops.addAll(tmpHosts);
					}
				}
			}
		}
		else
			hops = resolveSrv(hop, transport);
		
		
		if (hops == null || hops.isEmpty())
			hops = lookupAllHostAddr(hop, transport);
		
		return hops;
	}
	
	private List<Hop> lookupAllHostAddr(Hop hop, Transport transport) throws UnknownHostException
	{
		List<Hop> hops = new ArrayList<Hop>();
		// SRV resolution has failed, so resolve using A or AAAA request
		
		if (transport == null)
			transport = hop.isSecure() ? Transport.TLS : Transport.UDP;
		
		int port = hop.isPortSet() ? hop.getPort() : transport.getDefaultPort();
		
		InetAddress[] addresses = _dnsService.lookupAllHostAddr(hop.getHost());
		for (InetAddress address : addresses)
		{
			Hop hop2 = new Hop();
			hop2.setHost(hop.getHost());
			hop2.setPort(port);
			hop2.setTransport(transport);
			hop2.setAddress(address);
			hops.add(hop2);
		}
		return hops;
	}
	
	private Transport getTransport(NaptrRecord record)
	{
		String service = record.getService();
		for (Transport transport : Transport.values())
		{
			if (transport.getService().equalsIgnoreCase(service))
				return transport;
		}
		return null;
	}
	
	private List<Hop> resolveSrv(Hop hop, Transport transport)
	{
		Name srvName = new Name(transport.getSrvPrefix() + hop.getHost());
		return resolveSrv(srvName, transport);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private List<Hop> resolveSrv(Name srvName, Transport transport)
	{
		SortedSet<SrvRecord> records;
		try
		{
			records = new TreeSet(_dnsService.lookup(new SrvRecord(srvName)));
		}
		catch (Exception e1)
		{
			LOG.debug("Could not get SRV record for name {}", srvName);
			return null;
		}
		
		List<Hop> hops = new ArrayList<Hop>();
		for (SrvRecord record : records)
		{
			try
			{
				InetAddress[] addresses = _dnsService.lookupAllHostAddr(record.getTarget().toString());
				for (InetAddress address : addresses)
				{
					Hop hop = new Hop();
					hop.setHost(record.getTarget().toString());
					hop.setPort(record.getPort());
					hop.setAddress(address);
					hop.setTransport(transport);
					hops.add(hop);
				}
			}
			catch (UnknownHostException e)
			{
				LOG.debug("Could not get IP address for {} retrieve for SRV name {}", record.getTarget(), srvName);
			}
		}
		return hops;
	}

	public List<String> getEnableNaptrTransports()
	{
		return _enableNaptrTransports;
	}

	public void setEnableTransports(Collection<Transport> transports)
	{
		_enableNaptrTransports.clear();
		for (Transport transport : transports)
			_enableNaptrTransports.add(transport.getService());
		_enableTransports.clear();
		_enableTransports.addAll(transports);
	}


	@ManagedAttribute(value="Enabled transport", readonly=true)
	public Collection<Transport> getEnableTransports()
	{
		return Collections.unmodifiableList(_enableTransports);
	}

	@ManagedAttribute("DNS service")
	public DnsService getDnsService()
	{
		return _dnsService;
	}


	public void setDnsService(DnsService dnsService)
	{
		updateBean(_dnsService, dnsService);
		_dnsService = dnsService;
	}







}
