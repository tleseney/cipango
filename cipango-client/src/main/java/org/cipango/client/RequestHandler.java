package org.cipango.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

/**
 * Handles a transaction, keeping track of all responses received in a list.
 * <p>
 * If credentials are provided, then authentication is automatically handled
 * with the help of a {@link AuthenticationHelper} throughout the whole
 * transaction. Responses with a challenge which can be successfully resolved
 * by the helper are not directly propagated to the user, but are recorded in
 * the request responses list anyway.
 */
public class RequestHandler extends AbstractChallengedMessageHandler
{
	private List<SipServletResponse> _responses = new ArrayList<SipServletResponse>();

	private int _read = 0;
	private SipServletRequest _request;

	
	public RequestHandler(SipServletRequest request, long timeout)
	{
		setTimeout(timeout);
		_request = request;
		_request.setAttribute(MessageHandler.class.getName(), this);
	}
	
	public void send() throws IOException, ServletException
	{
		AuthenticationHelper helper = getAuthenticationHelper(_request);
		if (helper != null)
			helper.addAuthentication(_request);

		_request.send();
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
	
	
	public SipServletResponse waitForNextResponse()
	{
		synchronized (this)
		{
			SipServletResponse response = getNextResponse();
			if (response != null)
				return response;
			doWait(this);
			return getNextResponse();

		}
	}
	
	public SipServletResponse waitForFinalResponse()
	{
		long end = System.currentTimeMillis() + getTimeout();
		synchronized (this)
		{
			while (System.currentTimeMillis() <= end)
			{
				SipServletResponse response = getLastResponse();
				if (response != null && response.getStatus() >= 200)
					return response;
				doWait(this, end - System.currentTimeMillis());
				response = getLastResponse();
				if (response != null && response.getStatus() >= 200)
					return response;
			}
		}
		return null;
	}
	
	public List<SipServletResponse> getResponses()
	{
		return new ArrayList<SipServletResponse>(_responses);
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
	public boolean handleAuthentication(SipServletResponse response)
			throws IOException, ServletException
	{
		boolean result = super.handleAuthentication(response);
		if (!result)
			_responses.add(response);
		return result;
	}
}
