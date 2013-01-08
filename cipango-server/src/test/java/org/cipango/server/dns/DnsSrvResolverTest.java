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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.cipango.dns.DnsService;
import org.cipango.server.Transport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Simulate all case of client resolution that are described in RFC 3263.
 * 
 * Note: To see DNS requests on network, use bind with configuration file db.cipango.voip saved in
 * this package and replace TestDnsService instance by DnsService instance in setUp method.
 */
public class DnsSrvResolverTest
{

	private DnsSrvResolver _dnsResolver;
	private static InetAddress DEFAULT_IP;
		
	@Before
	public void setUp() throws Exception
	{
		DEFAULT_IP = InetAddress.getByName("192.168.2.183");
		_dnsResolver = new DnsSrvResolver();
		DnsService dnsService = new TestDnsService();
		_dnsResolver.setDnsService(dnsService);
		dnsService.start();
	}
	
	
	@After
	public void tearDown() throws Exception
	{
		_dnsResolver.getDnsService().stop();
	}
	
	/**
	 * Ensure that if two NAPTR records are present in DNS response only the one with lower order is returned.
	 */
	@Test
	public void testTcpOrder() throws IOException
	{
		Hop hop = new Hop();
		hop.setHost("tcp-order.cipango.voip");
		
		List<Hop> hops = _dnsResolver.getHops(hop);
		
		//System.out.println(hops);
		assertEquals(1, hops.size());
		hop = hops.get(0);
		assertEquals(Transport.TCP, hop.getTransport());
		assertEquals(DEFAULT_IP, hop.getAddress());
		assertEquals("sip.cipango.voip", hop.getHost());
	}
	
	/**
	 * Ensure that if two NAPTR records are present in DNS response with same order, both are returned.
	 */
	@Test
	public void testSameOrder() throws IOException
	{
		Hop hop = new Hop();
		hop.setHost("same-order.cipango.voip");
		
		List<Hop> hops = _dnsResolver.getHops(hop);
		
		//System.out.println(hops);
		assertEquals(2, hops.size());
		List<Transport> expectedTransports = new ArrayList<Transport>(Arrays.asList(Transport.TCP, Transport.UDP));
		
		for (Hop hop2 : hops)
		{
			assertTrue(expectedTransports.remove(hop2.getTransport()));
			assertEquals(DEFAULT_IP, hop2.getAddress());
			assertEquals("sip.cipango.voip", hop2.getHost());
		}
		assertTrue(expectedTransports.isEmpty());
	}
	
	/**
	 * Ensure that if two NAPTR records are present in DNS response with same order, both are returned by preference order.
	 */
	@Test
	public void testUdpPref() throws IOException
	{
		Hop hop = new Hop();
		hop.setHost("udp-pref.cipango.voip");
		
		List<Hop> hops = _dnsResolver.getHops(hop);
		
		//System.out.println(hops);
		assertEquals(2, hops.size());
		hop = hops.get(0);
		assertEquals(Transport.UDP, hop.getTransport());
		assertEquals("sip.cipango.voip", hop.getHost());
		hop = hops.get(1);
		assertEquals(Transport.TCP, hop.getTransport());
		assertEquals("sip.cipango.voip", hop.getHost());
	}
	
	/**
	 * Ensure that a NAPTR record with an unknown service is not selected.
	 */
	@Test
	public void testUnknownService() throws IOException
	{
		Hop hop = new Hop();
		hop.setHost("unknown-service.cipango.voip");
		
		List<Hop> hops = _dnsResolver.getHops(hop);
		
		//System.out.println(hops);
		assertEquals(1, hops.size());
		hop = hops.get(0);
		assertEquals(Transport.UDP, hop.getTransport());
		assertEquals(DEFAULT_IP, hop.getAddress());
		assertEquals("sip.cipango.voip", hop.getHost());
		assertEquals(5060, hop.getPort());
	}
	
	/**
	 * Ensure that a NAPTR record with an unknown service is not selected.
	 */
	@Test
	public void testUnsuportedService() throws IOException
	{
		_dnsResolver.setEnableTransports(Arrays.asList(Transport.UDP));
		Hop hop = new Hop();
		hop.setHost("same-order.cipango.voip");
		
		List<Hop> hops = _dnsResolver.getHops(hop);
		
		//System.out.println(hops);
		assertEquals(1, hops.size());
		hop = hops.get(0);
		assertEquals(Transport.UDP, hop.getTransport());
		assertEquals(DEFAULT_IP, hop.getAddress());
		assertEquals("sip.cipango.voip", hop.getHost());
		assertEquals(5060, hop.getPort());
	}
	
	
	@Test
	public void testNumericalAddress() throws IOException
	{
		Hop hop = new Hop();
		hop.setHost("192.168.1.1");
		
		List<Hop> hops = _dnsResolver.getHops(hop);
		
		//System.out.println(hops);
		assertEquals(1, hops.size());
		hop = hops.get(0);
		assertEquals(Transport.UDP, hop.getTransport());
		assertEquals(5060, hop.getPort());
		assertEquals("192.168.1.1", hop.getAddress().getHostAddress());
	}
	
	@Test
	public void testPortSet() throws IOException
	{
		Hop hop = new Hop();
		hop.setHost("port-set.cipango.voip");
		hop.setPort(5060);
		
		List<Hop> hops = _dnsResolver.getHops(hop);
		
		//System.out.println(hops);
		assertEquals(1, hops.size());
		hop = hops.get(0);
		assertEquals(Transport.UDP, hop.getTransport());
		assertEquals("192.168.1.2", hop.getAddress().getHostAddress());
	}
	
	@Test
	public void testTransportSet() throws IOException
	{
		Hop hop = new Hop();
		hop.setHost("transport-set.cipango.voip");
		hop.setTransport(Transport.TCP);
		
		List<Hop> hops = _dnsResolver.getHops(hop);
		
		//System.out.println(hops);
		assertEquals(1, hops.size());
		hop = hops.get(0);
		assertEquals(Transport.TCP, hop.getTransport());
		assertEquals(5070, hop.getPort());
		assertEquals(DEFAULT_IP, hop.getAddress());
	}
	
	/**
	 * If no NAPTR records are found, the client constructs SRV queries for those transport
	 * protocols it supports, and does a query for each. Queries are done using the service
	 * identifier "_sip" for SIP URIs and "_sips" for SIPS URIs.
	 */
	@Test
	public void testNaptrNoRecords() throws IOException
	{
		_dnsResolver.setEnableTransports(Arrays.asList(Transport.UDP, Transport.TCP));
		Hop hop = new Hop();
		hop.setHost("naptr-empty.cipango.voip");
		
		List<Hop> hops = _dnsResolver.getHops(hop);
		
		//System.out.println(hops);
		assertEquals(2, hops.size());
		hop = hops.get(0);
		assertEquals(Transport.UDP, hop.getTransport());
		assertEquals(5090, hop.getPort());
		assertEquals(DEFAULT_IP, hop.getAddress());

		hop = hops.get(1);
		assertEquals(Transport.TCP, hop.getTransport());
		assertEquals(5080, hop.getPort());
		assertEquals(DEFAULT_IP, hop.getAddress());
	}
	
	/**
	 * Case no NAPTR and no SRV records are returned ==> A or AAAA request is done
	 */
	@Test
	public void testSrvNoRecords() throws IOException
	{
		_dnsResolver.setEnableTransports(Arrays.asList(Transport.UDP, Transport.TCP));
		Hop hop = new Hop();
		hop.setHost("srv-empty.cipango.voip");
		
		List<Hop> hops = _dnsResolver.getHops(hop);
		
		//System.out.println(hops);
		assertEquals(1, hops.size());
		hop = hops.get(0);
		assertEquals(Transport.UDP, hop.getTransport());
		assertEquals(5060, hop.getPort());
		assertEquals("srv-empty.cipango.voip", hop.getHost());
		assertEquals("192.168.1.3", hop.getAddress().getHostAddress());
	}
	
	@Test
	public void testMutipleARecords() throws IOException
	{
		Hop hop = new Hop();
		hop.setHost("multiple.cipango.voip");
		
		List<Hop> hops = _dnsResolver.getHops(hop);
		
		//System.out.println(hops);
		assertEquals(2, hops.size());
		List<String> expectedIp = new ArrayList<String>(Arrays.asList("192.168.1.4", "192.168.1.5"));
		
		for (Hop hop2 : hops)
		{
			assertEquals(Transport.UDP, hop2.getTransport());
			assertEquals(5090, hop2.getPort());
			assertEquals("multiple-a.cipango.voip", hop2.getHost());
			assertTrue(expectedIp.remove(hop2.getAddress().getHostAddress()));
		}
		assertTrue(expectedIp.isEmpty());
	}
}
