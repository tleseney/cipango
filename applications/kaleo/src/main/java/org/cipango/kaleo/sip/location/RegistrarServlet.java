// ========================================================================
// Copyright 2008-2009 NEXCOM Systems
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

package org.cipango.kaleo.sip.location;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.TimeZone;

import javax.servlet.ServletException;
import javax.servlet.sip.Address;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;

import org.cipango.kaleo.Constants;
import org.cipango.kaleo.URIUtil;
import org.cipango.kaleo.event.Subscription;
import org.cipango.kaleo.event.Subscription.Reason;
import org.cipango.kaleo.event.Subscription.State;
import org.cipango.kaleo.location.Binding;
import org.cipango.kaleo.location.LocationService;
import org.cipango.kaleo.location.Registration;
import org.cipango.kaleo.location.event.RegEventPackage;
import org.cipango.kaleo.location.event.RegResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegistrarServlet extends SipServlet
{
	private static final long serialVersionUID = 1L;

	private LocationService _locationService;
	private RegEventPackage _regEventPackage;
	
	private int _minExpires = 5; // 60;
	private int _maxExpires = 3600;
	private int _defaultExpires = 3600;
	
	private DateFormat _dateFormat;
	private SipFactory _sipFactory;
	
	private Logger _log = LoggerFactory.getLogger(RegistrarServlet.class);
	
	public void init()
	{
		_dateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z", Locale.US);
		_dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		
		_sipFactory = (SipFactory) getServletContext().getAttribute(SIP_FACTORY);
		
		_locationService = (LocationService) getServletContext().getAttribute(LocationService.class.getName());
		_regEventPackage = (RegEventPackage) getServletContext().getAttribute(RegEventPackage.class.getName());
	}
	
	protected void doRegister(SipServletRequest register) throws IOException, ServletException
	{
		String aor = URIUtil.toCanonical(register.getTo().getURI());
		
		ListIterator<String> it2 = register.getHeaders(Constants.REQUIRE);
		if (it2.hasNext())
		{
			SipServletResponse response = register.createResponse(SipServletResponse.SC_BAD_EXTENSION);
			while (it2.hasNext())
				response.addHeader(Constants.UNSUPPORTED, it2.next());
			response.send();
			return;
		}
		
		long now = System.currentTimeMillis();
		List<Binding> bindings;

		Registration record = _locationService.get(aor);
		
		record.addListener(_regEventPackage.getRegistrationListener());
		
		try
		{
			bindings = record.getBindings();
			if (bindings == null)
				bindings = Collections.emptyList();
				
			Iterator<Address> it = register.getAddressHeaders(Constants.CONTACT);
			if (it.hasNext())
			{
				List<Address> contacts = new ArrayList<Address>();
				boolean wildcard = false;
				
				while (it.hasNext())
				{
					Address contact = it.next();
					if (contact.isWildcard())
					{
						wildcard = true;
						if (it.hasNext() || contacts.size() > 0 || register.getExpires() > 0)
						{
							register.createResponse(SipServletResponse.SC_BAD_REQUEST, "Invalid wildcard").send();
							return;
						}
					}
					contacts.add(contact);
				}
				
				String callId = register.getCallId();
				int cseq;
				try
				{
					String s = register.getHeader(Constants.CSEQ);
					cseq = Integer.parseInt(s.substring(0, s.indexOf(' ')));
				}
				catch (Exception e)
				{
					register.createResponse(SipServletResponse.SC_BAD_REQUEST).send();
					return;
				}
				
				if (wildcard)
				{
					for (Binding binding : bindings)
					{
						if (callId.equals(binding.getCallId()) && cseq < binding.getCSeq())
						{
							_log.debug("Got lower CSeq for aor {} and call-ID {}", aor, binding.getCallId());
							register.createResponse(SipServletResponse.SC_SERVER_INTERNAL_ERROR, "Lower CSeq").send();
							return;
						}
					}
					if (_log.isDebugEnabled())
						_log.debug("removing all bindings for aor " + aor);
					record.removeAllBindings();
				}
				else
				{				
					for (Address contact : contacts)
					{
						int expires = -1;
						expires = contact.getExpires();
						if (expires < 0)
							expires = register.getExpires();
						
						if (expires != 0)
						{
							if (expires < 0)
								expires = _defaultExpires;
							if (expires > _maxExpires)
								expires = _maxExpires;
							if (expires < _minExpires)
							{
								SipServletResponse response = register.createResponse(SipServletResponse.SC_INTERVAL_TOO_BRIEF);
								response.addHeader(Constants.MIN_EXPIRES, Integer.toString(_minExpires));
								response.send();
								return;
							}
						}
						Binding binding = null;
						
						for (int i = 0; i < bindings.size() && binding == null; i++)
						{
							binding = bindings.get(i);
							if (!contact.getURI().equals(binding.getContact()))
								binding = null;
						}
						if (binding != null)
						{
							if (callId.equals(binding.getCallId()) && cseq < binding.getCSeq())
							{
								_log.debug("Got lower CSeq for aor {} and call-ID {}", aor, binding.getCallId());
								register.createResponse(SipServletResponse.SC_SERVER_INTERNAL_ERROR, "Lower CSeq").send();
								return;
							}
							if (expires == 0)
							{
								if (_log.isDebugEnabled())
									_log.debug("removing binding {} for aor {}", binding, aor);
								record.removeBinding(binding);
							}
							else
							{
								if (_log.isDebugEnabled())
									_log.debug("updating binding {} for aor {}", binding, aor);
								record.updateBinding(binding, contact.getURI(), callId, cseq, now + expires*1000);
							}
						}
						
						if (binding == null && expires != 0)
						{
							binding = new Binding(contact.getURI(), callId, cseq, now + expires*1000);
							
							if (_log.isDebugEnabled())
								_log.debug("adding binding {} to aor {}", binding, aor);
							record.addBinding(binding);
						}
					}
				}
				bindings = record.getBindings();
			}
		}
		finally 
		{
			_locationService.put(record);
		}
		
		SipServletResponse ok = register.createResponse(SipServletResponse.SC_OK);
		ok.addHeader(Constants.DATE, _dateFormat.format(new Date(now)));
		if (bindings != null)
		{
			for (Binding binding : bindings)
			{
				Address address = _sipFactory.createAddress(binding.getContact());
				address.setExpires((int) ((binding.getExpirationTime() - now) / 1000));
				ok.addAddressHeader(Constants.CONTACT, address,false);
			}
		}
		ok.send();
	}

	@Override
	protected void doSubscribe(SipServletRequest subscribe) throws ServletException,
			IOException
	{
		String event = subscribe.getHeader(Constants.EVENT);
		
		if (event == null || !(event.equals(_regEventPackage.getName())))
		{
			SipServletResponse response = subscribe.createResponse(SipServletResponse.SC_BAD_EVENT);
			response.addHeader(Constants.ALLOW_EVENTS, _regEventPackage.getName());
			response.send();
			response.getApplicationSession().invalidate();
			return;
		}
		
		if(!checkAcceptHeader(subscribe))
			return;
				
		int expires = subscribe.getExpires();
		if (expires != -1)
		{
			if (expires != 0)
			{
				if (expires < _regEventPackage.getMinExpires())
				{
					SipServletResponse response = subscribe.createResponse(SipServletResponse.SC_INTERVAL_TOO_BRIEF);
					response.addHeader(Constants.MIN_EXPIRES, Integer.toString(_regEventPackage.getMinExpires()));
					response.send();
					response.getApplicationSession().invalidate();
					return;
				}
				else if (expires > _regEventPackage.getMaxExpires())
				{
					expires = _regEventPackage.getMaxExpires();
				}
			}
		}
		else 
		{
			expires = _regEventPackage.getDefaultExpires();
		}
		
		SipSession session = subscribe.getSession();
		String uri = null;
		
		if (subscribe.isInitial())
		{
			uri = URIUtil.toCanonical(subscribe.getRequestURI());
		}
		else
		{
			uri = (String) session.getAttribute(Constants.SUBSCRIPTION_ATTRIBUTE);
			if (uri == null)
			{
				subscribe.createResponse(SipServletResponse.SC_CALL_LEG_DONE).send();
				subscribe.getApplicationSession().invalidate();
				return;
			}
		}
		
		RegResource regResource = _regEventPackage.get(uri);
		
		try
		{
			Subscription subscription = null;
			
			if (expires == 0)
			{
				subscription = regResource.removeSubscription(session.getId());
				
				if (subscription == null)
				{
					subscription = new Subscription(regResource, session, -1);
				}
				else
				{
					subscription.setExpirationTime(System.currentTimeMillis());
					if (_log.isDebugEnabled())
						_log.debug("removed reg subscription {} to registration resource {}", 
							subscription.getSession().getId(), regResource.getUri());
				}	
				subscription.setState(Subscription.State.TERMINATED, Reason.TIMEOUT);
			}
			else
			{
				long now = System.currentTimeMillis();
				
				subscription = regResource.getSubscription(session.getId());
		
				if (subscription == null)
				{
					subscription = new Subscription(regResource, session, now + expires*1000);
					subscription.setState(State.ACTIVE, Reason.SUBSCRIBE);
					regResource.addSubscription(subscription);
					
					session.setAttribute(Constants.SUBSCRIPTION_ATTRIBUTE, uri);
					
					if (_log.isDebugEnabled())
						_log.debug("added reg subscription {} to registration resource {}", 
								subscription.getSession().getId(), regResource.getUri());
				}
				else 
				{
					subscription.setExpirationTime(now + expires * 1000);
					
					if (_log.isDebugEnabled())
						_log.debug("refreshed reg subscription {} to registration resource {}",
								subscription.getSession().getId(), regResource.getUri());
				}
			}
			
			int code = (subscription.getState() != Subscription.State.PENDING) ? 
					SipServletResponse.SC_OK : SipServletResponse.SC_ACCEPTED;
			
			SipServletResponse response = subscribe.createResponse(code);
			response.setExpires(expires);
			response.send();
				
			_regEventPackage.notify(subscription);
		}
		finally
		{
			_regEventPackage.put(regResource);
		}
	}
	
	private boolean checkAcceptHeader(SipServletRequest subscribe) throws IOException
	{
		List<String> supported = _regEventPackage.getSupportedContentTypes();
		Iterator<String> it = subscribe.getHeaders(Constants.ACCEPT);
		if (!it.hasNext())
			return true;
		while (it.hasNext())
		{
			String contentType = it.next();
			if (contentType.equals("") ||  supported.contains(contentType))
				return true;
		}
		SipServletResponse response = subscribe.createResponse(SipServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
		for (String s : supported)
			response.addHeader(Constants.ACCEPT, s);

		response.send();
		response.getApplicationSession().invalidate();
		return false;
	}
	
}
