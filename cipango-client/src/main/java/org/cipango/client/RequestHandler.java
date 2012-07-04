package org.cipango.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.sip.Address;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;

public class RequestHandler implements MessageHandler
{
	private static final List<String> SYSTEM_HEADERS = Arrays.asList(SipHeaders.CALL_ID,
			SipHeaders.CONTACT, SipHeaders.FROM, SipHeaders.MAX_FORWARDS, SipHeaders.TO,
			SipHeaders.CSEQ, SipHeaders.VIA, SipHeaders.ROUTE, SipHeaders.RECORD_ROUTE,
			SipHeaders.CONTENT_TYPE, SipHeaders.CONTENT_LENGTH);

	public static final String HANDLED_ATTRIBUTE = RequestHandler.class.getName() + "-Handled";

	private List<SipServletResponse> _responses = new ArrayList<SipServletResponse>();
	private List<Credentials> _credentials;
	private int _read = 0;
	private SipServletRequest _request;
	private long _timeout;
	
	public RequestHandler(SipServletRequest request, long timeout)
	{
		_timeout = timeout;
		_request = request;
		_request.setAttribute(MessageHandler.class.getName(), this);
	}
	
	public RequestHandler(SipServletRequest request, UserAgent userAgent)
	{
		_timeout = userAgent.getTimeout();
		_request = request;
		_request.setAttribute(MessageHandler.class.getName(), this);
	}

	public List<Credentials> getCredentials()
	{
		return _credentials;
	}

	public void setCredentials(List<Credentials> credentials)
	{
		_credentials = credentials;
	}
	
	public void send() throws IOException
	{
		_request.send();
	}

	@Override
	public void handleRequest(SipServletRequest request)
			throws IOException, ServletException 
	{
		request.createResponse(SipServletResponse.SC_NOT_ACCEPTABLE_HERE).send();
	}

	@Override
	public void handleResponse(SipServletResponse response)
			throws IOException, ServletException 
	{
		_responses.add(response);
		synchronized (this)
		{
			notify();
		}
	}

	@Override
	public void handleAuthentication(SipServletResponse response)
			throws IOException, ServletException
	{
		if (_credentials != null && !_credentials.isEmpty())
		{
			String authorization = response.getRequest().getHeader(SipHeaders.AUTHORIZATION);
			String authenticate = response.getHeader(SipHeaders.WWW_AUTHENTICATE);
			Authentication.Digest digest = Authentication.getDigest(authenticate);

			if (authorization != null && !digest.isStale())
				return;

			for (Credentials creds : _credentials)
			{
				if (creds.getRealm() == digest._realm)
				{
		            SipServletRequest request = buildAuthenticatedRequest(
		            		response, new Authentication(digest), creds);
		            if (request != null)
		            	request.send();
				}
			}
		}
	}
		
	public SipServletResponse getNextResponse()
	{
		if (_responses.size() > _read)
		{
			return _responses.get(_read++);
		}
		return null;
	}
	
	public SipServletResponse getLastResponse()
	{
		if (_responses.isEmpty())
			return null;
		
		return _responses.get(_responses.size() - 1);
	}
	
	
	public SipServletResponse waitNextUnreadResponse()
	{
		synchronized (this)
		{
			doWait(_timeout);
			return getNextResponse();

		}
	}
	
	public SipServletResponse waitFinalResponse()
	{
		long end = System.currentTimeMillis() + _timeout;
		synchronized (this)
		{
			while (System.currentTimeMillis() <= end)
			{
				doWait(end - System.currentTimeMillis());
				SipServletResponse response = getLastResponse();
				if (response.getStatus() >= 200)
					return response;
			}
		}
		return null;
	}
	
	public List<SipServletResponse> getResponses()
	{
		return new ArrayList<SipServletResponse>(_responses);
	}
	
	protected void doWait(long timeout)
	{
		synchronized (this)
		{
			try { wait(timeout); } catch (InterruptedException e) {}
		}
	}
	
	// Should be moved to some toolkit package or class.
  	protected SipServletRequest buildAuthenticatedRequest(
  			SipServletResponse response, Authentication authentication,
  			Credentials credentials)
  			throws ServletException, IOException
	{
  		SipSession session = response.getSession();
		SipServletRequest orig = response.getRequest();
		SipServletRequest request = session.createRequest(response.getMethod());
		String authorization = authentication.authorize(orig.getMethod(),
				orig.getRequestURI().toString(), credentials);

		request.addHeader(SipHeaders.AUTHORIZATION, authorization);
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
		Iterator<Address> routes = orig.getAddressHeaders(SipHeaders.ROUTE);
		while (routes.hasNext())
			request.pushRoute(routes.next());
		
		byte[] content = orig.getRawContent();
		if (content != null)
			request.setContent(content, orig.getHeader(SipHeaders.CONTENT_TYPE));

		session.setAttribute(Authentication.class.getName(), authentication);
		session.setAttribute(Credentials.class.getName(), credentials);
		response.setAttribute(HANDLED_ATTRIBUTE, true);

		return request;
	}
}
