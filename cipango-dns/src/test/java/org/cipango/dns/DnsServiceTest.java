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
package org.cipango.dns;

import static org.junit.Assert.*;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.MissingResourceException;
import java.util.Random;

import junit.framework.Assert;

import org.cipango.dns.bio.TcpConnector;
import org.cipango.dns.bio.UdpConnector;
import org.cipango.dns.record.ARecord;
import org.cipango.dns.record.NaptrRecord;
import org.cipango.dns.record.OptRecord;
import org.cipango.dns.record.PtrRecord;
import org.cipango.dns.record.Record;
import org.cipango.dns.record.SrvRecord;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class DnsServiceTest
{
	private DnsService _dnsService;
	private Class<DnsConnector> _connectorClass;
	public static final String  IPV4_ADDR = "213.186.33.5";
	public static final String  IPV6_ADDR = "2001:41d0:2:7a93::1";
	
	
	@Before
	public void setUp() throws Exception
	{
		_dnsService = new DnsService();
		_dnsService.addConnector(_connectorClass.newInstance());
		_dnsService.start();
	}
	
	@Parameters
	public static Collection<Object[]> data() {
		try
		{
			return Arrays.asList(new Object[][] {
					{
						UdpConnector.class
					},
					{
						TcpConnector.class
					} });
		}
		catch (MissingResourceException e)
		{
			throw new RuntimeException("Incomplete properties:" + e.getMessage());
		}
	}
	
	@After
	public void tearDown() throws Exception
	{
		_dnsService.stop();
	}
	
	public DnsServiceTest(Class<DnsConnector> connectorClass) throws Exception {
		_connectorClass = connectorClass;
	}
	
	
	@Test
	public void testA() throws Exception
	{
		InetAddress[] addr = _dnsService.lookupAllHostAddr("jira.cipango.org");
		assertNotNull(addr);
		assertEquals(1, addr.length);
		assertEquals(IPV4_ADDR, addr[0].getHostAddress());
	}
	
	@Test
	public void testEdns() throws Exception
	{
		
		_dnsService.getResolvers()[0].setQueryOpt(new OptRecord(1280));
		testBigRequest("EDNS");
	}
	
	private void testBigRequest(String description) {
		try {
			InetAddress[] addr = _dnsService.lookupAllHostAddr("big.cipango.voip");
			assertNotNull(addr);
			assertEquals(34, addr.length);
		} catch (UnknownHostException e) {
			System.err.println("Could not test " + description + " : entry big.cipango.voip is not in DNS");
			System.err.println("To be able to pass this test, you should add on your DNS server, the file "
					+ "cipango-server/src/test/resources/org/cipango/server/dns/db.cipango.voip");
		}
	}
	
	@Test
	public void testTruncated() throws Exception
	{
		_dnsService.addConnector(new TcpConnector());
		testBigRequest("truncated flag and fallback to TCP");
	}
	
	private void setLocalPorts(int min, int delta) 
	{
		DnsConnector connector = _dnsService.getDefaultConnector();
		if (connector instanceof UdpConnector)
			((UdpConnector) connector).setPort(min);
		else 
		{
			TcpConnector tcpConnector = (TcpConnector) connector;
			// Use delta to prevent exception due to BindException if test is run multiple times
			tcpConnector.setMinPort(new Random().nextInt(delta) + min);
			tcpConnector.setMaxPort(tcpConnector.getMinPort() + delta);
		}
	}
	
	@Test
	public void testNewConnection() throws Exception
	{
		AbstractConnector connector = (AbstractConnector) _dnsService.getDefaultConnector();
		setLocalPorts(10053, 10);
		connector.setTimeout(1100);
		InetAddress[] addr = _dnsService.lookupAllHostAddr("jira.cipango.org");
		assertNotNull(addr);
		
		System.out.println("Waiting 1 second for connection timeout");
		int currentPort = 0;
		if (connector instanceof TcpConnector) {
			currentPort = ((TcpConnector) connector).getCurrentPort();
		}
		Thread.sleep(4500);
		// After timeout, no socket should be open.
		if (connector instanceof UdpConnector) {
			DatagramSocket socket = new DatagramSocket(10053);
			socket.close();
		}
		
		addr = _dnsService.lookupAllHostAddr("confluence.cipango.org");
		assertNotNull(addr);
		
		if (connector instanceof TcpConnector) {
			assertTrue(((TcpConnector) connector).getCurrentPort() != currentPort);
		}
	}
	
	
	
	@Test
	public void testConcurrent() throws Exception
	{
		setLocalPorts(10053, 50);
		// In TCP, with some server there could be more latency with some DNS servers
		_dnsService.getResolvers()[0].setTimeout(4000); 
		Load[] loads = new Load[5];
		Random random = new Random(); // Use random to prevent cache
		for (int i = 0; i < loads.length; i++)
		{
			loads[i] = new Load("test" + random.nextInt() + ".cipango.org", 4);
			new Thread(loads[i]).start();
		}
	
		for (int i = 0; i < loads.length; i++)
			loads[i].waitDone();
		
		for (int i = 0; i < loads.length; i++)
			assertEquals(0, loads[i].getExceptions().size());

	}
	
	@Test (expected = UnknownHostException.class)
	public void testInvalidHost() throws Exception
	{
		_dnsService.lookupAllHostAddr("host.bad.cipango.org");
	}
	
	@Test
	public void testAaaa() throws Exception
	{
		_dnsService.setPreferIpv6(true);
		InetAddress[] addr = _dnsService.lookupAllHostAddr("cipango.org");
		assertNotNull(addr);
		assertEquals(1, addr.length);
		assertEquals(InetAddress.getByName(IPV6_ADDR), addr[0]);
	}
	
	@Test
	public void testSrv() throws Exception
	{
		List<Record> records = _dnsService.lookup(new SrvRecord("sip", "udp", "cipango.org"));
		assertNotNull(records);
		assertEquals(1, records.size());
		//System.out.println(records);
		SrvRecord srvRecord = (SrvRecord) records.get(0);
		assertEquals(10, srvRecord.getPriority());
		assertEquals(60, srvRecord.getWeight());
		assertEquals(5060, srvRecord.getPort());
		assertEquals("cipango.org", srvRecord.getTarget().toString());
	}
	
	@Test
	public void testNaptr() throws Exception
	{
		List<Record> records = _dnsService.lookup(new NaptrRecord("cipango.org"));
		assertNotNull(records);
		assertEquals(2, records.size());
		//System.out.println(records);
		for (Record record : records)
		{
			NaptrRecord naptrRecord = (NaptrRecord) record;
			if (naptrRecord.getOrder() == 100)
			{
				assertEquals(50, naptrRecord.getPreference());
				assertEquals("S", naptrRecord.getFlags());
				assertEquals("SIP+D2U", naptrRecord.getService());
				assertEquals("", naptrRecord.getRegexp());
				assertEquals("_sip._udp.cipango.org", naptrRecord.getReplacement().toString());
			}
			else if (naptrRecord.getOrder() == 90)
			{
				assertEquals(50, naptrRecord.getPreference());
				assertEquals("S", naptrRecord.getFlags());
				assertEquals("SIP+D2T", naptrRecord.getService());
				assertEquals("", naptrRecord.getRegexp());
				assertEquals("_sip._tcp.cipango.org", naptrRecord.getReplacement().toString());
			}
		}
	}
	
	@Test
	public void testPtr() throws Exception
	{
		List<Record> records = _dnsService.lookup(new PtrRecord(InetAddress.getByName(IPV4_ADDR)));
		assertEquals(1, records.size());
		PtrRecord ptr = (PtrRecord) records.get(0);
		assertEquals("redirect.ovh.net", ptr.getPrtdName().toString());
		//System.out.println(records);
	}
	

	public void testPtrIpv6() throws Exception
	{
		//new PtrRecord(InetAddress.getByName(IPV6_ADDR));
		List<Record> records = _dnsService.lookup(new PtrRecord(InetAddress.getByName(IPV6_ADDR)));
		assertEquals(1, records.size());
		PtrRecord ptr = (PtrRecord) records.get(0);
		assertEquals("46-105-46-188.ovh.net", ptr.getPrtdName().toString());
		//System.out.println(records);
	}
	
	@Test
	public void testBadResolver() throws Exception
	{
		Resolver badResolver = new Resolver();
		badResolver.setHost("127.0.0.1");
		badResolver.setPort(45877);
		badResolver.setTimeout(500);
		badResolver.setAttemps(2);
		Resolver[] resolvers = new Resolver[2];
		resolvers[0] = badResolver;
		resolvers[1] = _dnsService.getResolvers()[0];
		_dnsService.setResolvers(resolvers);
		InetAddress[] addr = _dnsService.lookupAllHostAddr("jira.cipango.org");
		assertNotNull(addr);
		assertEquals(1, addr.length);
		assertEquals(IPV4_ADDR, addr[0].getHostAddress());
	}
	
	@Test
	public void testSearchList() throws Exception
	{
		Name[] searchList = new Name[] {new Name("cipango.org")};
		_dnsService.setSearchList(searchList);
		InetAddress[] addr = _dnsService.lookupAllHostAddr("jira");
		//System.out.println(addr);
		assertNotNull(addr);
		assertEquals(1, addr.length);
		assertEquals(IPV4_ADDR, addr[0].getHostAddress());
	}	
	
	@Test
	public void testEtcHost() throws Exception
	{
		_dnsService.addEtcHosts(getClass().getResourceAsStream("/hosts"));
		_dnsService.lookupAllHostAddr("space.cipango.test");
		assertIpEqual("space.cipango.test", "192.168.1.1");
		assertIpEqual("tab.cipango.test", "192.168.1.2");
		assertIpEqual("multiple.cipango.test", "192.168.1.3");
		assertIpEqual("multiple", "192.168.1.3");
		assertIpEqual("comment.cipango.test", "192.168.1.4");
		assertIpEqual("comment", "192.168.1.4");
	}
	
	private void assertIpEqual(String name, String expectedIp) throws UnknownHostException
	{
		InetAddress[] actual = _dnsService.lookupAllHostAddr(name);
		if (actual == null || actual.length == 0)
			Assert.fail("Got no records for " + name);
		
		if (actual.length != 1)
			Assert.fail("Got multiple records for " + name + ": " + Arrays.asList(actual));
		
		InetAddress expectedAddr = InetAddress.getByName(expectedIp);
		assertEquals(expectedAddr, actual[0]);
	}
		
	class Load implements Runnable
	{
		private String _name;
		private int _messages;
		private List<Exception> _exceptions = new ArrayList<Exception>();
		private boolean _done = false;
		
		public Load(String name, int messages)
		{
			_name = name;
			_messages = messages;
		}
		
		public void run()
		{
			long start = System.currentTimeMillis();
			for (int i = 0; i < _messages; i++)
			{
				try
				{
					_dnsService.lookup(new ARecord(i + _name));
				}
				catch (UnknownHostException e) 
				{
				}
				catch (Exception e) 
				{
					_exceptions.add(e);
					e.printStackTrace();
				}
			}
			synchronized (this)
			{
				_done = true;
				this.notify();
			}
			System.out.println("Took " + ((System.currentTimeMillis() - start) / _messages) + "ms by message");
		}

		public List<Exception> getExceptions()
		{
			return _exceptions;
		}
		
		public void waitDone()
		{
			synchronized (this)
			{	
				while (!_done)
				{
					try
					{
						this.wait(200);
					}
					catch (InterruptedException e){}	
				}
			}
			
		}
		
	}
}
