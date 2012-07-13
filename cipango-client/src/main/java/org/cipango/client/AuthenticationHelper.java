package org.cipango.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import javax.servlet.ServletException;
import javax.servlet.sip.Address;
import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

public class AuthenticationHelper
{
	private static final List<String> SYSTEM_HEADERS = Arrays.asList(SipHeaders.CALL_ID,
			SipHeaders.CONTACT, SipHeaders.FROM, SipHeaders.MAX_FORWARDS, SipHeaders.TO,
			SipHeaders.CSEQ, SipHeaders.VIA, SipHeaders.ROUTE, SipHeaders.RECORD_ROUTE,
			SipHeaders.CONTENT_TYPE, SipHeaders.CONTENT_LENGTH);

	private static final List<String> AUTHORIZATION_HEADERS = Arrays.asList(
			SipHeaders.AUTHORIZATION, SipHeaders.PROXY_AUTHORIZATION);

	/**
	 * Attribute of the original <code>SipServletRequest</code> which holds the
	 * authenticated request created from it.
	 */
	public static final String ORIGINAL_REQUEST = AuthenticationHelper.class
			.getPackage().toString() + ".originalRequest";

	/**
	 * Attribute of the authenticated <code>SipServletRequest</code> which holds
	 * the original request.
	 */
	public static final String AUTHENTICATED_REQUEST = AuthenticationHelper.class
			.getPackage().toString() + ".authenticatedRequest";

	/**
	 * Attribute of a <code>SipServletRequest</code> which holds the
	 * AuthenticationHelper instance used to handle it.
	 */
	public static final String AUTH_HELPER = AuthenticationHelper.class
			.getPackage().getName();

	protected List<AuthenticationHolder> _authentications = new ArrayList<AuthenticationHolder>();
	protected List<Credentials> _credentials;
	
	public AuthenticationHelper(List<Credentials> credentials)
	{
		_credentials = credentials;
	}

	public Credentials getCredentials(String realm)
	{
		if (_credentials != null)
		{
    		for (Credentials creds : _credentials)
    		{
    			if (creds.getRealm().equals(realm))
    				return creds;
    		}
		}
		return null;
	}

	/**
	 * Returns the original request from which <code>request</code> was built.
	 * 
	 * @param request
	 *            a potentially authenticated request.
	 * @return the original request, cloned to build the given one, or
	 *         <code>request</code> if it was not built from another one.
	 * @see #ORIGINAL_REQUEST
	 */
	public SipServletRequest getOriginalRequest(SipServletRequest request)
	{
		SipServletRequest orig = (SipServletRequest) request
				.getAttribute(ORIGINAL_REQUEST);
		return (orig == null) ? request : orig;
	}

	/**
	 * Returns the authenticated request built from <code>request</code>.
	 * 
	 * @param request
	 * @return the latest authenticated request built from <code>request</code>,
	 *         or <code>request</code> if none was found.
	 * @see #AUTHENTICATED_REQUEST
	 */
	public SipServletRequest getAuthenticatedRequest(SipServletRequest request)
	{
		SipServletRequest authenticated = (SipServletRequest) request
				.getAttribute(AUTHENTICATED_REQUEST);
		return (authenticated == null) ? request : authenticated;
	}
	
	public boolean handleChallenge(SipServletResponse response) throws IOException, ServletException
	{
    	boolean handled = false;
    	
    	ListIterator<String> it = response.getHeaders(SipHeaders.WWW_AUTHENTICATE);
    	while (it.hasNext())
    	{
        	handled = handleAuthenticate(response, it.next(), false);
        	if (!handled)
        		return false;
    	}
    
    	it = response.getHeaders(SipHeaders.PROXY_AUTHENTICATE);
    	while (it.hasNext())
    	{
    		handled = handleAuthenticate(response, it.next(), true);
    		if (!handled)
    			return false;
    	}
    
    	if (handled)
    	{
    		SipServletRequest request = copy(response.getRequest());
    		addAuthentication(request);
    		request.send();
    	}

    	return handled;
	}

	public void addAuthentication(SipServletRequest request) throws ServletException
	{
		if (request.getHeader(SipHeaders.PROXY_AUTHORIZATION) != null
				|| request.getHeader(SipHeaders.AUTHORIZATION) != null)
			return;

		for (AuthenticationHolder holder : _authentications)
		{
			String authorization = holder.getAuthentication().authorize(
					request.getMethod(), request.getRequestURI().toString(),
					holder.getCredentials());

			if (holder.isProxy())
				request.addHeader(SipHeaders.PROXY_AUTHORIZATION, authorization);
			else
				request.addHeader(SipHeaders.AUTHORIZATION, authorization);
		}
	}
  	
	private boolean handleAuthenticate(SipServletResponse response,
			String authenticate, boolean proxy) throws ServletException
	{
		Authentication.Digest digest = Authentication.createDigest(authenticate);
		String authorization = getAuthorization(response.getRequest(), digest.getRealm(), proxy);

		if (authorization != null && !digest.isStale())
			return false;

		Credentials credentials = getCredentials(digest.getRealm());
		if (credentials == null)
			return false;
		
		if (digest.isStale())
		{
			Iterator<AuthenticationHolder> it = _authentications.iterator();
			while (it.hasNext())
			{
				AuthenticationHolder old = it.next();
				if (old.getAuthentication().getDigest().getRealm().equals(digest.getRealm()))
					it.remove();
			}
		}
		_authentications.add(new AuthenticationHolder(
				new Authentication(digest), credentials, proxy));

		return true;
	}

	private String getAuthorization(SipServletRequest request, String realm, boolean proxy)
	{
		Iterator<String> it = request.getHeaders(proxy ? SipHeaders.PROXY_AUTHENTICATE : SipHeaders.AUTHORIZATION);
		while (it.hasNext())
		{
			String authorization = it.next();

			// TODO: using String.contains is not sufficient here. 
			if (authorization.contains(realm))
				return authorization;
		}
		return null;
	}
	
	private SipServletRequest copy(SipServletRequest request) throws IOException, ServletParseException
	{
		SipServletRequest newRequest = request.getSession().createRequest(
				request.getMethod());
		Iterator<String> it = request.getHeaderNames();
		while (it.hasNext())
		{
			String header = (String) it.next();
			if (!SYSTEM_HEADERS.contains(header)
					&& !AUTHORIZATION_HEADERS.contains(header)
					|| (SipMethods.REGISTER.equals(request.getMethod())
							&& SipHeaders.CONTACT.equals(header)))
			{
				for (Iterator<?> headers = request.getHeaders(header); headers.hasNext();)
				{
					String value = (String) headers.next();
					newRequest.addHeader(header, value);
				}
			}
		}
		
		for (Enumeration<String> e = request.getAttributeNames(); e.hasMoreElements();)
		{
			String name = e.nextElement();
		       newRequest.setAttribute(name, request.getAttribute(name));
		}
		SipServletRequest origRequest = (SipServletRequest) request.getAttribute(ORIGINAL_REQUEST);
		if (origRequest == null)
			origRequest = request;
		newRequest.setAttribute(ORIGINAL_REQUEST, origRequest);
		origRequest.setAttribute(AUTHENTICATED_REQUEST, newRequest);

		if (request.isInitial())
		{
			Iterator<Address> routes = request.getAddressHeaders(SipHeaders.ROUTE);
			while (routes.hasNext())
				newRequest.pushRoute(routes.next());
		}

		newRequest.setRequestURI(request.getRequestURI());

		byte[] content = request.getRawContent();
		if (content != null)
			newRequest.setContent(content, request.getHeader(SipHeaders.CONTENT_TYPE));

		return newRequest;
	}

	private class AuthenticationHolder
	{
		private boolean _proxy;
		private Authentication _authentication;
		private Credentials _credentials;
		
		public AuthenticationHolder(Authentication authentication,
				Credentials credentials, boolean isProxy)
		{
			_authentication = authentication;
			_credentials = credentials;
			_proxy = isProxy;
		}

		public boolean isProxy()
		{
			return _proxy;
		}

		public Authentication getAuthentication()
		{
			return _authentication;
		}

		public Credentials getCredentials()
		{
			return _credentials;
		}
	};
}
