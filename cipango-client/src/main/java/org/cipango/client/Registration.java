// ========================================================================
// Copyright 2011 NEXCOM Systems
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

package org.cipango.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import javax.servlet.ServletException;
import javax.servlet.sip.Address;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.URI;

import org.cipango.sip.AddressImpl;
import org.cipango.sip.SipHeader;
import org.cipango.sip.SipMethod;

/**
 * Manages the registration of a user agent.
 * 
 * Refreshes regularly the registration to remain registered.
 *
 */
public class Registration 
{
	private Listener _listener;
	private Authentication _authentication;
	private Credentials _credentials;
	private SipFactory _factory;
	private SipSession _session;
	private SipURI _uri;
	private URI _contact;
	private int _expires;
	private long _timeout;
	private Status _status;
	
	public enum Status
	{
		UNREGISTERED,
		REGISTERED,
		PENDING,
		FAILED
	}

	public interface Listener
	{
		void onRegistered(Address contact, int expires, List<Address> contacts);
		void onUnregistered(Address contact);
		void onRegistrationFailed(int status);
	}
	
	public Registration(SipURI uri)
	{
		_uri = uri;
		_status = Status.UNREGISTERED; 
	}

	public SipFactory getFactory()
	{
		return _factory;
	}
	
	public void setFactory(SipFactory factory)
	{
		_factory = factory;
	}

	public Credentials getCredentials()
	{
		return _credentials;
	}

	public void setCredentials(Credentials credentials)
	{
		_credentials = credentials;
	}

	public long getTimeout()
	{
		return _timeout;
	}

	public void setTimeout(long timeout)
	{
		_timeout = timeout;
	}
	
	public SipURI getURI()
	{
		return _uri;
	}
	
	public void setListener(Listener listener)
	{
		_listener = listener;
	}

	public SipServletRequest createRegister(URI contact, int expires)
	{
		SipServletRequest register;
		
		if (_session == null)
		{
			SipApplicationSession appSession = _factory.createApplicationSession();
			register = _factory.createRequest(appSession, SipMethod.REGISTER.asString(), _uri, _uri);
			_session = register.getSession();
		}
		else
		{
			register = _session.createRequest(SipMethod.REGISTER.asString());
		}
		
		SipURI registrar = _factory.createSipURI(null, _uri.getHost());
		register.setRequestURI(registrar);
		register.setAddressHeader(SipHeader.CONTACT.asString(), new AddressImpl(contact));
		register.setExpires(expires);
		
		if (_authentication != null)
		{
			try
			{
				String authorization = _authentication.authorize(
						register.getMethod(),
						register.getRequestURI().toString(), 
						_credentials);
				register.addHeader(SipHeader.AUTHORIZATION.asString(), authorization);
			}
			catch (ServletException e)
			{
				e.printStackTrace();
			}
		}
		
		return register;
	}

	/**
	 * Synchronously registers <code>contact</code> with the given expiration value.
	 * 
	 * This method returns either when success, failure or request timeout is
	 * encountered. Authentication is handled automatically if credentials were
	 * provided to this Registration.
	 * 
	 * On success, the registration is maintained in the background by sending
	 * regularly re-REGISTER requests. Subsequent events (end of registration,
	 * failure to register) will be reported through the listener.
	 * 
	 * @param contact
	 * @param expires
	 * @throws IOException
	 * @throws ServletException
	 */
	public void register(URI contact, int expires) throws IOException, ServletException
	{
		long end = System.currentTimeMillis() + _timeout;
		RequestHandler handler;

		_contact = contact;
		_expires = expires;
		_status = Status.PENDING;
		
		while (_status == Status.PENDING)
		{
			handler = new RequestHandler(createRegister(_contact, _expires),
					end - System.currentTimeMillis());
			handler.send();
			processResponse(handler.waitFinalResponse());
		}
		
		// TODO: start registration monitoring.
	}

	/**
	 * Unregisters any registration previously successfully achieved.
	 * 
	 * After this method returns, the contact that was previously registered is
	 * unregistered, and no background activity is sustained to register it
	 * again.
	 * 
	 * @throws IOException 
	 * @throws ServletException 
	 */
	public void unregister() throws IOException, ServletException
	{
		long end = System.currentTimeMillis() + _timeout;
		RequestHandler handler;

		_status = Status.PENDING;
		
		while (_status == Status.PENDING)
		{
			handler = new RequestHandler(createRegister(_contact, 0),
					end - System.currentTimeMillis());
			handler.send();
			processResponse(handler.waitFinalResponse());
		}

		// TODO: start registration monitoring.
	}
	
	protected void registrationDone(Address contact, int expires, List<Address> contacts)
	{
		if (_listener != null)
		{
			if (expires == 0)
				_listener.onUnregistered(contact);
			else
				_listener.onRegistered(contact, expires, contacts);
		}
		_status = Status.REGISTERED;
	}
	
	protected void registrationFailed(int status)
	{
		if (_listener != null)
			_listener.onRegistrationFailed(status);
		_status = Status.FAILED;
	}
	
	protected void processResponse(SipServletResponse response) throws ServletException, IOException 
	{
		if (response == null)
		{
			registrationFailed(-1);
			return;
		}

		int status = response.getStatus();
		
		if (status == SipServletResponse.SC_OK)
		{
			int expires = -1;
			
			Address requestContact = response.getRequest().getAddressHeader(SipHeader.CONTACT.asString());
			
			List<Address> contacts = new ArrayList<Address>();
			
			ListIterator<Address> it = response.getAddressHeaders(SipHeader.CONTACT.asString());
			while (it.hasNext())
			{
				Address contact = it.next();
				if (contact.equals(requestContact))
					expires = contact.getExpires();
				contacts.add(contact);
			}
			
			if (expires != -1)
				registrationDone(requestContact, expires, contacts);
			else 
				registrationFailed(0);
		}
		else if (status == SipServletResponse.SC_UNAUTHORIZED)
		{
			if (_credentials == null)
			{
				registrationFailed(status);
			}
			else
			{
				String authorization = response.getRequest().getHeader(SipHeader.AUTHORIZATION.asString());
				
				String authenticate = response.getHeader(SipHeader.WWW_AUTHENTICATE.asString());
				Authentication.Digest digest = Authentication.getDigest(authenticate);
				
				if (authorization != null && !digest.isStale())
				{
					registrationFailed(status);
				}
				else
				{
					_authentication = new Authentication(digest);
					_contact = response.getRequest().getAddressHeader(SipHeader.CONTACT.asString()).getURI();
					_expires = response.getRequest().getExpires();
				}
			}
		}
		else 
		{
			registrationFailed(status);
		}
	}
}
