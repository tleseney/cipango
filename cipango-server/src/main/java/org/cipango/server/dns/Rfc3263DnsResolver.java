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
import java.util.Random;
import java.util.Iterator; 

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
	private boolean _useNaptr = true;
	private final List<String> _enableNaptrTransports = new ArrayList<String>();
	private final List<Transport> _enableTransports = new ArrayList<Transport>();
	private final Random _random = new Random();

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
			if (transport == null)
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
			if (_useNaptr)
			{
  			try
  			{
  				records = _dnsService.lookup(new NaptrRecord(hop.getHost()));
  			}
  			catch (IOException e)
  			{
  				LOG.debug("Could not get NAPTR records for name {}, SRV resolution will be done.", hop.getHost());
  			}
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
	
	private List<Hop> resolveSrv(Name srvName, Transport transport)
	{
		Collection<SrvRecord> records;
		try
		{
			records = sortSrv(_dnsService.lookup(new SrvRecord(srvName)));
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
	
	/**
	 * For priority/weight resolution we need to do more then just sorting SRVs by priority/weight.
	 * Specifically: Among records with the same priority we need to randomly choose record according to weight.
	 * 
	 * Note: method is protected, to make it possible to apply different sorting methods to SRV in subclasses
	 * (e.g. methods which account for deployment site network topology, for example favoring records pointing to 
	 * "nearby" servers).
	 * 
	 * @param original
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected Collection<SrvRecord> sortSrv(List<Record> original)
	{
	  List<SrvRecord> list = new ArrayList(original);
	  
	  Collections.sort(list);
	  // At this point we have sorted list by priority, weight (and hash code).
	  // Now we need to randomize result according to weights within the same priority.
	  
	  ArrayList<SrvRecord> result = new ArrayList<SrvRecord>(list.size());
	  while (!list.isEmpty())
	  {
	    // Get total weight for all (remaining) top-priority records.
	    final int priority = list.get(0).getPriority();
	    int totalWeight = 0;
	    int count = 0;
	    for (SrvRecord r : list)
	    {
	      if (r.getPriority() != priority) break;
	      totalWeight += r.getWeight();
	      count++;
	    }
	    if (count == 1)
	    {
	      // Optimization: if there is only one top-priority record left, don't need to generate random numbers.
	      result.add(list.remove(0));
	      continue;
	    }
	    // select random number between 0 (inclusive) and totalWeight (exclusive):
	    int w = _random.nextInt(totalWeight);
	    // Now find the record which was selected by random number:
	    for (final Iterator<SrvRecord> i = list.iterator(); i.hasNext();)
	    {
	      final SrvRecord r = i.next(); // It's guaranteed that r has top priority.
	      w -= r.getWeight();
	      if (w < 0)
	      {
	        // The first record which leads to strictly negative number should be the one selected.
	        // Example: 2 records, each has weight 1. Then totalWeight would be 2 and random w can be either 0 or 1.
	        // in case w==0 the first record should be returned, in case w==1 the second.
	        result.add(r);
	        i.remove();
	        break;
	      }
	    }
	    // Keep looping until we have any SRV records left.
	  }
	  return result;
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

	@ManagedAttribute("Use NAPTR")
  public boolean getUseNaptr()
  {
    return _useNaptr;
  }
	/**
	 * Relaxes RFC3263: When environment is known to do not have NAPTR records as optimization NAPTR lookup can be skipped.
	 * @param useNaptr
	 */
  public void setUseNaptr(boolean useNaptr)
  {
    updateBean(_useNaptr, useNaptr);
    _useNaptr = useNaptr;
  }





}
