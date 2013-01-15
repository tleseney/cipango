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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.cipango.server.Transport;
import org.junit.Before;
import org.junit.Test;

public class SystemDnsResolverTest
{
	private DnsResolver _dnsResolver;
		
	@Before
	public void setUp() throws Exception
	{
		_dnsResolver = new SystemDnsResolver();
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
}
