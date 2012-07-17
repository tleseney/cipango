// ========================================================================
// Copyright 2007-2008 NEXCOM Systems
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
package org.cipango.sipunit;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.sip.Address;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipURI;

import junit.framework.TestCase;

import org.cipango.client.SipClient;
import org.cipango.client.SipMethods;

public abstract class UaTestCase extends TestCase
{
	public static final String APP_NAME = "cipango-servlet-test";
	
	protected SipClient _sipClient;
	protected TestAgent _ua;
	private TestAgent _ub;
	private TestAgent _uc;

	protected Properties _properties;

	public UaTestCase()
	{
		_properties = new Properties();
		try
		{
			_properties.load(getClass().getClassLoader()
					.getResourceAsStream("commonTest.properties"));
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	public String getProtocol()
	{
		// TODO: now useless?
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

	public String getHttpBaseUrl()
	{
		return "http://" + getRemoteHost() + ":" + _properties.getProperty("sipunit.http.port")
				+ "/cipango-servlet-test";
	}

	public String getFrom()
	{
		return "sip:sipUnit@" + _properties.getProperty("sipunit.test.domain");
	}
	
	public String getBobUri()
	{
		return "sip:bob@" + _properties.getProperty("sipunit.test.domain");
	}
	
	public Address getBobContact()
	{
		return getBobUserAgent().getAor();
	}
	
	public String getCarolUri()
	{
		return "sip:carol@" + _properties.getProperty("sipunit.test.domain");
	}
	
	public Address getCarolContact()
	{
		return getCarolUserAgent().getAor();
	}

	public String getTo()
	{
		return "sip:sipServlet@" + _properties.getProperty("sipunit.test.domain");
	}

	public void setUp() throws Exception
	{
		Properties properties = new Properties();
		properties.putAll(_properties);
		_sipClient = new SipClient(getLocalPort());
		_sipClient.start();

		_ua = new TestAgent(_sipClient.getFactory().createAddress(getFrom()));
		_sipClient.addUserAgent(_ua);

		SipURI uri = _ua.getFactory().createSipURI(null, _properties.getProperty("sipunit.proxy.host"));
		uri.setPort(Integer.parseInt(_properties.getProperty("sipunit.proxy.port")));
		uri.setLrParam(true);
		_ua.setOutboundProxy(_ua.getFactory().createAddress(uri));
	}

	public void tearDown() throws Exception
	{
		_ua = _ub = _uc = null;
		_sipClient.stop();
	}

	public void assertValid(SipServletResponse response)
	{
		assertValid(response, SipServletResponse.SC_OK);
	}
	
	public void assertValid(SipServletResponse response, int statusCode)
	{
		if (response == null)
			fail("Does not received SIP response");
		if (response.getStatus() != statusCode)
		{
			String error = "Test case fail on " + response.getStatus() + " "
					+ response.getReasonPhrase();
			try
			{
				if (response.getContentLength() > 0)
					fail(error + "\n" + new String(response.getRawContent()));
				else
					fail(error);
			}
			catch (IOException e)
			{
				fail(error);
			}
		}
	}
	
	public TestAgent getBobUserAgent()
	{
		try {
			if (_ub == null)
			{
				Properties properties = new Properties();
				properties.putAll(_properties);
				_ub = new TestAgent(_sipClient.getFactory().createAddress(getBobUri()));
				_sipClient.addUserAgent(_ub);
				// TODO: Useless? _ub.getProxy().setUser(APP_NAME);
			}
			return _ub;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public TestAgent getCarolUserAgent()
	{
		try {
			if (_uc == null)
			{
				Properties properties = new Properties();
				properties.putAll(_properties);
				_uc = new TestAgent(_sipClient.getFactory().createAddress(getCarolUri()));
				_sipClient.addUserAgent(_uc);
				// TODO: Useless? _uc.getProxy().setUser(APP_NAME);
			}
			return _uc;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
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
	
	
	public void sendAndAssertMessage() throws IOException, ServletException
	{
		SipServletRequest request = _ua.createRequest(SipMethods.MESSAGE, getTo());
		SipServletResponse response = _ua.sendSynchronous(request);
        assertValid(response);
	}
	
	
	/**
	 * Call the method checkForFailure on the AS.
	 * This method is useful when exception are thrown on the AS when no response 
	 * can be sent by the server (in doResponse(), on a committed request or on a
	 * ACK).
	 */
	public void checkForFailure()
	{
		try
		{
			// Ensure servlet has ended the treatment.
			Thread.sleep(50);

			SipServletRequest request = _ua.createRequest(SipMethods.MESSAGE, getTo());
	        assertValid(_ua.sendSynchronous(request));
		}
		catch (Exception e)
		{
			throw new IllegalStateException(e);
		}
	}
}
