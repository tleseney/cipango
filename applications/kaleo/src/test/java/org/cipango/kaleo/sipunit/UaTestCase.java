// ========================================================================
// Copyright 2009 NEXCOM Systems
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
package org.cipango.kaleo.sipunit;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ListIterator;
import java.util.Properties;

import javax.sip.address.AddressFactory;
import javax.sip.header.Header;
import javax.sip.header.HeaderFactory;
import javax.sip.message.Message;

import org.cafesip.sipunit.SipPhone;
import org.cafesip.sipunit.SipStack;
import org.cafesip.sipunit.SipTestCase;
import org.cipango.kaleo.xcap.XcapUri;

public abstract class UaTestCase extends SipTestCase
{
		
	protected static HeaderFactory __headerFactory;
	protected static AddressFactory __addressFactory;

	private SipStack _sipStack;
	private SipPhone _alicePhone;
	private SipPhone _bobPhone;

	protected Properties _properties;

	public UaTestCase()
	{
		try
		{
			_properties = new Properties();
			_properties.load(getClass().getClassLoader().getResourceAsStream("integrationTests.properties"));
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	public void setContent(String xcapUri) throws IOException
	{
		XcapUri uri = new XcapUri(xcapUri, "/");
		String doc = uri.getDocumentSelector().replace(":", "%3A");
		File sourceFile = new File("target/test-classes/xcap-root", doc);
		if (!sourceFile.exists())
			sourceFile = new File("target/test-classes/xcap-root", doc.replace("@", "%40"));
		InputStream is = new FileInputStream(sourceFile);
		File outputFile = new File("target/test-data", doc.replace("@", "%40"));
		outputFile.getParentFile().mkdirs();
		ByteArrayOutputStream os = new ByteArrayOutputStream();		
		
		int read;
		byte[] buffer = new byte[1024];
		while ((read = is.read(buffer)) != -1) {
			os.write(buffer, 0, read);
		}
		String content = new String(os.toByteArray());
		content = content.replaceAll("http://xcap.cipango.org", getHttpXcapUri());
		FileOutputStream fos = new FileOutputStream(outputFile);
		fos.write(content.getBytes());
		fos.close();
		is.close();
	}

	public String getProtocol()
	{
		return _properties.getProperty("sipunit.test.protocol");
	}

	public int getInt(String property)
	{
		return Integer.parseInt(_properties.getProperty(property));
	}

	public int getLocalPort()
	{
		return getInt("sipunit.test.port");
	}

	public int getRemotePort()
	{
		return getInt("sipunit.proxy.port");
	}

	public String getRemoteHost()
	{
		return _properties.getProperty("sipunit.proxy.host");
	}

	public String getAliceUri()
	{
		return "sip:alice@" + _properties.getProperty("sipunit.test.domain");
	}
	
	public String getBobUri()
	{
		return "sip:bob@" + _properties.getProperty("sipunit.test.domain");
	}

	public String getTo()
	{
		return "sip:sipServlet@" + _properties.getProperty("sipunit.test.domain");
	}
	
	public String getHttpXcapUri()
	{
		return _properties.getProperty("http.xcap.uri");
	}
	
	public SipPhone getAlicePhone()
	{
		return _alicePhone;
	}
	
	public SipPhone getBobPhone()
	{
		try {
			if (_bobPhone == null)
			{
				_bobPhone = _sipStack.createSipPhone(getRemoteHost(), getProtocol(),
						getRemotePort(), getBobUri());
			}
			return _bobPhone;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void setUp() throws Exception
	{
		_sipStack = new SipStack(getProtocol(), getLocalPort(), (Properties) _properties.clone());
		SipStack.setTraceEnabled(_properties.getProperty("sipunit.trace").equalsIgnoreCase("true"));
		_alicePhone =  _sipStack.createSipPhone(getRemoteHost(), getProtocol(),
				getRemotePort(), getAliceUri());
		if (__headerFactory == null) 
		{
			__headerFactory = _sipStack.getHeaderFactory();
			__addressFactory = _sipStack.getAddressFactory();
		}
	}

	public void tearDown() throws Exception
	{
		_alicePhone.dispose();
		if (_bobPhone != null)
			_bobPhone.dispose();
		_sipStack.dispose();
	}
	
    /**
     * Asserts that the given SIP message contains at least one occurrence of
     * the specified header and that at least one occurrence of this header
     * contains the given value. The assertion fails if no occurrence of the
     * header contains the value or if the header is not present in the mesage.
     * 
     * @param sipMessage
     *            the SIP message.
     * @param header
     *            the string identifying the header as specified in RFC-3261.
     * @param value
     *            the string value within the header to look for. An exact
     *            string match is done against the entire contents of the
     *            header. The assertion will pass if any part of the header
     *            matches the value given.
     */
    public void assertHeaderContains(Message sipMessage, String header,
            String value)
    {
        assertHeaderContains(null, sipMessage, header, value); // value is case
        // sensitive?
    }

    /**
     * Asserts that the given SIP message contains at least one occurrence of
     * the specified header and that at least one occurrence of this header
     * contains the given value. The assertion fails if no occurrence of the
     * header contains the value or if the header is not present in the mesage.
     * Assertion failure output includes the given message text.
     * 
     * @param msg
     *            message text to output if the assertion fails.
     * @param sipMessage
     *            the SIP message.
     * @param header
     *            the string identifying the header as specified in RFC-3261.
     * @param value
     *            the string value within the header to look for. An exact
     *            string match is done against the entire contents of the
     *            header. The assertion will pass if any part of the header
     *            matches the value given.
     */
    public void assertHeaderContains(String msg, Message sipMessage,
            String header, String value)
    {
        assertNotNull("Null assert object passed in", sipMessage);
        ListIterator<Header> l = sipMessage.getHeaders(header);
        while (l.hasNext())
        {
            String h = ((Header) l.next()).toString();
            if (h.indexOf(value) != -1)
                return;
        }

        fail(msg);
    }
    
	public void assertBetween(int min, int max, int actual)
	{
		if (actual > max)
			fail("Got " + actual + " when max is " + max);
		else if (actual < min)
			fail("Got " + actual + " when min is " + min);
	}

}
