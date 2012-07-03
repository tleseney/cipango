package org.cipango.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
	private List<Credentials> _credentials = new ArrayList<Credentials>();
	private SipFactory _factory;
	private long _timeout;

	public UserAgent(Address aor)
	{
		_aor = aor;
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
//		final Registration reg = new Registration(this);
//		
//		reg.setListener(new Registration.Listener() {
//			@Override
//			public void onRegistered(Address contact, int expires,
//					List<Address> contacts)
//			{
//				reg.notify();
//			}
//			@Override
//			public void onRegistrationFailed(int status)
//			{
//				reg.notify();
//			}
//		});
//
//		try
//		{
//			reg.register(_contact.getURI(), expires);
//			reg.wait(_timeout);
//		}
//		catch (IOException e)
//		{
//			// TODO
//		}
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
	public SipServletResponse sendSynchronous(String method, Address to) throws IOException, InterruptedException
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
	
	/**
	 * Returns <code>true</code> if a challenge has been proceeded and a request has been sent else
	 * returns <code>false</code>.
	 * @param response
	 * @return
	 */
	public boolean handleChallenge(SipServletResponse response)
	{
		if (_credentials.isEmpty())
			return false;
		
		return true;
		
// TODO		String authorization = response.getRequest().getHeader(SipHeader.AUTHORIZATION.asString());
//		
//		String authenticate = response.getHeader(SipHeader.WWW_AUTHENTICATE.asString());
//		Authentication.Digest digest = Authentication.getDigest(authenticate);
//		
//		if (authorization != null && !digest.isStale())
//		{
//			registrationFailed(status);
//		}
//		else
//		{
//			_authentication = new Authentication(digest);
//			
//			URI contact = response.getRequest().getAddressHeader(SipHeader.CONTACT.asString()).getURI();
//			register(contact, response.getRequest().getExpires());
//		}
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
}
