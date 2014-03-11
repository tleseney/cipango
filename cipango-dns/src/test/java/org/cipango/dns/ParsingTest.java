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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.ByteBuffer;

import org.cipango.dns.record.ARecord;
import org.cipango.dns.record.AaaaRecord;
import org.cipango.dns.record.NaptrRecord;
import org.cipango.dns.record.NsRecord;
import org.cipango.dns.record.Record;
import org.cipango.dns.record.SoaRecord;
import org.cipango.dns.record.SrvRecord;
import org.cipango.dns.section.HeaderSection;
import org.cipango.dns.section.HeaderSection.OpCode;
import org.cipango.dns.section.HeaderSection.ResponseCode;
import org.cipango.dns.section.QuestionSection;
import org.junit.Test;

public class ParsingTest
{
	@Test
	public void testParsingAQuery() throws Exception
	{
		DnsMessage message = getMessage("/queryA.dat");
		HeaderSection header = message.getHeaderSection();
		assertEquals(0xECD1, header.getId());
		assertEquals(OpCode.QUERY, header.getOpCode());
		assertFalse(header.isTruncated());
		assertTrue(header.isRecursionDesired());	
		
		QuestionSection question = message.getQuestionSection();
		assertEquals(1, question.size());
		Record record = question.get(0);
		assertEquals(Type.A, record.getType());
		assertEquals("www.cipango.org", record.getName().toString());
		assertEquals(DnsClass.IN, record.getDnsClass());
	}
	
	@Test
	public void testEncode() throws Exception
	{
		testEncode("/queryA.dat");
		testEncode("/responseA.dat");
		testEncode("/responseAAAA.dat");
		testEncode("/responseSrv.dat");
		testEncode("/responseNaptr.dat");
		testEncode("/responseNameError.dat");
	}
	

	public void checkPerf() throws Exception
	{
		byte[] raw = getRawMessage("/responseA.dat");
		for (int i = 0; i < 100000; i++)
		{
			DnsMessage message = new DnsMessage();
			message.decode(ByteBuffer.wrap(raw));
		}
	}
	
	public void testEncode(String name) throws Exception
	{
		byte[] raw = getRawMessage(name);
		ByteBuffer origBuffer = ByteBuffer.wrap(raw);
		DnsMessage message = new DnsMessage();
		message.decode(origBuffer);
		origBuffer = ByteBuffer.wrap(raw);
		ByteBuffer buffer = ByteBuffer.allocate(raw.length);
		message.encode(buffer);
		byte[] encoded = buffer.array();
		//System.out.println(message);
		
		DnsMessage message2 = new DnsMessage();
		message2.decode(ByteBuffer.wrap(encoded));
		
		assertArrayEquals(raw, encoded);
	}
	
	@Test
	public void testParsingAResponse() throws Exception
	{		
		DnsMessage message = getMessage("/responseA.dat");
		HeaderSection header = message.getHeaderSection();
		assertEquals(0xECD1, header.getId());
		assertEquals(OpCode.QUERY, header.getOpCode());
		assertFalse(header.isTruncated());
		assertTrue(header.isRecursionDesired());	
		assertTrue(header.isRecursionAvailable());
		assertEquals(ResponseCode.NO_ERROR, header.getResponseCode());
		
		QuestionSection question = message.getQuestionSection();
		assertEquals(1, question.size());
		Record record = question.get(0);
		assertEquals(Type.A, record.getType());
		assertEquals("www.cipango.org", record.getName().toString());
		assertEquals(DnsClass.IN, record.getDnsClass());
		
		assertEquals(1, message.getAnswerSection().size());
		record = message.getAnswerSection().get(0);
		assertEquals(Type.A, record.getType());
		assertEquals("www.cipango.org", record.getName().toString());
		assertEquals(DnsClass.IN, record.getDnsClass());
		assertEquals(0x013EBC, record.getTtl());
		assertEquals(InetAddress.getByName("46.105.46.188"), ((ARecord) record).getAddress());
		
		assertEquals(2, message.getAuthoritySection().size());
		record = message.getAuthoritySection().get(0);
		assertEquals(Type.NS, record.getType());
		assertEquals("cipango.org", record.getName().toString());
		assertEquals(DnsClass.IN, record.getDnsClass());
		assertEquals(0x010F63, record.getTtl());
		assertEquals("ns.ovh.net", ((NsRecord) record).getNsdName().toString());
		
		record = message.getAuthoritySection().get(1);
		assertEquals(Type.NS, record.getType());
		assertEquals("cipango.org", record.getName().toString());
		assertEquals(DnsClass.IN, record.getDnsClass());
		assertEquals(0x010F63, record.getTtl());
		assertEquals("dns.ovh.net", ((NsRecord) record).getNsdName().toString());
		
		assertEquals(2, message.getAdditionalSection().size());
		record = message.getAdditionalSection().get(0);
		assertEquals(Type.A, record.getType());
		assertEquals("ns.ovh.net", record.getName().toString());
		assertEquals(DnsClass.IN, record.getDnsClass());
		assertEquals(0x01152B, record.getTtl());
		assertEquals(InetAddress.getByName("213.251.128.136"), ((ARecord) record).getAddress());
		
		record = message.getAdditionalSection().get(1);
		assertEquals(Type.A, record.getType());
		assertEquals("dns.ovh.net", record.getName().toString());
		assertEquals(DnsClass.IN, record.getDnsClass());
		assertEquals(0x01152B, record.getTtl());
		assertEquals(InetAddress.getByName("213.186.33.102"), ((ARecord) record).getAddress());
	}
	
	@Test
	public void testParsingNameError() throws Exception
	{		
		DnsMessage message = getMessage("/responseNameError.dat");
		HeaderSection header = message.getHeaderSection();
		assertEquals(ResponseCode.NAME_ERROR, header.getResponseCode());
		assertEquals(0, message.getAnswerSection().size());
		assertEquals(1, message.getAuthoritySection().size());
		SoaRecord soaRecord = (SoaRecord) message.getAuthoritySection().get(0);
		assertEquals("cipango.org", soaRecord.getName().toString());
		assertEquals("dns.ovh.net", soaRecord.getPrimaryNameServer().toString());
		assertEquals("tech.ovh.net", soaRecord.getMailbox().toString());
		assertEquals(2011041207, soaRecord.getSerial());
		assertEquals(86400, soaRecord.getRefresh());
		assertEquals(3600, soaRecord.getRetry());
		assertEquals(3600000, soaRecord.getExpires());
		assertEquals(86400, soaRecord.getMinimumTtl());
	}
	
	@Test
	public void testParsingAAAAResponse() throws Exception
	{		
		DnsMessage message = getMessage("/responseAAAA.dat");
		assertEquals(ResponseCode.NO_ERROR, message.getHeaderSection().getResponseCode());
		
		QuestionSection question = message.getQuestionSection();
		assertEquals(1, question.size());
		Record record = question.get(0);
		assertEquals(Type.AAAA, record.getType());
		assertEquals("cipango.org", record.getName().toString());
		assertEquals(DnsClass.IN, record.getDnsClass());
		
		assertEquals(1, message.getAnswerSection().size());
		record = message.getAnswerSection().get(0);
		assertEquals(Type.AAAA, record.getType());
		assertEquals("cipango.org", record.getName().toString());
		assertEquals(DnsClass.IN, record.getDnsClass());
		assertEquals(InetAddress.getByName(DnsServiceTest.IPV6_ADDR),
				((AaaaRecord) record).getAddress());
	}
	
	@Test
	public void testParsingSrvResponse() throws Exception
	{		
		DnsMessage message = getMessage("/responseSrv.dat");
		assertEquals(ResponseCode.NO_ERROR, message.getHeaderSection().getResponseCode());
		
		QuestionSection question = message.getQuestionSection();
		assertEquals(1, question.size());
		Record record = question.get(0);
		assertEquals(Type.SRV, record.getType());
		assertEquals("_sip._udp.cipango.org", record.getName().toString());
		
		assertEquals(1, message.getAnswerSection().size());
		record = message.getAnswerSection().get(0);
		assertEquals(Type.SRV, record.getType());
		assertEquals("_sip._udp.cipango.org", record.getName().toString());
		assertEquals(DnsClass.IN, record.getDnsClass());
		SrvRecord srvRecord = (SrvRecord) record;
		assertEquals(10, srvRecord.getPriority());
		assertEquals(60, srvRecord.getWeight());
		assertEquals(5060, srvRecord.getPort());
		assertEquals("cipango.org", srvRecord.getTarget().toString());
	}
	
	@Test
	public void testParsingNaptrResponse() throws Exception
	{		
		DnsMessage message = getMessage("/responseNaptr.dat");
		assertEquals(ResponseCode.NO_ERROR, message.getHeaderSection().getResponseCode());
		//System.out.println(message);
		assertEquals(2, message.getAnswerSection().size());
		
		NaptrRecord naptrRecord = (NaptrRecord) message.getAnswerSection().get(0);
		assertEquals(90, naptrRecord.getOrder());
		assertEquals(50, naptrRecord.getPreference());
		assertEquals("S", naptrRecord.getFlags());
		assertEquals("SIP+D2T", naptrRecord.getService());
		assertEquals("", naptrRecord.getRegexp());
		assertEquals("_sip._tcp.cipango.org", naptrRecord.getReplacement().toString());
		

		naptrRecord = (NaptrRecord) message.getAnswerSection().get(1);
		assertEquals(100, naptrRecord.getOrder());
		assertEquals(50, naptrRecord.getPreference());
		assertEquals("S", naptrRecord.getFlags());
		assertEquals("SIP+D2U", naptrRecord.getService());
		assertEquals("", naptrRecord.getRegexp());
		assertEquals("_sip._udp.cipango.org", naptrRecord.getReplacement().toString());
	}
	
	@Test
	public void testParsingAnyResponse() throws Exception
	{		
		DnsMessage message = getMessage("/responseAny.dat");
		assertEquals(ResponseCode.NO_ERROR, message.getHeaderSection().getResponseCode());
		
		//System.out.println(message);

	}
	
	public byte[] getRawMessage(String name) throws Exception
	{
		InputStream is = getClass().getResourceAsStream(name);
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		byte[] b = new byte[512];
		int read;
		while ((read = is.read(b)) != -1)
			os.write(b, 0, read);;
		return os.toByteArray();
	}
	
	public DnsMessage getMessage(String name) throws Exception
	{
		ByteBuffer buffer = ByteBuffer.wrap(getRawMessage(name));
		DnsMessage message = new DnsMessage();
		message.decode(buffer);
		return message;
	}
	
}
