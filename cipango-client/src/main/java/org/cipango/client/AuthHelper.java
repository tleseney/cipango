package org.cipango.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.servlet.ServletException;
import javax.servlet.sip.Address;
import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSessionEvent;
import javax.servlet.sip.SipSessionListener;

public class AuthHelper implements SipSessionListener
{
	/**
	 * Attribute of the SipServletRequest used to find from an authenticated request
	 * the original request from wich the request has been created.
	 */
	public static final String ORIGINAL_REQUEST = AuthHelper.class.getPackage().toString() + ".originalRequest";
	
	/**
	 * Attribute of the SipServletRequest used to find from an original request
	 * the authenticated request.
	 */
	public static final String AUTHENTICATED_REQUEST = AuthHelper.class.getPackage().toString() + ".authenticatedRequest";

	private static final List<String> SYSTEM_HEADERS = 
			Arrays.asList(SipHeaders.CALL_ID,
					SipHeaders.CONTACT, SipHeaders.FROM, SipHeaders.MAX_FORWARDS, SipHeaders.TO,
					SipHeaders.CSEQ, SipHeaders.VIA, SipHeaders.ROUTE, SipHeaders.RECORD_ROUTE,
					SipHeaders.CONTENT_TYPE, SipHeaders.CONTENT_LENGTH);
  
	
	private ConcurrentMap<String, List<Authentication>> _authentications = new ConcurrentHashMap<String, List<Authentication>>();
	private Map<String, Credentials> _credentials;
	
	
	/**
	 * Add the authentication headers to the request if its have been already set.
	 * @param request
	 * @throws ServletException
	 */
	public void addAuthentication(SipServletRequest request) throws ServletException
	{
		List<Authentication> auths = _authentications.get(request.getSession().getId());
		if (auths == null)
			return;
		
		// Check if auth already set.
		if (request.getHeader(SipHeaders.PROXY_AUTHORIZATION) != null
				|| request.getHeader(SipHeaders.AUTHORIZATION) != null)
			return; 
		
		for (Authentication auth : auths)
		{
			Credentials credentials = getCredentials(auth.getRealm());
			String authorization = auth.authorize(request.getMethod(),
					request.getRequestURI().toString(), 
					credentials);
			if (auth.isProxyAuthentication())
				request.addHeader(SipHeaders.PROXY_AUTHORIZATION, authorization);
			else
				request.addHeader(SipHeaders.AUTHORIZATION, authorization);
		}
	}
	
	public void addCredential(Credentials credentials)
	{
		_credentials.put(credentials.getRealm(), credentials);
	}
	
	/**
	 * Returns <code>true</code> if a challenge has been proceeded and a request has been sent else
	 * returns <code>false</code>.
	 * @param response
	 * @return
	 */
	public boolean handleChallenge(SipServletResponse response) throws ServletException, IOException
	{
		if (_credentials.isEmpty())
			return false;
		
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
			return true;
		}
		return false;
	}
	
	public SipServletRequest copy(SipServletRequest orig) throws IOException, ServletParseException
	{
		SipServletRequest request = orig.getSession().createRequest(orig.getMethod());
		Iterator<String> it = orig.getHeaderNames();
		while (it.hasNext())
		{
			String header = (String) it.next();
			if (!SYSTEM_HEADERS.contains(header)
					|| (SipMethods.REGISTER.equals(request.getMethod()) && SipHeaders.CONTACT.equals(header)))
			{
				for (Iterator<?> headers = orig.getHeaders(header); headers.hasNext(); ) 
				{
	                String value = (String)headers.next();
	                request.addHeader(header, value);
	            }
			}
		}
		if (orig.isInitial())
		{
			Iterator<Address> routes = orig.getAddressHeaders(SipHeaders.ROUTE);
			while (routes.hasNext())
				request.pushRoute(routes.next());
		}
		
		byte[] content = orig.getRawContent();
		if (content != null)
			request.setContent(content, orig.getHeader(SipHeaders.CONTENT_TYPE));
				
		SipServletRequest request2 = (SipServletRequest) orig.getAttribute(ORIGINAL_REQUEST);
		if (request2 == null)
			request2 = orig;
		request.setAttribute(ORIGINAL_REQUEST, request2);
		request2.setAttribute(AUTHENTICATED_REQUEST, request);
		
		return request;
	}
	
	private boolean handleAuthenticate(SipServletResponse response, String authenticate, boolean proxyAuth) throws ServletException
	{
		Authentication.Digest digest = Authentication.getDigest(authenticate);
		
		String authorization = getAuthorization(response.getRequest(), proxyAuth, digest.getRealm());
		if (authorization != null && !digest.isStale())
		{
			return false;
		}
		else
		{
			Authentication authentication = new Authentication(digest);
			authentication.setProxyAuthentication(proxyAuth);
			
			List<Authentication> auths = _authentications.get(response.getSession().getId());
			if (auths == null)
			{
				auths = new ArrayList<Authentication>();
				List<Authentication> l = _authentications.putIfAbsent(response.getSession().getId(), auths);
				if (l != null)
					auths = l;
			}
			if (digest.isStale())
			{
				Iterator<Authentication> it = auths.iterator();
				while (it.hasNext())
				{
					Authentication authentication2 = (Authentication) it.next();
					if (authentication2.getRealm().equals(authentication.getRealm()))
						it.remove();
				}		
			}
			
			auths.add(authentication);
			return true;
		}
		
		
	}
	
	private String getAuthorization(SipServletRequest request, boolean proxy, String realm)
	{
		Iterator<String> it = request.getHeaders(proxy ? SipHeaders.PROXY_AUTHENTICATE : SipHeaders.AUTHORIZATION);
		while (it.hasNext())
		{
			String authorization = it.next();
			if (authorization.contains(realm))
				return authorization;
		}
		return null;
	}
	
	public Credentials getCredentials(String realm)
	{
		Credentials credentials = _credentials.get(realm);
		if (credentials == null)
			credentials = _credentials.get("");
		return credentials;
	}
	
	/**
	 * Returns the original request from a request that may have been authentified.
	 * If the request has not been copied, it returns <code>request</code>.
	 * @see #ORIGINAL_REQUEST
	 */
	public SipServletRequest getOriginalRequest(SipServletRequest request)
	{
		SipServletRequest orig = (SipServletRequest) request.getAttribute(ORIGINAL_REQUEST);
		if (orig == null)
			return request;
		return orig;
	}
	
	/**
	 * Returns the authenticated request from a request.
	 * If the request has not been copied, it returns <code>request</code>.
	 * @see #AUTHENTICATED_REQUEST
	 */
	public SipServletRequest getAuthenticatedRequest(SipServletRequest request)
	{
		SipServletRequest authenticated = (SipServletRequest) request.getAttribute(AUTHENTICATED_REQUEST);
		if (authenticated == null)
			return request;
		return authenticated;
	}
	
	
	@Override
	public void sessionCreated(SipSessionEvent arg0)
	{
	}

	@Override
	public void sessionDestroyed(SipSessionEvent e)
	{
		_authentications.remove(e.getSession().getId());
	}

	@Override
	public void sessionReadyToInvalidate(SipSessionEvent arg0)
	{
	}
}
