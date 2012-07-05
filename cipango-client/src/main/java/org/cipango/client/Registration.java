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

/**
 * Manages the registration of a user agent.
 */
public class Registration 
{
	private Listener _listener;
	private List<Credentials> _credentials;
	private SipFactory _factory;
	private SipSession _session;
	private SipURI _uri;
	private URI _contact;
	private long _timeout;
	private boolean _registered;

	public interface Listener
	{
		void onRegistered(Address contact, int expires, List<Address> contacts);
		void onUnregistered(Address contact);
		void onRegistrationFailed(int status);
	}
	
	public Registration(SipURI uri)
	{
		_uri = uri;
	}

	public SipFactory getFactory()
	{
		return _factory;
	}
	
	public void setFactory(SipFactory factory)
	{
		_factory = factory;
	}

	public List<Credentials> getCredentials()
	{
		return _credentials;
	}

	public void setCredentials(List<Credentials> credentials)
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
			register = _factory.createRequest(appSession, SipMethods.REGISTER, _uri, _uri);
			_session = register.getSession();
		}
		else
		{
			register = _session.createRequest(SipMethods.REGISTER);
		}
		
		SipURI registrar = _factory.createSipURI(null, _uri.getHost());
		register.setRequestURI(registrar);
		if (contact != null)
			register.setAddressHeader(SipHeaders.CONTACT, _factory.createAddress(contact));
		else
			register.setHeader(SipHeaders.CONTACT, "*");
		register.setExpires(expires);
		
		return register;
	}

	/**
	 * Synchronously registers <code>contact</code> with the given expiration
	 * value.
	 * 
	 * This method returns either when success, failure or request timeout is
	 * encountered, and the listener, if any, is notified as well.
	 * Authentication is handled automatically if credentials were provided to
	 * this <code>UserAgent</code>.
	 * 
	 * @param contact
	 * @param expires
	 * @return <code>true</code> if registration was successful,
	 *         <code>false</code> otherwise.
	 * @throws IOException
	 * @throws ServletException
	 */
	public boolean register(URI contact, int expires) throws IOException, ServletException
	{
		RequestHandler handler = new RequestHandler(createRegister(contact, expires), _timeout);

		_contact = contact;

		handler.setCredentials(_credentials);
		handler.send();
		processResponse(handler.waitFinalResponse());

		return _registered;
	}

	/**
	 * Synchronously unregisters <code>contact</code>.
	 * 
	 * @param contact
	 *            the contact to unregister. If null, then <code>*</code> is set
	 *            in the REGISTER request's Contact header, so that all contacts
	 *            are unregistered.
	 * @return <code>true</code> if unregistration was successful,
	 *         <code>false</code> otherwise.
	 * @throws IOException
	 * @throws ServletException
	 */
	public boolean unregister(URI contact) throws IOException, ServletException
	{
		RequestHandler handler = new RequestHandler(createRegister(_contact, 0), _timeout);

		handler.send();
		processResponse(handler.waitFinalResponse());
		
		return !_registered;
	}
	
	protected void registrationDone(Address contact, int expires, List<Address> contacts)
	{
		if (_listener != null)
		{
			if (expires == 0)
			{
				_registered = false;
				_listener.onUnregistered(contact);
			}
			else
			{
				_registered = true;
				_listener.onRegistered(contact, expires, contacts);
			}
		}
	}
	
	protected void registrationFailed(int status)
	{
		if (_listener != null)
			_listener.onRegistrationFailed(status);
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
			
			Address requestContact = response.getRequest().getAddressHeader(SipHeaders.CONTACT);
			
			List<Address> contacts = new ArrayList<Address>();
			
			ListIterator<Address> it = response.getAddressHeaders(SipHeaders.CONTACT);
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
			if (response.getAttribute(RequestHandler.HANDLED_ATTRIBUTE) == null)
				registrationFailed(status);
		}
		else 
		{
			registrationFailed(status);
		}
	}
}
