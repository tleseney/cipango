// ========================================================================
// Copyright 2007-2009 NEXCOM Systems
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
package org.cipango.example;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;

import javax.servlet.ServletException;
import javax.servlet.sip.Address;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipURI;

public class ProxyRegistrarServlet extends SipServlet
{
	private static final long serialVersionUID = 5425892819952843296L;
	
	private int _minExpires = 60;
	private int _maxExpires = 86400;
	private int _defaultExpires = 3600;
	
	private Map<String, SortedSet<Binding>> _bindings;
	private SipFactory _sipFactory;
	private Timer _timer;
	private DateFormat _dateFormat;
	
	@Override
	public void init()
	{
		_sipFactory = (SipFactory) getServletContext().getAttribute(SipServlet.SIP_FACTORY);
		_bindings = new HashMap<String, SortedSet<Binding>>();
		
		getServletContext().setAttribute(Binding.class.getName(), _bindings);
		
		_timer = new Timer();
		_timer.scheduleAtFixedRate(new BindingScavenger(), 5000, 5000);
		
		_dateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z", Locale.US);
		_dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
	}
	
	public String getAor(Address address)
	{
		SipURI uri = (SipURI) address.getURI();
		return "sip:" + uri.getUser() + "@" + uri.getHost().toLowerCase(); 
	}
	
	protected SortedSet<Binding> getBindings(String aor, boolean create)
	{
		SortedSet<Binding> bindings;
		synchronized (_bindings)
		{
			bindings = _bindings.get(aor);
			
			if (bindings == null && create)
			{
				bindings = new TreeSet<Binding>();
				_bindings.put(aor, bindings);
			}
		}
		return bindings;
	}
	
	@Override
	protected void doRegister(SipServletRequest register) throws IOException, ServletException
	{
		try 
		{
			String aor = getAor(register.getTo());
			
			SortedSet<Binding> bindings = getBindings(aor, true);
			
			synchronized (bindings)
			{
				Iterator<Address> it = register.getAddressHeaders("Contact");
				
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
						String s = register.getHeader("cseq");
						cseq = Integer.parseInt(s.substring(0, s.indexOf(' ')));
					}
					catch (Exception e)
					{
						register.createResponse(SipServletResponse.SC_BAD_REQUEST, e.getMessage()).send();
						return;
					}
					
					if (wildcard)
					{
						for (Binding binding : bindings)
						{
							if (callId.equals(binding.getCallId()) && cseq < binding.getCseq())
							{
								register.createResponse(SipServletResponse.SC_SERVER_INTERNAL_ERROR,
										"Lower cseq").send();
								return;
							}
							bindings.remove(binding);
						}
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
								{
									expires = _maxExpires;
								}
								else if (expires < _minExpires)
								{
									SipServletResponse response = register.createResponse(423);
									response.addHeader("Min-Expires", Integer.toString(_minExpires));
									response.send();
									return;
								}
							}
							boolean exist = false;
							
							Iterator<Binding> it2 = bindings.iterator();
							while (it2.hasNext())
							{
								Binding binding = it2.next();
								
								if (contact.getURI().equals(binding.getContact()))
								{
									exist = true;
									if (callId.equals(binding.getCallId()) && cseq < binding.getCseq())
									{
										register.createResponse(SipServletResponse.SC_SERVER_INTERNAL_ERROR, 
												"Lower CSeq").send();
										return;
									}
									if (expires == 0)
									{
										it2.remove();
									}
									else 
									{
										binding.setCallId(callId);
										binding.setCseq(cseq);
										binding.setExpires(expires);
										binding.setQ(contact.getQ());
									}
								}
							}
							if (!exist && expires != 0)
							{
								Binding binding = new Binding(aor, contact.getURI());
								binding.setCallId(callId);
								binding.setCseq(cseq);
								binding.setExpires(expires);
								binding.setQ(contact.getQ());
								bindings.add(binding);
							}
						}
					}
				}
				
				// Empty bindings set are removed in the scavenger to prevent deadlocks.
				
				SipServletResponse ok = register.createResponse(SipServletResponse.SC_OK);
				if (bindings.size() > 0)
				{
					for (Binding binding : bindings)
					{
						Address address = _sipFactory.createAddress(binding.getContact());
						address.setExpires(binding.getExpires());
						address.setQ(binding.getQ());
						ok.addAddressHeader("contact", address, false);
					}
				}
				ok.addHeader("Date", _dateFormat.format(new Date()));
				ok.send();
			}	
		}
		catch (Throwable e) {
			log("Caught unexpected exception on REGISTER processing", e);
			SipServletResponse response = register.createResponse(SipServletResponse.SC_SERVER_INTERNAL_ERROR);
			response.setContent(e, "text/stackTrace");
			response.send();
		}
		finally
		{
			register.getApplicationSession().invalidate();
		}
	}
	
	protected void proxy(SipServletRequest request) throws ServletException, IOException
	{
		if (!request.isInitial())
			return;
		
		String aor = getAor(request.getTo());
		
		SortedSet<Binding > bindings = getBindings(aor, false);
				
		if (bindings != null && bindings.size() > 0)
		{
			synchronized (bindings)
			{
				Binding binding = bindings.first();
				request.getProxy().proxyTo(binding.getContact());
			}
		}
		else
		{
			request.createResponse(SipServletResponse.SC_NOT_FOUND).send();
		}	
	}

	@Override
	protected void doRequest(SipServletRequest request) throws ServletException, IOException
	{
		String method = request.getMethod();
		if ("REGISTER".equalsIgnoreCase(method))
			doRegister(request);
		else
			proxy(request);		
	}
	
	@Override
	public void destroy()
	{
		_timer.cancel();
	}
	
	class BindingScavenger extends TimerTask 
	{
		public void run() {
			synchronized (_bindings)
			{
				Iterator<SortedSet<Binding>> it = _bindings.values().iterator();
				long now = System.currentTimeMillis();
				while (it.hasNext()) {
					SortedSet<Binding> list = it.next();
					synchronized (list)
					{
						Iterator<Binding> it2 = list.iterator();
						while (it2.hasNext()) {
							Binding binding = (Binding) it2.next();
							if (binding.getAbsoluteExpires() <= now)
								it2.remove();
						}
						if (list.isEmpty())
							it.remove();
					}
				}
			}
		}
	}
}
