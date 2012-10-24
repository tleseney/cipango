package org.cipango.sip;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.cipango.sip.SipParser.State;
import org.eclipse.jetty.util.StringUtil;
import org.junit.Before;
import org.junit.Test;

public class SipGeneratorTest
{
	private SipGenerator _generator;
	private ByteBuffer _buffer;
	
	@Before
	public void setUp()
	{
		_generator = new SipGenerator();
		_buffer = ByteBuffer.allocate(4096);
	}

	@Test
	public void testGenerateRequestError()
	{
		try
		{
			_generator.generateRequest(_buffer, "METHOD", null, null, null);
			fail();
		}
		catch (NullPointerException e)
		{
		}
	}
	
	@Test
	public void testGenerateRequestEmpty()
	{
		SipURIImpl uri = new SipURIImpl("host", 5061);

		uri.setUser("user");
		uri.setParameter("param", "value");
		
		_buffer.put(StringUtil.getUtf8Bytes("XXXX"));
		_buffer.position(2);
		_generator.generateRequest(_buffer, "METHOD", uri, null, null);

		assertEquals("XXMETHOD sip:user@host:5061;param=value SIP/2.0\r\n\r\n",
				toString());
	}
	
	@Test
	public void testGenerateRequestOverflow()
	{
		ByteBuffer buffer = ByteBuffer.allocate(15);

		try
		{
			_generator.generateRequest(buffer, "METHOD",
					new SipURIImpl("host", 5061), new SipFields(), null);
			fail();
		}
		catch (BufferOverflowException e)
		{
		}
	}
	
	@Test
	public void testGenerateRequest() throws IOException
	{
		SipFields headers = new SipFields();
		SipURIImpl uri = new SipURIImpl("biloxi.com", 5060);

		headers.add("Host", "localhost");
		headers.add("Header 1", "value1");
		headers.add("Header 2", "vale 2a");
		headers.add("Header 2", "vale 2b");
		headers.add("Header 3", "");
		headers.add("Header 4", "value 4a, value 4b, value 4c");

		uri.setUser("bob");
		uri.setParameter("uri-param", "uri-value");

		_generator.generateRequest(_buffer, "METHOD", uri, headers, null);

		Handler handler = parse(_buffer);

		assertEquals("METHOD", handler.getMethodOrVersion());
		assertEquals("sip:bob@biloxi.com:5060;uri-param=uri-value", handler.getUriOrStatus());
		assertEquals("SIP/2.0", handler.getVersionOrReason());
		
		Map<String, List<String>> parsed = handler.getHeaders();
		if (parsed == null)
			fail();

		Iterator<String> names = headers.getNames();
		while (names.hasNext())
		{
			String name = names.next();
			Iterator<String> values = headers.getValues(name);
			while (values.hasNext())
			{
				if (!parsed.get(name).remove(values.next()))
					fail();
			}
			parsed.remove(name);
		}
		
		if (!parsed.isEmpty())
			fail();
	}
	
	@Test
	public void testMergeHeaders() throws Exception
	{
		SipFields fields = new SipFields();
		fields.add("Accept", "INVITE");
		fields.add("Accept", "ACK");
		
		_generator.generateRequest(_buffer, "INVITE", new SipURIImpl("sip:cipango.org"), fields, null);
		String s = toString();
		//System.out.println("**" + s + "**");
		assertEquals("INVITE sip:cipango.org SIP/2.0\r\n"
				+ "Accept: INVITE, ACK\r\n\r\n", s);
		
	}
	
	@Test
	public void testNonMergeHeaders() throws Exception
	{
		SipFields fields = new SipFields();
		fields.add("Route", "<sip:cipango.org;lr>");
		fields.add("Route", "<sip:cipango.org:5062;lr>");
		
		_generator.generateRequest(_buffer, "INVITE", new SipURIImpl("sip:cipango.org"), fields, null);
		String s = toString();
		//System.out.println("**" + s + "**");
		assertEquals("INVITE sip:cipango.org SIP/2.0\r\n"
				+ "Route: <sip:cipango.org;lr>\r\n"
				+ "Route: <sip:cipango.org:5062;lr>\r\n\r\n", s);
		
	}

	@Test
	public void testGenerateResponse()
	{
		_buffer.put(StringUtil.getUtf8Bytes("XXXX"));
		_buffer.position(2);
		_generator.generateResponse(_buffer, 202, "All is OK", null, null);

		assertEquals("XXSIP/2.0 202 All is OK\r\n\r\n", toString());
	}
	
	@Test
	public void testGenerateResponseOverflow()
	{
		ByteBuffer buffer = ByteBuffer.allocate(30);

		try
		{
			_generator.generateResponse(buffer, 202, "All is full of love", null, null);
			fail();
		}
		catch (BufferOverflowException e)
		{
		}
	}
	
	public String toString()
	{
		return new String(_buffer.array(), 0, _buffer.position(), StringUtil.__UTF8_CHARSET);
	}
	
	private Handler parse(ByteBuffer buffer) throws IOException
	{
		Handler handler = new Handler();
		SipParser parser = new SipParser(handler);
		
		buffer.flip();
		while (!parser.isState(State.END) && buffer.hasRemaining())
		{
			int remaining = buffer.remaining();
			parser.parseNext(buffer);
			if (remaining == buffer.remaining())
				break;
		}

		if (parser.getState() != State.END)
			throw new IOException("Incomplete parsing: state is " + parser.getState());

		return handler;
	}
	
	private class Handler implements SipParser.SipMessageHandler
	{
		private String _methodOrVersion;
		private String _uriOrStatus;
		private String _versionOrReason;

		private Map<String, List<String>> _hdr = new Hashtable<String, List<String>>();
		
		public String getMethodOrVersion()
		{
			return _methodOrVersion;
		}

		public String getUriOrStatus()
		{
			return _uriOrStatus;
		}

		public String getVersionOrReason()
		{
			return _versionOrReason;
		}

		public Map<String, List<String>> getHeaders()
		{
			return _hdr;
		}

		public boolean startRequest(String method, String uri, SipVersion version) 
		{
			_methodOrVersion = method;
			_uriOrStatus = uri;
			_versionOrReason = version == null ? null : version.asString();
			
			return false;
		}
		
		@Override
		public boolean startResponse(SipVersion version, int status, String reason) throws ParseException
		{
			_methodOrVersion = version == null ? null : version.asString();
			_uriOrStatus = String.valueOf(status);
			_versionOrReason = reason;
			
			return false;
		}

		@Override
		public boolean parsedHeader(SipHeader header, String name, String value) 
		{
			if (!_hdr.containsKey(name))
				_hdr.put(name, new ArrayList<String>());
			_hdr.get(name).add(value == null? "": value);
			
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
			return true;
		}

		@Override
		public void badMessage(int status, String reason) 
		{
			_hdr = null;
		}
	}
}
