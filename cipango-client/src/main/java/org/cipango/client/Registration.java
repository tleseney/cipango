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
 * Manages registration of a user agent.
 * Refreshes regularly the registration to remain registered.
 *
 */
public class Registration 
{
	private UserAgent _userAgent;
	private Listener _listener;
	private Authentication _authentication;
	private SipSession _session;


	public Registration(UserAgent userAgent)
	{
		_userAgent = userAgent;
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
			SipApplicationSession appSession = _userAgent.getFactory().createApplicationSession();
			register = _userAgent.getFactory().createRequest(appSession, 
					SipMethods.REGISTER,
					_userAgent.getAor(), 
					_userAgent.getAor());
			
			_session = register.getSession();
			_session.setAttribute(MessageHandler.class.getName(), new Handler());
		}
		else
		{
			register = _session.createRequest(SipMethods.REGISTER);
		}
		
		SipURI registrar = _userAgent.getFactory().createSipURI(null, _userAgent.getDomain());
		register.setRequestURI(registrar);
		register.setAddressHeader(SipHeaders.CONTACT, _userAgent.getFactory().createAddress(contact));
		register.setExpires(expires);
		
//		if (_authentication != null)
//		{
//			try
//			{
//				String authorization = _authentication.authorize(
//						register.getMethod(),
//						register.getRequestURI().toString(), 
//						_credentials);
//				register.addHeader(SipHeader.AUTHORIZATION.asString(), authorization);
//			}
//			catch (ServletException e)
//			{
//				e.printStackTrace();
//			}
//		}
		
		return register;
	}

	public void register(URI contact, int expires) throws IOException
	{
		try
		{
			createRegister(contact, expires).send();
		}
		catch (IOException e)
		{
			if (_listener != null)
				_listener.onRegistrationFailed(-1);
			throw(e);
		}
	}
	
	protected void registrationDone(Address contact, int expires, List<Address> contacts)
	{
		if (_listener != null)
			_listener.onRegistered(contact, expires, contacts);
	}
	
	protected void registrationFailed(int status)
	{
		if (_listener != null)
			_listener.onRegistrationFailed(status);
	}
	
	public interface Listener
	{
		void onRegistered(Address contact, int expires, List<Address> contacts);
		void onUnregistered(Address contact);
		void onRegistrationFailed(int status);
	}
	
	class Handler implements MessageHandler
	{
		@Override
		public void handleRequest(SipServletRequest request) throws IOException, ServletException 
		{
			request.createResponse(SipServletResponse.SC_NOT_ACCEPTABLE_HERE).send();
		}
	
		@Override
		public void handleResponse(SipServletResponse response) throws IOException, ServletException 
		{
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
//				if (_credentials == null)
//				{
//					registrationFailed(status);
//				}
//				else
//				{
//					String authorization = response.getRequest().getHeader(SipHeader.AUTHORIZATION.asString());
//					
//					String authenticate = response.getHeader(SipHeader.WWW_AUTHENTICATE.asString());
//					Authentication.Digest digest = Authentication.getDigest(authenticate);
//					
//					if (authorization != null && !digest.isStale())
//					{
//						registrationFailed(status);
//					}
//					else
//					{
//						_authentication = new Authentication(digest);
//						
//						URI contact = response.getRequest().getAddressHeader(SipHeader.CONTACT.asString()).getURI();
//						register(contact, response.getRequest().getExpires());
//					}
//				}
			}
			else 
			{
				registrationFailed(status);
			}
		}
	}	
}
