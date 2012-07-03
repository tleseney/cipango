package org.cipango.client;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.sip.Address;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.URI;

public class UserAgent 
{
	private SipProfile _profile;
	private Address _contact;
	private SipFactory _factory;
	private SipServletResponse _lastResponse;
	private long _timeout;

	public UserAgent(SipProfile profile)
	{
		_profile = profile;
	}
	
	public SipProfile getProfile()
	{
		return _profile;
	}
	
	public void setFactory(SipFactory factory)
	{
		_factory = factory;
	}
	
	public SipFactory getFactory()
	{
		return _factory;
	}
	
	public void setContact(Address contact)
	{
		_contact = contact;
	}
	
	public Address getContact()
	{
		return _contact;
	}

	public SipServletResponse getLastResponse()
	{
		return _lastResponse;
	}
	
	public void handleInitialRequest(SipServletRequest request)
	{
		// TODO
	}
	
	
	/**
	 * High level and synchronous registration method.
	 * @throws InterruptedException 
	 */
	public void register(int expires) throws InterruptedException
	{
		final Registration reg = new Registration(_profile.getURI());
		reg.setFactory(_factory);
		reg.setCredentials(_profile.getCredentials());

		reg.setListener(new Registration.Listener() {
			@Override
			public void onRegistered(Address contact, int expires,
					List<Address> contacts)
			{
				reg.notify();
			}
			@Override
			public void onRegistrationFailed(int status)
			{
				reg.notify();
			}
		});

		try
		{
			reg.register(_contact.getURI(), expires);
			reg.wait(_timeout);
		}
		catch (IOException e)
		{
			// TODO
		}
	}
		
//	Call call(URI remote)
//	{
//
//	}
	
	public SipServletRequest createRequest(String method, URI to)
	{
		SipApplicationSession appSession = _factory.createApplicationSession();
		return createRequest(appSession, method, to);
	}

	public SipServletRequest createRequest(SipApplicationSession appSession, String method, URI to)
	{
		return _factory.createRequest(appSession, method, _profile.getURI(), to);
	}

	public SipServletResponse waitForResponse(String method, URI to) throws IOException, InterruptedException
	{
		return waitForResponse(method, to, -1);
	}

	public SipServletResponse waitForResponse(String method, URI to, int code) throws IOException, InterruptedException
	{
		return waitForResponse(createRequest(method, to), code);
	}

	public SipServletResponse waitForResponse(SipServletRequest request, int code) throws IOException, InterruptedException
	{
		SimpleRequestHandler handler = new SimpleRequestHandler(code);

		_lastResponse = null;
		handler.send(request);

		return _lastResponse;
	}

	class SimpleRequestHandler implements MessageHandler
	{
		private int _expected;

		public SimpleRequestHandler(int expected)
		{
			_expected = expected;
		}

		public void send(SipServletRequest request) throws IOException, InterruptedException
		{
			request.getSession().setAttribute(MessageHandler.class.getName(), this);
			request.send();

			wait(_timeout);
		}

		public void handleRequest(SipServletRequest request) throws IOException, ServletException 
		{
			request.createResponse(SipServletResponse.SC_NOT_ACCEPTABLE_HERE).send();
		}
	
		public void handleResponse(SipServletResponse response) throws IOException, ServletException 
		{
			_lastResponse = response;
			if (_expected == -1 || _expected == response.getStatus())
				notify();
		}
	}
}
