package org.cipango.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.sip.Address;
import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipURI;

public class UserAgent 
{
	private Address _aor;
	private Address _contact;
	private AuthHelper _authHelper;
	private List<Credentials> _credentials = new ArrayList<Credentials>();
	private SipFactory _factory;
	private Registration _registration;
	private Registration.Listener _registrationListener;
	private long _timeout;

	public UserAgent(Address aor)
	{
		_aor = aor;
	}
	
	public SipFactory getFactory()
	{
		return _factory;
	}
	
	public void setFactory(SipFactory factory)
	{
		_factory = factory;
	}
	
	/**
	 * @return the registrationListener
	 */
	public Registration.Listener getRegistrationListener()
	{
		return _registrationListener;
	}

	/**
	 * @param registrationListener the registrationListener to set
	 */
	public void setRegistrationListener(Registration.Listener listener)
	{
		_registrationListener = listener;
		if (_registration != null)
			_registration.setListener(listener);
	}

	public void setContact(Address contact)
	{
		_contact = contact;
	}
	
	public Address getContact()
	{
		return _contact;
	}
	
	public void handleInitialRequest(SipServletRequest request)
	{
		// TODO
	}
		
	/**
	 * Synchronous registers this <code>UserAgent</code>.
	 * 
	 * @param expires
	 * @throws IOException 
	 * @throws ServletException 
	 * @see Registration#register(javax.servlet.sip.URI, int)
	 */
	public void register(int expires) throws IOException, ServletException
	{
		if (!_aor.getURI().isSipURI())
		{
			// TODO
			return;
		}

		if (_registration == null)
		{
			_registration = new Registration((SipURI) _aor.getURI());
			_registration.setListener(_registrationListener);
		}

		_registration.register(_contact.getURI(), expires);
	}
	
	public void unregister() throws IOException, ServletException
	{
		if (_registration != null)
			_registration.unregister();
	}
		
//	Call call(URI remote)
//	{
//
//	}
	
	public SipServletRequest createRequest(String method, String to) throws ServletParseException
	{
		SipApplicationSession appSession = _factory.createApplicationSession();
		return _factory.createRequest(appSession, method, getAor(), _factory.createAddress(to));
	}
	
	public SipServletRequest createRequest(String method, Address to)
	{
		SipApplicationSession appSession = _factory.createApplicationSession();
		return _factory.createRequest(appSession, method, getAor(), to);
	}
	
	public SipServletRequest createRequest(String method, UserAgent agent)
	{
		SipApplicationSession appSession = _factory.createApplicationSession();
		return _factory.createRequest(appSession, method, getAor(), agent.getAor());
	}


	/**
	 * Creates a request with the given method and destination URI and waits for the final response.
	 * @param request
	 * @return the final response or <code>null</code> if no response has been received before timeout.
	 * @throws IOException
	 * @see {@link #setTimeout()}
	 */
	public SipServletResponse sendSynchronous(String method, Address to) throws IOException
	{
		return sendSynchronous(createRequest(method, to));
	}

	/**
	 * Sends <code>request</code> and waits for the final response.
	 * @param request
	 * @return the final response or <code>null</code> if no response has been received before timeout.
	 * @throws IOException
	 * @see {@link #setTimeout()}
	 */
	public SipServletResponse sendSynchronous(SipServletRequest request) throws IOException
	{
		RequestHandler handler = new RequestHandler(request, this);
		handler.send();
		return handler.waitFinalResponse();
	}
	
	/**
	 * Sends <code>request</code>.
	 * @param request
	 * @return the request handler.
	 * @throws IOException
	 */
	public RequestHandler send(SipServletRequest request) throws IOException
	{
		RequestHandler handler = new RequestHandler(request, this);
		handler.send();
		return handler;
	}
		
	@SuppressWarnings("unchecked")
	public List<SipServletResponse> getResponses(SipServletRequest request)
	{
		return (List<SipServletResponse>) request.getAttribute(SipServletResponse.class.getName());
	}
	
	public long getTimeout()
	{
		return _timeout;
	}

	public void setTimeout(long timeout)
	{
		_timeout = timeout;
	}

	public List<Credentials> getCredentials()
	{
		return _credentials;
	}

	public void addCredentials(Credentials credentials)
	{
		_credentials.add(credentials);
	}

	public Address getAor()
	{
		return _aor;
	}
	
	public String getDomain()
	{
		if (_aor.getURI().isSipURI())
			return ((SipURI) _aor.getURI()).getHost();
		return null;
	}

	public AuthHelper getAuthHelper()
	{
		return _authHelper;
	}

	public void setAuthHelper(AuthHelper authHelper)
	{
		_authHelper = authHelper;
	}
}
