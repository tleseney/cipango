// ========================================================================
// Copyright 2006-2014 NEXCOM Systems
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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import org.cipango.dns.record.ARecord;
import org.cipango.dns.record.AaaaRecord;
import org.cipango.dns.record.CnameRecord;
import org.cipango.dns.record.Record;
import org.junit.Test;

public class CacheTest
{

	@Test
	public void testGet() throws Exception
	{
		Name name = new Name("www.cipango.org");
		Cache cache = new Cache();
		
		ARecord r1 = new ARecord(name.toString());
		r1.setTtl(1);
		r1.setAddress(InetAddress.getByName("192.168.1.1"));
		cache.addRecord(r1);
		
		List<Record> records = cache.getRecords(name, Type.A);
		assertEquals(1, records.size());
		assertEquals(r1, records.get(0));
		
		ARecord r2 = new ARecord(name.toString());
		r2.setTtl(1);
		r2.setAddress(InetAddress.getByName("192.168.1.2"));
		cache.addRecord(r2);
		
		AaaaRecord r3 = new AaaaRecord(name.toString());
		r3.setTtl(1);
		r3.setAddress(InetAddress.getByName("[2000::1]"));
		cache.addRecord(r3);
		
		records = cache.getRecords(name, Type.A);
		assertEquals(2, records.size());
		assertTrue(records.contains(r1));
		assertTrue(records.contains(r2));
		
		records = cache.getRecords(name, Type.AAAA);
		assertEquals(1, records.size());
		assertEquals(r3, records.get(0));
		
		assertTrue(cache.getRecords(name, Type.NAPTR).isEmpty());
		
		Name name2 = new Name("cipango.org");
		CnameRecord r4 = new CnameRecord();
		r4.setCname(name);
		r4.setName(name2);
		r4.setTtl(10);
		cache.addRecord(r4);
		
		records = cache.getRecords(name2, Type.AAAA);
		assertEquals(1, records.size());
		assertEquals(r4, records.get(0));
		
		
	}
	
	@Test
	public void testConcurrent() throws InterruptedException
	{
		testConcurrent(false);
	}
	
	@Test
	public void testConcurrentWithScavenge() throws InterruptedException
	{
		testConcurrent(true);
	}
	

	public void testConcurrent(final boolean scavenge) throws InterruptedException 
	{
		final Cache cache = new Cache();
		int threads = 5;
		final int maxHost = 500;
		final CountDownLatch doneSignal = new CountDownLatch(threads);
		for (int i = 0; i < threads; i++)
		{
			new Thread() {
				public void run() {
					Random rand = new Random();
					try 
					{
    					for (int i = 0; i < 50000; i++) 
    					{
    						ARecord r = new ARecord(rand.nextInt(maxHost) + ".www.cipango.org");
        					r.setTtl(1);
        					r.setAddress(InetAddress.getByName("192.168.2." + rand.nextInt(5)));
        					cache.addRecord(r);

        					if (!scavenge)
        						cache.getRecords(new Name(rand.nextInt(maxHost) + ".www.cipango.org"), Type.A);

    						if (rand.nextInt(500) == 0)
    							Thread.sleep(10);	
    					}
					} catch (Exception e)
					{
						e.printStackTrace();
					}
					doneSignal.countDown();
				}
			}.start();
		}
		
		while (scavenge && doneSignal.getCount() != 0)
			cache.scavenge();
		
		doneSignal.await();
		for (int i = 0; i < maxHost; i++) 
		{
			try { 
				List<Record> records = cache.getRecords(new Name(i + ".www.cipango.org"), Type.A);
				assertTrue(records.size() > 1);
				
			} catch (UnknownHostException e)
			{
				fail("Should have record for " + i + ".www.cipango.org");
			}
		}
		//System.out.println(cache.dump());
		
		Thread.sleep(1010);
		
		for (int i = 0; i < maxHost; i++) 
		{
			try { 
				List<Record> records = cache.getRecords(new Name(i + ".www.cipango.org"), Type.A);
				assertTrue("Should be empty for " + i + ".www.cipango.org", records.isEmpty());
			} catch (UnknownHostException e) {}
		}
		
		assertEquals("Cache is not empty", new Cache().dump(), cache.dump());
	}
}
