package org.cipango.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ListIterator;

import javax.servlet.sip.Address;
import javax.servlet.sip.Parameterable;
import javax.servlet.sip.SipServletMessage.HeaderForm;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.URI;

import org.cipango.server.SipMessageParserTortureTest.TestMessageBuilder;
import org.cipango.sip.AddressImpl;
import org.cipango.sip.ParameterableImpl;
import org.cipango.sip.SipHeader;
import org.cipango.sip.SipMethod;
import org.cipango.sip.SipParser;
import org.cipango.sip.SipParser.State;
import org.cipango.sip.SipURIImpl;
import org.junit.Test;

public class SipMessageTest
{
	
	private static final String REGISTER = "REGISTER sip:nexcom.fr SIP/2.0\r\n"
		+ "Via: SIP/2.0/UDP 192.168.1.2:5061;branch=z9hG4bKnashds7\r\n"
		+ "Max-Forwards: 70\r\n"
		+ "From: Bob <sips:bob@nexcom.fr>;tag=a73kszlfl\r\n"
		+ "To: Bob <sips:bob@nexcom.fr>\r\n"
		+ "Call-ID: 1j9FpLxk3uxtm8tn@192.168.1.2\r\n"
		+ "CSeq: 1 REGISTER\r\n"
		+ "Expires: 0\r\n"
		+ "Contact: <sip:127.0.0.1:5060;transport=TCP>\r\n"
		+ "Content-Length: 0\r\n\r\n";
	
	public static final String INVITE = "INVITE sips:ss2.biloxi.example.com SIP/2.0\r\n"
			+ "Via: SIP/2.0/TLS client.biloxi.example.com:5061;branch=z9hG4bKnashds7\r\n"
			+ "Max-Forwards: 70\r\n"
			+ "From: Bob <sips:bob@biloxi.example.com>;tag=a73kszlfl\r\n"
			+ "To: Alice <sips:alice@biloxi.example.com>\r\n"
			+ "Call-ID: 1j9FpLxk3uxtm8tn@biloxi.example.com\r\n"
			+ "CSeq: 1 INVITE\r\n"
			+ "Expires: 0\r\n"
			+ "Contact: <sip:127.0.0.1:5060;transport=TCP>\r\n"
			+ "Accept: application/sdp;level=1, application/x-private, text/html\r\n"
			+ "Content-Length: 0\r\n\r\n";
	
	@Test
	public void testCompactHeaders() throws Exception
	{
		SipRequest request = new SipRequest();
		request.setRequestURI(new SipURIImpl("sip:cipango.org"));
		request.setMethod(SipMethod.REGISTER, SipMethod.REGISTER.asString());
		request._fields.add(SipHeader.CALL_ID.asString(), "aa");
		assertEquals("aa", request.getCallId());
		assertEquals("aa", request.getHeader("i"));
		
		request.addHeader("a", "<sip:accept-contact>");
		assertEquals("<sip:accept-contact>", request.getHeader(SipHeader.ACCEPT_CONTACT.asString()));
		assertEquals("<sip:accept-contact>", request.getHeader("a"));
		assertTrue(request.getHeaders("a").hasNext());
		
		AddressImpl addr = new AddressImpl("<sip:contact;lr>");
		addr.parse();
		request.addAddressHeader("m", addr, false);
		assertEquals(addr, request.getAddressHeader(SipHeader.CONTACT.asString()));
		assertEquals(addr, request.getAddressHeader("m"));
		assertTrue(request.getAddressHeaders("m").hasNext());
		
		request.removeHeader("m");
		assertNull(request.getAddressHeader(SipHeader.CONTACT.asString()));
		assertNull(request.getAddressHeader("m"));
		assertFalse(request.getAddressHeaders("m").hasNext());
		
		request.setAddressHeader("m", addr);
		assertEquals(addr, request.getAddressHeader(SipHeader.CONTACT.asString()));
		assertEquals(addr, request.getAddressHeader("m"));
		
		request.setHeader("s", "call");
		assertEquals("call", request.getHeader(SipHeader.SUBJECT.asString()));
		assertEquals("call", request.getHeader("s"));
		
		request.setHeaderForm(HeaderForm.COMPACT);
		String compactForm = request.toString();
		request.setHeaderForm(HeaderForm.LONG);
		String longForm = request.toString();
		assertFalse(compactForm.equals(longForm));
		assertTrue(compactForm.contains("a:"));
		assertTrue(longForm.contains("Accept-Contact:"));
	}
	
	@Test
	public void testReadOnly() throws Exception
	{
		SipRequest request = (SipRequest) getMessage(INVITE);
		assertReadOnly(request.getFrom());
		assertReadOnly(request.getTo());
		assertReadOnly(request.getAddressHeader("from"));
		assertReadOnly(request.getParameterableHeader("Via"));
		assertReadOnly(request.getAddressHeaders("Contact").next());
		assertReadOnly(request.getParameterableHeaders("Via").next());
		// Note: contact header is tested in getContact()
		
	}
	
	private void assertReadOnly(Parameterable parameterable)
	{
		try { parameterable.removeParameter("param"); fail("Should be read-only"); } catch(Exception e) {}
		try { parameterable.setParameter("a", "b"); fail("Should be read-only"); } catch(Exception e) {}
		try { parameterable.setValue("dd"); fail("Should be read-only"); } catch(Exception e) {}
	}
	
	private void assertReadOnly(Address address)
	{
		assertReadOnly((Parameterable) address);
		try { address.setDisplayName("Bob"); fail("Should be read-only"); } catch(Exception e) {}
		URI uri = address.getURI();
		try { uri.setParameter("a", "b"); fail("Should be read-only"); } catch(Exception e) {}
		try { uri.removeParameter("a"); fail("Should be read-only"); } catch(Exception e) {}
	}
	
	@Test
	public void testParameterable() throws Exception
	{
		SipRequest request = (SipRequest) getMessage(INVITE);
		Parameterable p = request.getParameterableHeader("from");
		assertEquals("Bob <sips:bob@biloxi.example.com>", p.getValue());
		assertEquals("a73kszlfl", p.getParameter("tag"));
		
		p = request.getParameterableHeader("Via");
		assertEquals("z9hG4bKnashds7", p.getParameter("branch"));
		assertEquals("SIP/2.0/TLS client.biloxi.example.com:5061", p.getValue());
		
		p = request.getParameterableHeader("Accept");
		assertEquals("application/sdp", p.getValue());
		assertEquals("1", p.getParameter("level"));
		
		ListIterator<? extends Parameterable> it = request.getParameterableHeaders("Accept");
		while (it.hasNext())
		{
			int index = it.nextIndex();
			p = (Parameterable) it.next();
			switch (index)
			{
			case 0:
				assertEquals("application/sdp", p.getValue());
				assertEquals("1", p.getParameter("level"));
				break;
			case 1:
				assertEquals("application/x-private", p.getValue());
				assertFalse(p.getParameterNames().hasNext());
				break;
			case 2:
				assertEquals("text/html", p.getValue());
				assertFalse(p.getParameterNames().hasNext());
				break;
			default:
				fail("Too much parameterable");
				break;
			}
		}
		assertEquals(3, it.nextIndex());
		
		request.setParameterableHeader("P-unknown", new ParameterableImpl("unknow;a=1"));
		p = request.getParameterableHeader("P-unknown");
		assertEquals("1", p.getParameter("a"));
		assertEquals("unknow", p.getValue());
		
		request.addParameterableHeader("Event", new ParameterableImpl("dialog;id=3"), true);
		p = request.getParameterableHeader("Event");
		assertEquals("3", p.getParameter("id"));
		assertEquals("dialog", p.getValue());
	}
	
	@Test
	public void testContact() throws Exception
	{
		SipRequest request = (SipRequest) getMessage(INVITE);
		Address contact = request.getAddressHeader("Contact");
		assertEquals("<sip:127.0.0.1:5060;transport=TCP>", contact.toString());
		contact.setDisplayName("Bob");
		contact.setParameter("isfocus", "");
		assertEquals("Bob", contact.getDisplayName());
		SipURI uri = (SipURI) contact.getURI();
		uri.setUser("bob");
		assertEquals("bob", uri.getUser());
		try { uri.setHost("bad"); fail(); } catch (IllegalStateException e) {}
		try { uri.setLrParam(true); fail(); } catch (IllegalStateException e) {}
		try { uri.setMAddrParam("bad"); fail(); } catch (IllegalStateException e) {}
		try { uri.setMethodParam("Bad"); fail(); } catch (IllegalStateException e) {}
		try { uri.setTTLParam(2); fail(); } catch (IllegalStateException e) {}
		try { uri.setParameter("lr", ""); fail(); } catch (IllegalStateException e) {}
		try { uri.removeParameter("Maddr"); fail(); } catch (IllegalStateException e) {}
		uri.setParameter("transport", "UDP");
		assertEquals("UDP", uri.getParameter("transport"));
		assertEquals("Bob <sip:bob@127.0.0.1:5060;transport=UDP>;isfocus", contact.toString());
		
		// Full read-only on committed
		request.setCommitted(true);
		contact = request.getAddressHeader("Contact");
		uri = (SipURI) contact.getURI();
		try { contact.setDisplayName("bad"); fail(); } catch (IllegalStateException e) {}
		try { uri.setUser("bad"); fail(); } catch (IllegalStateException e) {}
		
		// Full writable on REGISTER
		request = (SipRequest) getMessage(REGISTER);
		contact = request.getAddressHeader("Contact");
		uri = (SipURI) contact.getURI();
		contact.setDisplayName("Bob");
		uri.setHost("nexcom.fr");
		uri.setPort(5062);
		uri.removeParameter("transport");
		uri.setUser("bob");
		assertEquals("Bob <sip:bob@nexcom.fr:5062>", contact.toString());		
	}
	
	
	@Test
	public void testMultipleLineHeaders() throws Exception
	{
		InputStream is = getClass().getResourceAsStream("/org/cipango/server/MultipleLineRequest.txt");		
		SipMessage message = getMessage(is);
		String toString = message.toString();
		//System.out.println(message);
	
		assertEquals(1, count(toString, "Accept:"));
		assertEquals(3, count(toString, "Via:"));
		assertEquals(2, count(toString, "UnknownHeader:"));
		assertEquals(2, count(toString, "Proxy-Authenticate:"));
		assertEquals(2, count(toString, "Contact:"));
		
		ListIterator<String> it = message.getHeaders("UnknownHeader");
		while (it.hasNext())
		{
			int index = it.nextIndex();
			String value = (String) it.next();
			if (index == 0)
				assertEquals("valWith,Comma", value);
			else
				assertEquals("val2", value);
		}
	}
	
	/**
	 * Test for CIPANGO-205: Can't find Contact Header in REGISTER if a contact parameter is quoted
	 */
	@Test
	public void testQuotedContact() throws Exception
	{
		InputStream is = getClass().getResourceAsStream("/org/cipango/server/contact.dat");		
		SipMessage message = getMessage(is);

		//System.out.println(message);
		Address contact = message.getAddressHeader("contact");
		assertNotNull(contact);
		assertEquals("<sip:b@df7jal23ls0d.invalid;rtcweb-breaker=no;transport=ws>;expires=200;click2call=no;+g.oma.sip-im;+audio;language=\"en,fr\"",
				contact.toString());
	}
	
	private int count(String string, String token)
	{
		int i = 0;
		int index = -1;
		while ((index = string.indexOf(token, index + 1)) != -1)
			i++;
		return i;
	}
	
	public static SipMessage getMessage(InputStream is) throws Exception
	{
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int read;
		while ((read = is.read(buffer)) != -1)
		{
			os.write(buffer, 0, read);
		}
		
		return getMessage(os.toString());
	}
	
	public static SipMessage getMessage(String msg) throws Exception
	{
		TestMessageBuilder messageBuilder = new TestMessageBuilder();
		ByteBuffer b = ByteBuffer.wrap(msg.getBytes());
		SipParser parser = new SipParser(messageBuilder);
		parser.parseNext(b);
		
		if (parser.getState() != State.END)
			throw new IOException("Parse not ended: state is " + parser.getState());
		
		if (messageBuilder.isParseFailed())
			fail("Parsing failed on message \n" + messageBuilder.getMessage());
		return messageBuilder.getMessage();
	}

	

}
