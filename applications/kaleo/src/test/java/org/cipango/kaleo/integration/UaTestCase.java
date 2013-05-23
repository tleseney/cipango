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
package org.cipango.kaleo.integration;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.servlet.sip.Address;
import javax.servlet.sip.SipURI;

import junit.framework.TestCase;

import org.cipango.client.SipClient;
import org.cipango.client.SipClient.Protocol;
import org.cipango.client.test.TestAgent;
import org.cipango.client.UserAgent;
import org.cipango.kaleo.xcap.XcapUri;

public abstract class UaTestCase extends TestCase
{
	private int _nextPort;
	
	protected List<SipClient> _sipClients = new ArrayList<>();
	private TestAgent _alice;
	private TestAgent _bob;
	protected Properties _properties;
	
	public UaTestCase()
	{
		_properties = new Properties();
		try
		{
			_properties.load(getClass().getClassLoader().getResourceAsStream("integrationTests.properties"));
			_nextPort = getInt("local.sip.port");;
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	public int getTimeout()
	{
		return getInt("local.timeout");
	}
	
	public int getInt(String property)
	{
		return Integer.parseInt(_properties.getProperty(property));
	}

	public String getDomain()
	{
		return _properties.getProperty("local.domain");	
	}
	
	public String getLocalHost()
	{
		return _properties.getProperty("local.host");
	}
	
	public int getRemotePort()
	{
		return getInt("remote.sip.port");
	}

	public String getRemoteHost()
	{
		return _properties.getProperty("remote.host");
	}
	
	public String getHttpXcapUri()
	{
		return _properties.getProperty("http.xcap.uri");
	}
	
	public Protocol getSipDefaultProtocol()
	{
		return Protocol.valueOf(_properties.getProperty("sip.protocol.default", "UDP").toUpperCase());
	}

	public Address getOutboundProxy(UserAgent ua)
	{
		SipURI uri = ua.getFactory().createSipURI(null, getRemoteHost());
		uri.setPort(getRemotePort());
		uri.setLrParam(true);
		return ua.getFactory().createAddress(uri);
	}
	
	public TestAgent newUserAgent(String name) throws Exception
	{
		SipClient sipClient = new SipClient(getLocalHost(), _nextPort++);
		sipClient.start();
		_sipClients.add(sipClient);

		SipURI uri = sipClient.getFactory().createSipURI(name, getDomain());
		TestAgent ua = new TestAgent(sipClient.getFactory().createAddress(uri, name));
		sipClient.addUserAgent(ua);

		ua.setOutboundProxy(getOutboundProxy(ua));
		ua.setTimeout(getTimeout());
	
		return ua;
	}
	
	public String getUri(UserAgent agent)
	{
		return agent.getAor().getURI().toString();
	}
	
	public TestAgent getBob() throws Exception
	{
		if (_bob == null)
		{
			_bob = newUserAgent("bob");
		}
		return _bob;
	}
	
	public TestAgent getAlice()
	{
		return _alice;
	}

	@Override
	protected void setUp() throws Exception
	{
		_alice = newUserAgent("alice");
	}


	@Override
	protected void tearDown() throws Exception
	{
		_alice = null;
		for (SipClient sipClient : _sipClients)
			sipClient.stop();
		
		Thread.sleep(10);
	}
	
	
	
	public void assertValid(HttpURLConnection connection) throws Exception
	{
		connection.connect();
		int code = connection.getResponseCode();
		if (code != 200)
		{
			InputStream is = connection.getErrorStream();
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			int read = 0;
			byte[] b = new byte[1024];
			while ((read = is.read(b)) != -1)
				os.write(b, 0, read);
			fail("Fail on HTTP request: " + connection.getURL() + " with code " + code + " "
					+ connection.getResponseMessage() + "\n" + new String(os.toByteArray()));
		}
	}
	
	public void assertBetween(int min, int max, int actual)
	{
		if (actual > max)
			fail("Got " + actual + " when max is " + max);
		else if (actual < min)
			fail("Got " + actual + " when min is " + min);
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
	
}
