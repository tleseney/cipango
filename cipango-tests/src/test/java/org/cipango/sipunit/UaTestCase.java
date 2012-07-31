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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.cipango.sipunit.test.matcher.SipMatchers.*;

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
import org.cipango.client.SipHeaders;
import org.cipango.client.SipMethods;

public abstract class UaTestCase extends TestCase
{
	protected SipClient _sipClient;
	protected SipClient _sipBobClient;
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

	public int getTimeout()
	{
		return getInt("sipunit.test.timeout");
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
		return "sip:alice@" + _properties.getProperty("sipunit.test.domain");
	}
	
	public String getBobUri()
	{
		return "sip:bob@" + _properties.getProperty("sipunit.test.domain");
	}
	
	public Address getBobContact()
	{
		SipURI contact = (SipURI) _sipBobClient.getContact().clone();
		contact.setUser("bob");
		System.out.println("** Bob Contact: " + contact);
		return _ua.getFactory().createAddress(contact);
	}
	
	public String getCarolUri()
	{
		return "sip:carol@" + _properties.getProperty("sipunit.test.domain");
	}
	
	public Address getCarolContact()
	{
		SipURI contact = (SipURI) _sipClient.getContact().clone();
		contact.setUser("carol");
		return _ua.getFactory().createAddress(contact);
	}

	public String getTo()
	{
		return "sip:sipServlet@" + _properties.getProperty("sipunit.test.domain");
	}

	@Override
	protected void runTest() throws Throwable {
		decorate(_ua);
		
		super.runTest();
	}
	
	@Override
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
		_ua.setTimeout(getTimeout());
	}

	@Override
	public void tearDown() throws Exception
	{
		_ua = _ub = _uc = null;
		_sipClient.stop();
		if (_sipBobClient != null)
			_sipBobClient.stop();
	}
	
	public TestAgent getBobUserAgent()
	{
		return getBobUserAgent(SipClient.Protocol.UDP);
	}
	
	public TestAgent getBobUserAgent(SipClient.Protocol protocol)
	{
		try {
			if (_sipBobClient == null)
			{
				_sipBobClient = new SipClient();
				_sipBobClient.addConnector(protocol, null, getLocalPort() + 1);
				_sipBobClient.start();

				_ub = decorate(new TestAgent(_sipBobClient.getFactory().createAddress(getBobUri())));
				_ub.setTimeout(getTimeout());
				_sipBobClient.addUserAgent(_ub);
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
				_uc = decorate(new TestAgent(_sipClient.getFactory().createAddress(getCarolUri())));
				_uc.setTimeout(getTimeout());
				_sipClient.addUserAgent(_uc);
			}
			return _uc;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public TestAgent decorate(TestAgent agent)
	{
		agent.setTestServlet(getClass().getName());
		agent.setTestMethod(getName());
		
		return agent;
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
        assertThat(response, isSuccess());
	}
	
	public void startScenario() throws IOException, ServletException
	{
		SipServletRequest request = _ua.createRequest(SipMethods.REGISTER, getTo());
		request.addHeader(SipHeaders.CONTACT, _sipClient.getContact().toString());
		SipServletResponse response = _ua.sendSynchronous(request);
        assertThat(response, isSuccess());
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
			request.removeHeader(TestAgent.METHOD_HEADER);
			request.addHeader(TestAgent.METHOD_HEADER, "checkForFailure");
			SipServletResponse response = _ua.sendSynchronous(request);
			assertThat(response, isSuccess());
		}
		catch (Exception e)
		{
			throw new IllegalStateException(e);
		}
	}
}
