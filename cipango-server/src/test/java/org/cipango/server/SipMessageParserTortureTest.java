// ========================================================================
// Copyright 2007-2012 NEXCOM Systems
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

package org.cipango.server;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ListIterator;

import javax.servlet.sip.Address;
import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipURI;

import org.cipango.server.nio.UdpConnector.MessageBuilder;
import org.cipango.server.processor.TransportProcessor;
import org.cipango.sip.AddressImpl;
import org.cipango.sip.SipParser;
import org.cipango.sip.SipParser.State;
import org.cipango.sip.Via;
import org.junit.Test;

/**
 * Test based on <a href="http://tools.ietf.org/html/rfc4475"> Session
 * Initiation Protocol (SIP) Torture Test Messages</a>.
 * 
 */
public class SipMessageParserTortureTest
{

	private static final File TORTURE_DIR = 
		new File(SipMessageParserTortureTest.class.getResource("/org/cipango/sip/torture").getFile());
	private static final File TORTURE_VALID_DIR = new File(TORTURE_DIR, "valid");
	private static final File TORTURE_INVALID_DIR = new File(TORTURE_DIR, "invalid");

	@Test
	public void testSipTortureValid() throws Exception
	{
		File[] testFiles = TORTURE_VALID_DIR.listFiles(new FilenameFilter()
		{
			public boolean accept(File dir, String name)
			{
				return name.endsWith(".dat");
			}
		});

		System.out.println("Test " + testFiles.length + " SipTorture tests (valid)");
		for (int i = 0; i < testFiles.length; i++)
		{
			// System.out.println(testFiles[i]);
			try
			{
				TestMessageBuilder builder = getBuilder(testFiles[i]);
				SipMessage message = builder.getMessage();
				
				assertTrue("Failed on SipTorture test " + testFiles[i].getName()
						+ " of raw message \n" 
						+ new String(getMessageRawBytes(testFiles[i]))
						+ "Got\n:" + message,
						new TransportProcessor(null).preValidateMessage(message));
				assertFalse("Failed on SipTorture test " + testFiles[i].getName() + ": on header " + builder.getFailedHeader()
						+ " of raw message \n" 
						+ new String(getMessageRawBytes(testFiles[i]))
						+ "Got\n:" + message, builder.isParseFailed());
//				assertTrue("Failed on SipTorture test " + testFiles[i] + ":\n" + message,
//						new ConnectorManager().preValidateMessage(message));
				if (message instanceof SipRequest)
				{
					SipRequest request = (SipRequest) message;
					if (request.getMaxForwards() < 0 && request.getMaxForwards() != -1)
						fail("Failed on SipTorture test " + testFiles[i] + ":\n" + message);
				}
			}
			catch (Exception e)
			{
				System.err.println("Failed on SipTorture test " + testFiles[i]);
				throw e;
			}

		}
	}

	@Test
	public void testSipTortureInvalid() throws Exception
	{
		File[] testFiles = TORTURE_INVALID_DIR.listFiles(new FilenameFilter()
		{
			public boolean accept(File dir, String name)
			{
				return name.endsWith("clerr.dat");
			}
		});

		System.out.println("Test " + testFiles.length + " SipTorture tests (invalid)");
		for (int i = 0; i < testFiles.length; i++)
		{
			try
			{
				TestMessageBuilder builder = getBuilder(testFiles[i]);
				SipMessage message = builder.getMessage();
				
				boolean invalid = builder.isParseFailed() || !new TransportProcessor(null).preValidateMessage(message);
								
				assertTrue("Failed on SipTorture test " + testFiles[i].getName() + ":\n" 
					+ new String(getMessageRawBytes(testFiles[i]))
					+ "\nGot\n" + message, invalid);
			}
			catch (Exception e)
			{
				System.out.println(e.getClass().toString() + " thrown on sip torture test "
						+ testFiles[i]);
				// e.printStackTrace();
				// assertTrue(false);
			}
		}
	}

	
	 /*
      * wsinv2.dat test is same as wsinv.dat except LWS are removed on via headers. 
      */
	@Test
	public void testTortuousInvite() throws Exception
	{
		
		File msgFile = new File(TORTURE_VALID_DIR, "wsinv2.dat");
		SipMessage message = getMessage(msgFile);
		Address contact = message.getAddressHeader("contact");
		assertEquals("Quoted string \"\"", contact.getDisplayName());
		assertEquals("newvalue", contact.getParameter("newparam"));
		assertEquals("", message.getHeader("subject"));
		// Test multiline
		assertEquals("newfangled value continued newfangled value",  message.getHeader("NewFangledHeader"));
		Via via = message.getTopVia();
		assertEquals("390skdjuw", via.getBranch());
		assertEquals("192.0.2.2", via.getHost());
		assertEquals("UDP", via.getTransport());
		message.getFrom();
		message.getTo();
		//System.out.println(message);
		//System.out.println(message.getTo().toString() + message.getFrom());
	}
	 

	/**
	 * 3.1.1.4. Escaped Nulls in URIs
	 */
	@Test
	public void testEscapeNull() throws Exception
	{
		File msgFile = new File(TORTURE_VALID_DIR, "escnull.dat");
		SipServletMessage message = getMessage(msgFile);
		ListIterator<Address> contacts = message.getAddressHeaders("contact");
		assertNotSame(contacts.next(), contacts.next());
	}

	/**
	 * 3.1.1.5. Use of % when it is not an escape
	 */
	@Test
	public void testEscape() throws Exception
	{
		File msgFile = new File(TORTURE_VALID_DIR, "esc02.dat");
		SipServletMessage message = getMessage(msgFile);
		ListIterator<Address> contacts = message.getAddressHeaders("contact");
		int nbContacts = 0;
		while (contacts.hasNext())
		{
			contacts.next();
			nbContacts++;
		}
		assertNotSame("REGISTER", message.getMethod());
		assertNotSame("%ZE", message.getTo().getDisplayName());
		assertEquals(2, nbContacts);
	}

	/**
	 * 3.1.1.8. Extra trailing octets in a UDP datagram
	 */
	@Test
	public void testBody() throws Exception
	{
		File msgFile = new File(TORTURE_VALID_DIR, "dblreq.dat");
		SipServletMessage message = getMessage(msgFile);

		assertEquals(0, message.getContentLength());
		assertNull(message.getContent());
	}

	@Test
	public void testNoReason() throws Exception
	{
		File msgFile = new File(TORTURE_VALID_DIR, "noreason.dat");
		SipServletResponse message = (SipServletResponse) getMessage(msgFile);
		assertEquals("", message.getReasonPhrase());
		//System.out.println(message);
	}

	/**
	 * 3.1.1.9. Semicolon separated parameters in URI user part
	 */
	@Test
	public void testReqUri() throws Exception
	{
		File msgFile = new File(TORTURE_VALID_DIR, "semiuri.dat");
		SipServletRequest message = (SipServletRequest) getMessage(msgFile);
		assertEquals("user;par=u@example.net", ((SipURI) message.getRequestURI()).getUser());
	}
	
	@Test
	public void testBadaspec() throws Exception
	{
		File msgFile = new File(TORTURE_VALID_DIR, "badaspec.dat");
		SipServletMessage message = getMessage(msgFile);
		Address to = message.getTo();
		//to.removeParameter("tag");
		System.out.println(to);
		AddressImpl expected = new AddressImpl("\"Watson, Thomas\" <sip:t.watson@example.org>");
		expected.parse();
		assertEquals(expected, to);
	}


	/**
	 * 3.1.2.13. Failure to Enclose name-addr URI in <>
	 */
	@Test
	public void testRegbadct() throws Exception
	{
		File msgFile = new File(TORTURE_VALID_DIR, "regbadct.dat");
		SipServletRequest message = (SipServletRequest) getMessage(msgFile);
		// System.out.println(message.getAddressHeader("contact"));
		assertEquals("sip:user@example.com?Route=%3csip:sip.example.com%3e", message
				.getAddressHeader("contact").getURI().toString());

	}

	private SipMessage getMessage(File file) throws IOException
	{
		TestMessageBuilder messageBuilder = getBuilder(file);
		
		if (messageBuilder.isParseFailed())
			fail("Parsing failed on test " + file.getName() + ": " + messageBuilder.getFailedHeader()
					+ " of message \n" + messageBuilder.getMessage());
		
		SipMessage message = messageBuilder.getMessage();
				
		return message;
	}
	
	private TestMessageBuilder getBuilder(File file) throws IOException
	{

		ByteBuffer b = ByteBuffer.wrap(getMessageRawBytes(file));
		TestMessageBuilder messageBuilder = new TestMessageBuilder();

		SipParser parser = new SipParser(messageBuilder);
		parser.parseNext(b);
		
		if (parser.getState() != State.END)
			throw new IOException("Parse not ended: state is " + parser.getState());

		return messageBuilder;
	}
	
	private byte[] getMessageRawBytes(File file) throws IOException
	{
		FileInputStream is = new FileInputStream(file);
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int read;
		while ((read = is.read(buffer)) != -1)
		{
			os.write(buffer, 0, read);
		}
		return os.toByteArray();
	}
	
	
	class TestMessageBuilder extends MessageBuilder
	{
		private String _reason;
		
		@Override
		public void badMessage(int status, String reason)
		{
			super.badMessage(status, reason);
			_reason = reason;
		}

		public boolean isParseFailed()
		{
			return _reason != null;
		}



		public String getFailedHeader()
		{
			return _reason;
		}
	}
}
