// ========================================================================
// Copyright 2012 NEXCOM Systems
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
package org.cipango.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serializable;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.sip.Address;
import javax.servlet.sip.AuthInfo;
import javax.servlet.sip.B2buaHelper;
import javax.servlet.sip.Proxy;
import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.TooManyHopsException;
import javax.servlet.sip.URI;
import javax.servlet.sip.ar.SipApplicationRouterInfo;
import javax.servlet.sip.ar.SipApplicationRoutingDirective;
import javax.servlet.sip.ar.SipApplicationRoutingRegion;

import org.cipango.server.session.SessionManager.ApplicationSessionScope;
import org.cipango.server.session.scoped.ScopedServerTransactionListener;
import org.cipango.server.transaction.ClientTransaction;
import org.cipango.server.transaction.ServerTransaction;
import org.cipango.server.transaction.Transaction;
import org.cipango.sip.AddressImpl;
import org.cipango.sip.SipFields.Field;
import org.cipango.sip.RAck;
import org.cipango.sip.SipGenerator;
import org.cipango.sip.SipHeader;
import org.cipango.sip.SipMethod;
import org.eclipse.jetty.util.StringUtil;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class SipRequest extends SipMessage implements SipServletRequest 
{
	private static final Logger LOG = Log.getLogger(SipRequest.class);
	private static boolean __strictRoutingEnabled = false;
	
	private URI _requestUri;
	
	private Transaction _transaction;
	private Address _poppedRoute;
    private Address _initialPoppedRoute;
	
	private Serializable _stateInfo;    
	private SipApplicationRouterInfo _nextRouterInfo;
    private SipApplicationRoutingDirective _directive;
    private SipApplicationRoutingRegion _region;
    private URI _subscriberURI;
    
    private Proxy _proxy;
    private B2buaHelper _b2bHelper;
	private SipRequest _linkedRequest;
	
    private boolean _nextHopStrictRouting = false;
    private boolean _handled = false;
	
    public SipRequest()
    {
    }
    
    public SipRequest(SipRequest other)
    {
    	super(other);
    	_requestUri = other._requestUri.clone();
    	_stateInfo = other._stateInfo;
    	_nextRouterInfo = other._nextRouterInfo;
    	_directive = other._directive;
    	_subscriberURI = other._subscriberURI;
    	_initialPoppedRoute = other._initialPoppedRoute;
    }

	public boolean isRequest()
	{
		return true;
	}
	
	public void setMethod(SipMethod sipMethod, String method)
	{
		_sipMethod = sipMethod;
		_method = method;
	}
	
	/**
	 * @see SipServletMessage#getMethod()
	 */
	public String getMethod()
	{
		return _method;
	}
	
	
	public void setTransaction(Transaction transaction)
	{
		_transaction = transaction;
	}
	
	public Transaction getTransaction()
	{
		return _transaction;
	}
	
	protected boolean canSetContact()
	{
		return _sipMethod == SipMethod.REGISTER;
	}
	
	/**
	 * @see SipServletRequest#send()
	 */
	public void send() throws IOException
	{
		if (isCommitted())
            throw new IllegalStateException("Request is already commited");
        if (getTransaction() != null && !(getTransaction() instanceof ClientTransaction))
        	throw new IllegalStateException("Can send request only in UAC mode");
    	setCommitted(true);

		// Need a scope here as send() can be done outside of a managed thread or from a new UAC session
    	ApplicationSessionScope scope = appSession().getSessionManager().openScope(appSession());
    	try
    	{
	    	if (isCancel())
	    		((ClientTransaction) getTransaction()).cancel(this);
	    	else
	    		_session.sendRequest(this);
    	}
    	finally
    	{
    		scope.close();
    	}
	}
	
	public Address getTopRoute() throws ServletParseException
	{
		Field field = _fields.getField(SipHeader.ROUTE);
		if (field == null)
			return null;
		return field.asAddress();
	}
	
	public Address removeTopRoute()
	{
		return (Address) _fields.removeFirst(SipHeader.ROUTE);
	}
	
	public void setPoppedRoute(Address route)
	{
		_poppedRoute = route;
	}
	
	public void addRecordRoute(Address route) 
    {
		_fields.add(SipHeader.RECORD_ROUTE.asString(), route, true);
	}
	
	@Override
	public SipServletRequest createCancel() 
	{
		if (getTransaction().isCompleted())
			throw new IllegalStateException("Transaction has completed");
		
		SipRequest cancel = createRequest(SipMethod.CANCEL);
		cancel.to().removeParameter(AddressImpl.TAG);
		
		return cancel;
	}
	
	public SipRequest createRequest(SipMethod method) 
    {
		SipRequest request = new SipRequest();
		
		request._session = _session;
        request.setTransaction(getTransaction());
		request._fields.set(SipHeader.FROM, getFrom().clone());
		request._fields.set(SipHeader.TO, to().clone());
		
		request.setMethod(method, method.asString());
		request.setRequestURI(getRequestURI());
		request._fields.copy(_fields, SipHeader.CALL_ID);
		
        request._fields.set(SipHeader.CSEQ, getCSeq().getNumber() + " " + method.asString());
           
		request._fields.set(SipHeader.VIA, getTopVia());
		request._fields.copy(_fields, SipHeader.MAX_FORWARDS);
        request._fields.copy(_fields, SipHeader.ROUTE);
		
		return request;
	}
	
	/**
	 * @see SipServletRequest#createResponse(int)
	 */
	public SipServletResponse createResponse(int status) 
	{
		return createResponse(status, null);
	}
	
	/**
	 * @see SipServletRequest#createResponse(int, String)
	 */
	public SipServletResponse createResponse(int status, String reason) 
	{
		if (isAck())
			throw new IllegalStateException("Cannot create response to ACK");
		
		if (!(getTransaction() instanceof ServerTransaction))
    		throw new IllegalStateException("Cannot create response if not in UAS mode");
    	
    	if (getTransaction().isCompleted()) 
    		throw new IllegalStateException("Cannot create response if final response has been sent");
	
		return new SipResponse(this, status, reason);
	}
	
	
	//
	
	public boolean needsContact() 
    {
    	return isInvite() || isSubscribe() || isMethod(SipMethod.NOTIFY) || isMethod(SipMethod.REFER) || isMethod(SipMethod.UPDATE);
    }
	
	public SipURI getParamUri()
	{
		if (_poppedRoute != null)
			return (SipURI) _poppedRoute.getURI();
		else if (_requestUri.isSipURI())
			return (SipURI) _requestUri;
		else
			return null;
	}
	
	public String getParameter(String name) 
	{
		SipURI paramUri = getParamUri();
		
		if (paramUri == null) 
			return null;
		
		return paramUri.getParameter(name);
	}
	
	@Override
	public Enumeration<String> getParameterNames() 
	{
		SipURI paramUri = getParamUri();
		
		if (paramUri == null) 
			return Collections.emptyEnumeration();
		
		return new IteratorToEnum(paramUri.getParameterNames());
	}
	
	@Override
	public String[] getParameterValues(String name) 
	{
		String value = getParameter(name);
		if (value == null) 
			return null;
		
		return new String[] {value};
	}
	
	@Override
	public Map<String, String[]> getParameterMap() 
	{
		Map<String, String[]> map = new HashMap<String, String[]>();
		
		SipURI paramUri = getParamUri();
		
		if (paramUri != null) 
        {
			Iterator<String> it = paramUri.getParameterNames();
			while (it.hasNext()) 
            {
				String key = it.next();
				map.put(key, new String[] {paramUri.getParameter(key)});
			}
		}
		return Collections.unmodifiableMap(map);
	}
	
	@Override
	public String getScheme() 
	{
		return _requestUri.getScheme();
	}
	
	@Override
	public String getServerName() 
	{
		return getLocalName();
	}
	
	
	/**
	 * @see SipServletRequest#getServerPort()
	 */
	public int getServerPort() 
	{
		return getLocalPort();
	}
	
	/**
	 * @see SipServletRequest#getRemoteHost()
	 */
	public String getRemoteHost() 
	{
		return getRemoteAddr();
	}
	
	@Override
	public Locale getLocale()
	{
		return getAcceptLanguage();
	}

	@Override
	public Enumeration<Locale> getLocales()
	{
		final Iterator<Locale> it = getAcceptLanguages();
		return new Enumeration<Locale>()
		{
			public boolean hasMoreElements()
			{
				return it.hasNext();
			}

			public Locale nextElement()
			{
				return it.next();
			}
		};
	}
	
	public RAck getRAck() throws ServletParseException
	{
		String s = getFields().getString(SipHeader.RACK);
		return (s == null)? null: new RAck(s);
	}
	
	@Override
	public RequestDispatcher getRequestDispatcher(String path) {
		throw new UnsupportedOperationException("Not Applicable");
	}
	@Override
	public String getRealPath(String path) 
	{
		return null;
	}
	@Override
	public String getLocalName() 
	{
		return getConnection() != null ? getConnection().getLocalAddress().getHostName() : null;
	}

	@Override
	public void addAuthHeader(SipServletResponse response, AuthInfo authInfo)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void addAuthHeader(SipServletResponse response, String username, String password)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public B2buaHelper getB2buaHelper()
	{
		if (_b2bHelper != null)
			return _b2bHelper;

		if (_proxy != null)
			throw new IllegalStateException("getProxy() had already been called");

		_b2bHelper = B2bHelper.getInstance();
		if (!_session.isUA() && getTransaction().isServer())
		{
			_session.setUAS();
			((ServerTransaction) getTransaction()).setListener(new ScopedServerTransactionListener(session(), _session.getUa()));
		}
		return _b2bHelper;
	}

	@Override
	public Address getInitialPoppedRoute()
	{
		if (_initialPoppedRoute == null)
			return _poppedRoute;
		return _initialPoppedRoute;
	}
	
	public void setInitialPoppedRoute(Address route)
	{
		_initialPoppedRoute = route;
	}
	
	@Override
	public ServletInputStream getInputStream() throws IOException 
	{
		return null;
	}
	@Override
	public int getMaxForwards() 
	{
		return (int) _fields.getLong(SipHeader.MAX_FORWARDS);
	}
	
	@Override
	public Address getPoppedRoute() 
	{
		return _poppedRoute;
	}
	
	@Override
	public Proxy getProxy() throws TooManyHopsException 
	{
		return getProxy(true);
	}
	
	@Override
	public Proxy getProxy(boolean create) throws TooManyHopsException
	{
		if (_proxy != null || !create)
			return _proxy;

		if (_b2bHelper != null)
			throw new IllegalStateException("getB2buaHelper() had already been called");

		if (!_transaction.isServer())
			throw new IllegalStateException("Not a received request");

		_session.setProxy();

		if (_proxy == null)
		{
			SipProxy proxy = new SipProxy(this);
			proxy.setProxyTimeout(appSession().getContext().getProxyTimeout());
			_proxy = proxy;
		}
		return _proxy;
	}
	
	public void setProxy(Proxy proxy)
	{
		_proxy = proxy;
	}
	
	@Override
	public BufferedReader getReader() throws IOException
	{
		return null;
	}
	
	@Override
	public SipApplicationRoutingRegion getRegion() 
	{
		return _region;
	}
	
	@Override
	public URI getRequestURI() 
	{
		return _requestUri;
	}
	
	public SipApplicationRoutingDirective getRoutingDirective() throws IllegalStateException
	{
		if (!isInitial())
			throw new IllegalStateException("SipServletRequest is not initial");
		return _directive;
	}

	public void setRoutingDirective(SipApplicationRoutingDirective directive, SipServletRequest origRequest)
			throws IllegalStateException
	{
		if (!isInitial())
			throw new IllegalStateException("SipServletRequest is not initial");
		if (directive != SipApplicationRoutingDirective.NEW && 
				(origRequest == null || !origRequest.isInitial()))
			throw new IllegalStateException("origRequest is not initial");
		if (isCommitted())
			throw new IllegalStateException("SipServletRequest is committed");
		_directive = directive;
	}
	
	@Override
	public URI getSubscriberURI() 
	{
		return _subscriberURI;
	}
	
	public boolean isInitial() 
	{
		return getToTag() == null && !isCancel();
	}
	
	@Override
	public void pushPath(Address arg0) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void pushRoute(SipURI route) 
	{
		pushRoute(new AddressImpl(route));
	}
	@Override
	public void pushRoute(Address route) 
	{
		if (!route.getURI().isSipURI())
    		throw new IllegalArgumentException("Only routes with a SIP URI may be pushed"); 
		
		boolean strictRouting = !((SipURI) route.getURI()).getLrParam();
		
		if (strictRouting && !isStrictRoutingEnabled())
			throw new IllegalArgumentException("Route does not contains lr param and strict routing disabled");
		
		if (isNextHopStrictRouting())
		{
			if (strictRouting)
			{
				_fields.add(SipHeader.ROUTE.asString(), new AddressImpl(getRequestURI()), true);
				setRequestURI(route.getURI());
			}
			else
			{
				Address lastRoute = removeLastRoute();
				_fields.add(SipHeader.ROUTE.asString(), new AddressImpl(getRequestURI()), true);	
				setRequestURI(lastRoute.getURI());
				_fields.add(SipHeader.ROUTE.asString(), route, true);
			}
		}
		else if (strictRouting)
		{
			_fields.add(SipHeader.ROUTE.asString(), new AddressImpl(getRequestURI()), false);
			setRequestURI(route.getURI());
		}
		else
		{
			_fields.add(SipHeader.ROUTE.asString(), route, true);
		}
		setNextHopStrinctRouting(strictRouting);
	}
	
	// For strict routing
	public Address removeLastRoute()
	{
		try
		{
			Iterator<Address> it = _fields.getAddressValues(SipHeader.ROUTE, null);
			List<Address> list = new ArrayList<Address>();
			Address lastRoute = null;
			while (it.hasNext())
			{
				Address route = it.next();
				if (it.hasNext())
					list.add(route);
				else
					lastRoute = route;
			}
			it = list.iterator();
			if (it.hasNext())
				_fields.set(SipHeader.ROUTE, it.next());
			else
				return removeTopRoute();

			while (it.hasNext())
			{
				Address route = it.next();
				_fields.add(SipHeader.ROUTE.asString(), route, false);
			}
			return lastRoute;
		}
		catch (ServletParseException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	public void setNextHopStrinctRouting(boolean nextHopStrictRouting)
	{
		_nextHopStrictRouting = nextHopStrictRouting;
	}
	
	public boolean isNextHopStrictRouting()
	{
		return _nextHopStrictRouting;
	}
	
	@Override
	public void setMaxForwards(int maxForwards) 
	{
		if (maxForwards < 0 || maxForwards > 255) 
    		throw new IllegalArgumentException("Max-Forwards should be between 0 and 255");
    
    	_fields.set(SipHeader.MAX_FORWARDS, Long.toString(maxForwards));
	}
	@Override
	public void setRequestURI(URI uri) 
	{
		if (uri == null)
			throw new NullPointerException("Null URI");
		_requestUri = uri;
	}

	@Override
	public AsyncContext getAsyncContext()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DispatcherType getDispatcherType()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ServletContext getServletContext()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isAsyncStarted()
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isAsyncSupported()
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public AsyncContext startAsync() throws IllegalStateException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AsyncContext startAsync(ServletRequest arg0, ServletResponse arg1) throws IllegalStateException
	{
		// TODO Auto-generated method stub
		return null;
	}

    public SipRequest getLinkedRequest()
    {
    	return _linkedRequest;
    }
    
    public void setLinkedRequest(SipRequest request)
    {
    	_linkedRequest = request;
    }
    
	public Serializable getStateInfo()
	{
		return _stateInfo;
	}
	
	public void setStateInfo(Serializable stateInfo)
	{
		_stateInfo = stateInfo;
	}
	
	public void setRegion(SipApplicationRoutingRegion region)
	{
		_region = region;
	}
	
	public void setSubscriberURI(URI uri)
	{
		_subscriberURI = uri;
	}
	public boolean isHandled()
	{
		return _handled;
	}

	public void setHandled(boolean handled)
	{
		_handled = handled;
	}
	
	@Override
	public String toString()
	{
		ByteBuffer buffer = null;
		int bufferSize = 4096 + getContentLength();
		
		while (true)
		{
			buffer = ByteBuffer.allocate(bufferSize);

			try {
				new SipGenerator().generateRequest(buffer, _method, _requestUri, _fields, getRawContent(), getHeaderForm());
				return new String(buffer.array(), 0, buffer.position(), StringUtil.__UTF8_CHARSET);

			}
			catch (BufferOverflowException e)
			{
				bufferSize += 4096 + getContentLength();
			}
		}
	}
	
	@Override
	public String toStringCompact()
	{
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		new SipGenerator().generateRequestLine(buffer, _method, _requestUri);
		Field field = getFields().getField(SipHeader.CALL_ID);
		if (field != null)
			field.putTo(buffer, HeaderForm.DEFAULT);
		return new String(buffer.array(), 0, buffer.position(), StringUtil.__UTF8_CHARSET);
	}
	
	static class IteratorToEnum  implements Enumeration<String>
	{
		private Iterator<String> _it;
		public IteratorToEnum(Iterator<String> it)
		{
			_it = it;
		}

		public boolean hasMoreElements()
		{
			return _it.hasNext();
		}

		public String nextElement()
		{
			return _it.next();
		}
	}

	public static boolean isStrictRoutingEnabled()
	{
		return __strictRoutingEnabled;
	}

	public static void setStrictRoutingEnabled(boolean strictRoutingEnabled)
	{
		__strictRoutingEnabled = strictRoutingEnabled;
	}

}
