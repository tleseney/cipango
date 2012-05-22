package org.cipango.sip;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.cipango.util.StringUtil;
import org.eclipse.jetty.util.BufferUtil;
import org.junit.Test;

public class SipParserTest 
{
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
		parser.parseAll(buffer);
		buffer = BufferUtil.toBuffer("INVITE sip:alice@atlanta.com SIP/2.0\r\nCall-Id: toto\r\nContent-Length:11\r\n\r\nhello world");
		parser.parseAll(buffer);
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
	
	
	class Handler implements SipParser.EventHandler
	{
		public boolean startRequest(String method, String uri, String version) throws IOException 
		{
			//System.out.println("start request: " + method + " / " + uri + " / " + version);
			return false;
		}
		
		public boolean parsedHeader(SipHeader header, String name, String value) throws IOException
		{
			System.out.println(header + " - " + name + ":" + value);
			return false;
		}
		
		public boolean headerComplete() throws IOException
		{
			System.out.println("headerComplete");
			return false;
		}
		
		public boolean messageComplete(ByteBuffer content) throws IOException
		{
			byte[] b = new byte[content.remaining()];
			content.get(b);
		
			System.out.println("message complete: " + new String(b));
			return false;
		}
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
}
