package org.cipango.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

public class RequestHandler implements MessageHandler
{
	private List<SipServletResponse> _responses = new ArrayList<SipServletResponse>();
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

	public void send() throws IOException
	{
		_request.send();
	}

	@Override
	public void handleRequest(SipServletRequest request) throws IOException, ServletException 
	{
		request.createResponse(SipServletResponse.SC_NOT_ACCEPTABLE_HERE).send();
	}

	@Override
	public void handleResponse(SipServletResponse response) throws IOException, ServletException 
	{
		_responses.add(response);
		synchronized (this)
		{
			notify();
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
			SipServletResponse response = getNextResponse();
			if (response != null)
				return response;
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
		
	
}
