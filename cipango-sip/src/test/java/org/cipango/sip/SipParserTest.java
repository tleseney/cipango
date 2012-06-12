package org.cipango.sip;

import java.nio.ByteBuffer;

import org.cipango.sip.SipParser.State;
import org.cipango.util.StringUtil;
import org.eclipse.jetty.util.BufferUtil;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.*;
import static org.junit.Assert.assertEquals;

public class SipParserTest 
{
	public void parseAll(SipParser parser, ByteBuffer buffer)
	{
		if (parser.isState(State.END))
			parser.reset();
		if (!parser.isState(State.START))
			throw new IllegalStateException("!START");
		
		while (!parser.isState(State.END) && buffer.hasRemaining())
		{
			int remaining = buffer.remaining();
			parser.parseNext(buffer);
			if (remaining == buffer.remaining())
				break;
		}
	}
	
	@Test
	public void testLine0() throws Exception
	{
		ByteBuffer buffer = BufferUtil.toBuffer("INVITE sip:atlanta.com SIP/2.0\r\n" + "\r\n");
		
		Handler handler = new Handler();
		SipParser parser = new SipParser(handler);
		
		parseAll(parser, buffer);
		assertEquals("INVITE", _methodOrVersion);
		assertEquals("sip:atlanta.com", _uriOrStatus);
		assertEquals("SIP/2.0", _versionOrReason);
		assertEquals(-1, _h);
	}
	
	@Test
	public void testLine1() throws Exception
	{
		ByteBuffer buffer = BufferUtil.toBuffer("INVITE sip:alice@\ua743 SIP/2.0\r\n" + "\r\n", StringUtil.__UTF8_CHARSET);
		
		Handler handler = new Handler();
		SipParser parser = new SipParser(handler);
		
		parseAll(parser, buffer);
		assertEquals("INVITE", _methodOrVersion);
		assertEquals("sip:alice@\ua743", _uriOrStatus);
		assertEquals("SIP/2.0", _versionOrReason);
		assertEquals(-1, _h);
	}
	
	@Test
	public void testHeaderParse() throws Exception
	{
		ByteBuffer buffer= BufferUtil.toBuffer(
                "MESSAGE sip:bob@biloxi.com SIP/2.0\r\n" +
                        "Host: localhost\r\n" +
                        "Header1: value1\r\n" +
                        "Header 2  :   value 2a  \r\n" +
                        "                    value 2b  \r\n" +
                        "Header3: \r\n" +
                        "Header4 \r\n" +
                        "  value4\r\n" +
                        "Server5 : notServer\r\n" +
                        "Host Header: notHost\r\n" +
                "\r\n");
		Handler handler = new Handler();
		SipParser parser = new SipParser(handler);
		
		parseAll(parser, buffer);
		
		assertEquals("MESSAGE", _methodOrVersion);
		assertEquals("sip:bob@biloxi.com", _uriOrStatus);
		assertEquals("SIP/2.0", _versionOrReason);
		
		assertEquals("Host", _hdr[0]);
        assertEquals("localhost", _val[0]);
        assertEquals("Header1", _hdr[1]);
        assertEquals("value1", _val[1]);
        assertEquals("Header 2", _hdr[2]);
        assertEquals("value 2a value 2b", _val[2]);
        assertEquals("Header3", _hdr[3]);
        assertEquals(null, _val[3]);
        assertEquals("Header4", _hdr[4]);
        assertEquals("value4", _val[4]);
        assertEquals("Server5", _hdr[5]);
        assertEquals("notServer", _val[5]);
        assertEquals("Host Header", _hdr[6]);
        assertEquals("notHost", _val[6]);
        assertEquals(6, _h);
	}
	
	@Test
	public void testSplitHeaderParse()
	{
		ByteBuffer buffer= BufferUtil.toBuffer(
                "XXXXMESSAGE sip:bob@biloxi.com SIP/2.0\r\n" +
                        "Host: localhost\r\n" +
                        "Header1: value1\r\n" +
                        "Header 2  :   value 2a  \r\n" +
                        "                    value 2b  \r\n" +
                        "Header3: \r\n" +
                        "Header4 \r\n" +
                        "  value4\r\n" +
                        "Server5 : notServer\r\n" +
                        "Host Header: notHost\r\n" +
                "\r\nZZZZ");
		
		buffer.position(2);
		buffer.limit(buffer.capacity()-2);
		buffer = buffer.slice();
		
		for (int i = 0; i < buffer.capacity() - 4; i++)
		{
			Handler handler = new Handler();
			SipParser parser = new SipParser(handler);
			
			buffer.position(2);
			buffer.limit(2+i);
			
			if (!parser.parseNext(buffer))
			{
				assertEquals(0, buffer.remaining());
				
				buffer.limit(buffer.capacity()-2);
				parser.parseNext(buffer);
			}
			
			assertEquals("MESSAGE", _methodOrVersion);
			assertEquals("sip:bob@biloxi.com", _uriOrStatus);
			assertEquals("SIP/2.0", _versionOrReason);
			
			assertEquals("Host", _hdr[0]);
	        assertEquals("localhost", _val[0]);
	        assertEquals("Header1", _hdr[1]);
	        assertEquals("value1", _val[1]);
	        assertEquals("Header 2", _hdr[2]);
	        assertEquals("value 2a value 2b", _val[2]);
	        assertEquals("Header3", _hdr[3]);
	        assertEquals(null, _val[3]);
	        assertEquals("Header4", _hdr[4]);
	        assertEquals("value4", _val[4]);
	        assertEquals("Server5", _hdr[5]);
	        assertEquals("notServer", _val[5]);
	        assertEquals("Host Header", _hdr[6]);
	        assertEquals("notHost", _val[6]);
	        assertEquals(6, _h);
		}
	}
	
	@Test
	public void testStreamHeaderParse()
	{
		ByteBuffer buffer= BufferUtil.toBuffer(
                "MESSAGE sip:bob@biloxi.com SIP/2.0\r\n" +
                        "Host: localhost\r\n" +
                        "Header1: value1\r\n" +
                        "Header 2  :   value 2a  \r\n" +
                        "                    value 2b  \r\n" +
                        "Header3: \r\n" +
                        "Header4 \r\n" +
                        "  value4\r\n" +
                        "Server5 : notServer\r\n" +
                        "Host Header: notHost\r\n" +
                "\r\n");

		Handler handler = new Handler();
		SipParser parser = new SipParser(handler);

		for (int i = 0; i < buffer.capacity() - 1; i++)
		{
			buffer.limit(i);
			parser.parseNext(buffer);
			assertFalse(parser.isState(State.END));
		}
		
		buffer.limit(buffer.capacity());
		parser.parseNext(buffer);
		System.out.println(parser);
		assertTrue(parser.isState(State.END));
		
		assertEquals("MESSAGE", _methodOrVersion);
		assertEquals("sip:bob@biloxi.com", _uriOrStatus);
		assertEquals("SIP/2.0", _versionOrReason);
		
		assertEquals("Host", _hdr[0]);
        assertEquals("localhost", _val[0]);
        assertEquals("Header1", _hdr[1]);
        assertEquals("value1", _val[1]);
        assertEquals("Header 2", _hdr[2]);
        assertEquals("value 2a value 2b", _val[2]);
        assertEquals("Header3", _hdr[3]);
        assertEquals(null, _val[3]);
        assertEquals("Header4", _hdr[4]);
        assertEquals("value4", _val[4]);
        assertEquals("Server5", _hdr[5]);
        assertEquals("notServer", _val[5]);
        assertEquals("Host Header", _hdr[6]);
        assertEquals("notHost", _val[6]);
        assertEquals(6, _h);	
	}
	
	@Test
	public void testResetParse()
	{
		Handler handler = new Handler();
		SipParser parser = new SipParser(handler);
		
		for (int i = 0; i < 100; i++)
		{
			ByteBuffer buffer= BufferUtil.toBuffer(
					"MESSAGE sip:bob@biloxi.com SIP/2.0\r\n" +
	                "X-Iteration: " + i + "\r\n" +
	                "\r\n");
			parser.reset();
			parser.parseNext(buffer);
			
			assertEquals(i, Integer.parseInt(_val[0]));
		}
	}
	
	@Test
	public void testNoContentLength()
	{
		Handler handler = new Handler();
		SipParser parser = new SipParser(handler);
		
		ByteBuffer buffer= BufferUtil.toBuffer(
				"MESSAGE sip:bob@biloxi.com SIP/2.0\r\n" +
                "Call-ID: foo\r\n" +
				"\r\n" + 
                "content with undefined length");
		
		parser.parseNext(buffer);
		assertEquals("content with undefined length", _content);
	}
	
	@Test
	public void testContentLength()
	{
		Handler handler = new Handler();
		SipParser parser = new SipParser(handler);
		
		ByteBuffer buffer= BufferUtil.toBuffer(
				"MESSAGE sip:bob@biloxi.com SIP/2.0\r\n" +
                "Content-Length: 15\r\n" +
				"\r\n" + 
                "contentEndsHereXXXXXXXXXXXXXXXXXXXXXXXXXX");
		
		parser.parseNext(buffer);
		assertEquals("contentEndsHere", _content);
	}
	
	@Test
	public void testNoURI()
	{
		Handler handler = new Handler();
		SipParser parser = new SipParser(handler);
		
		ByteBuffer buffer= BufferUtil.toBuffer(
				"MESSAGE\r\n" +
                "Content-Length: 0\r\n" +
				"\r\n");
		
		parser.parseNext(buffer);
		assertNull(_methodOrVersion);
		assertEquals("No URI", _bad);
		assertFalse(buffer.hasRemaining());
		assertTrue(parser.isState(State.END));
	}
	
	public void testMulti()
	{
		Handler handler = new Handler();
		SipParser parser = new SipParser(handler);
		
		ByteBuffer buffer= BufferUtil.toBuffer(
				"MESSAGE sip:bob@biloxi.com SIP/2.0\r\n" +
                "Content-Length: 8\r\n" +
				"\r\n" + 
                "content1" +
                "INVITE sip:bob@biloxi.com SIP/2.0\r\n" +
                "Content-Length: 8\r\n" +
				"\r\n" + 
                "content2" +
                "\r\nREGISTER sip:bob@biloxi.com SIP/2.0\r\n" +
                "Content-Length: 8\r\n" +
				"\r\n" + 
                "content3");
		
		parser.parseNext(buffer);
		assertEquals("MESSAGE", _methodOrVersion);
		assertEquals("content1", _content);
		
		parser.reset();
		init();
		
		assertEquals("INVITE", _methodOrVersion);
		assertEquals("content2", _content);
		
		parser.reset();
		init();
		
		assertEquals("REGISTER", _methodOrVersion);
		assertEquals("content3", _content);
	}
	
	@Before
	public void init()
	{
		_methodOrVersion = null;
		_uriOrStatus = null;
		_versionOrReason = null;
		_hdr = null;
		_val = null;
		_h = 0;
	}
	
	private String _methodOrVersion;
	private String _uriOrStatus;
	private String _versionOrReason;
	
	private String[] _hdr;
	private String[] _val;
	private int _h;
	
	private String _content;
	
	private String _bad;
	
	private class Handler implements SipParser.SipMessageHandler
	{
		private boolean request;
		
		public boolean startRequest(String method, String uri, SipVersion version) 
		{
			request = true;
            _h= -1;
            _hdr= new String[9];
            _val= new String[9];
			_methodOrVersion = method;
			_uriOrStatus = uri;
			_versionOrReason = version == null ? null : version.asString();
			_bad = null;
			
			return false;
		}

		@Override
		public boolean parsedHeader(SipHeader header, String name, String value) 
		{
			_hdr[++_h] = name;
			_val[_h] = value;
			
			return false;
		}

		@Override
		public boolean headerComplete() 
		{
			return false;
		}

		@Override
		public boolean messageComplete(ByteBuffer content) 
		{
			_content = BufferUtil.toString(content, StringUtil.__UTF8_CHARSET);
			return false;
		}

		@Override
		public void badMessage(int status, String reason) 
		{
			_bad = reason;
		}
		
	}
	
	/*
	//@Test
	public void testParseStream() throws Exception 
	{
		SipParser parser = new SipParser(new Handler());
		
		
		byte[] b = "INVITE sip:alice@atlanta.com SIP/2.0\r\nCall-Id: toto\r\nContent-Length:11\r\n\r\nhello world".getBytes(StringUtil.__UTF8);
		
		ByteBuffer buffer = BufferUtil.allocateDirect(1024);
		
		for (int i = 0; i < b.length; i++)
		{
			int position = BufferUtil.flipToFill(buffer);
			
			buffer.put((byte)b[i]);
			
			BufferUtil.flipToFlush(buffer, position);

			parser.parseNext(buffer);
			//System.out.println(BufferUtil.toString(buffer, buffer.position(), buffer.remaining(), StringUtil.__UTF8_CHARSET));
			//System.out.println(parser.getState());
		}
	}
	
	@Test
	public void testParse() throws Exception
	{
		SipParser parser = new SipParser(new Handler());
		ByteBuffer buffer = BufferUtil.toBuffer("INVITE sip:alice@atlanta.com SIP/2.0\r\nCall-Id: toto\r\nContent-Length:11\r\n\r\nhello world");
		parser.parseNext(buffer);
		buffer = BufferUtil.toBuffer("INVITE sip:alice@atlanta.com SIP/2.0\r\nCall-Id: toto\r\nContent-Length:11\r\n\r\nhello world");
		parser.parseNext(buffer);
		System.out.println(parser.getState());
	}
	
	
	//@Test
	public void testPerf() throws Exception
	{
		SipParser parser = new SipParser(new Handler());
		ByteBuffer buffer = BufferUtil.toBuffer(_msg);
		
		for (int i = 0; i < 10000; i++)
		{
			parser.parseNext(buffer);
			buffer.position(0);
			parser.reset();
		}
		
		long start = System.currentTimeMillis();
		
		int n = 50000;
		
		for (int i = 0; i < n; i++)
		{
			parser.parseNext(buffer);
			buffer.position(0);
			parser.reset();
		}
		
		System.out.println( 1l * n /(System.currentTimeMillis() - start) * 1000);
	}
	
	
	
	String _msg = 
	        "REGISTER sip:127.0.0.1:5070 SIP/2.0\r\n"
	        + "Call-ID: c117fdfda2ffd6f4a859a2d504aedb25@127.0.0.1\r\n"
	        + "CSeq: 2 REGISTER\r\n"
	        + "From: <sip:cipango@cipango.org>;tag=9Aaz+gQAAA\r\n"
	        + "To: <sip:cipango@cipango.org>\r\n"
	        + "Via: SIP/2.0/UDP 127.0.0.1:6010\r\n"
	        + "Max-Forwards: 70\r\n"
	        + "User-Agent: Test Script\r\n"
	        + "Contact: \"Cipango\" <sip:127.0.0.1:6010;transport=udp>\r\n"
	        + "Allow: INVITE, ACK, BYE, CANCEL, PRACK, REFER, MESSAGE, SUBSCRIBE\r\n"
	        + "MyHeader: toto\r\n"
	        + "x x x      : f \r\n"
	        + "Content-Length: 0\r\n\r\n";
	        
	        */
}
