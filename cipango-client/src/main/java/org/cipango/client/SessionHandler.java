package org.cipango.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

public class SessionHandler extends AbstractChallengedMessageHandler
{
	protected List<ReadableMessage<SipServletRequest>> _requests = new ArrayList<ReadableMessage<SipServletRequest>>();
	protected List<ReadableMessage<SipServletResponse>> _responses = new ArrayList<ReadableMessage<SipServletResponse>>();
	
	@Override
	public void handleRequest(SipServletRequest request) throws IOException, ServletException
	{
		synchronized (_requests)
		{
			_requests.add(new ReadableMessage<SipServletRequest>(request));
			_requests.notifyAll();
		}
	}

	@Override
	public void handleResponse(SipServletResponse response) throws IOException, ServletException
	{
		synchronized (_responses)
		{
			_responses.add(new ReadableMessage<SipServletResponse>(response));
			_responses.notifyAll();
		}
	}
	
	@Override
	public boolean handleAuthentication(SipServletResponse response)
			throws IOException, ServletException
	{
		boolean result = super.handleAuthentication(response);
		if (result)
			_responses.add(new ReadableMessage<SipServletResponse>(response));
		return result;
	}
	
	public void send(SipServletRequest request) throws IOException, ServletException
	{
		AuthenticationHelper helper = getAuthenticationHelper(request);
		if (helper != null)
			helper.addAuthentication(request);

		request.send();
	}
	
	public SipServletRequest waitForRequest()
	{
		return waitForRequest(true);
	}
	
	protected SipServletRequest waitForRequest(boolean markRead)
	{
		SipServletRequest request = getUnreadRequest();
		
		if (request == null)
		{
			doWait(_requests);
			request = getUnreadRequest();
		}

		if (markRead)
			setRead(request);
		return request;
	}
	
	protected void setRead(SipServletMessage message)
	{
		if (message == null)
			return;
		
		if (message instanceof SipServletRequest)
		{
			for (ReadableMessage<SipServletRequest> request : _requests)
				if (request.getMessage().equals(message))
					request.setRead(true);
		}
		else
		{
			for (ReadableMessage<SipServletResponse> response : _responses)
				if (response.getMessage().equals(message))
					response.setRead(true);
		}
	}
	
	protected SipServletRequest getUnreadRequest()
	{
		for (ReadableMessage<SipServletRequest> request : _requests)
		{
			if (!request.isRead())
				return request.getMessage();
		}
		return null;
	}

	protected SipServletResponse getUnreadResponse()
	{
		for (ReadableMessage<SipServletResponse> response : _responses)
		{
			if (!response.isRead())
			{
				return response.getMessage();
			}
		}
		return null;
	}
	
	public SipServletRequest getLastRequest()
	{
		synchronized (_requests)
		{
			if (_requests.isEmpty())
				return null;
			return _requests.get(_requests.size() - 1).getMessage();
		}
	}
	
	public SipServletResponse getLastResponse()
	{
		synchronized (_responses)
		{
			if (_responses.isEmpty())
				return null;
			return _responses.get(_responses.size() - 1).getMessage();
		}
	}

	public SipServletResponse waitForResponse()
	{
		return waitForResponse(true);
	}
	
	protected SipServletResponse waitForResponse(boolean markRead)
	{
		SipServletResponse response = getUnreadResponse();
		if (response == null)
		{
			doWait(_responses);
			response = getUnreadResponse();
		}
		if (markRead)
			setRead(response);
		return response;
	}
	
	public SipServletResponse waitForFinalResponse()
	{
		long end = System.currentTimeMillis() + getTimeout();
		synchronized (this)
		{
			while (System.currentTimeMillis() <= end)
			{
				doWait(_responses, end - System.currentTimeMillis());
				SipServletResponse response = waitForResponse(false);
				if (response.getStatus() >= 200)
				{
					setRead(response);
					return response;
				}
			}
		}
		return null;
	}
	
	public List<SipServletRequest> getRequests()
	{
		synchronized (_requests)
		{
			List<SipServletRequest> l = new ArrayList<SipServletRequest>(_requests.size());
			for (ReadableMessage<SipServletRequest> request : _requests)
				l.add(request.getMessage());
			return l;
		}	
	}

	public List<SipServletResponse> getResponses()
	{
		synchronized (_responses)
		{
			List<SipServletResponse> l = new ArrayList<SipServletResponse>(_responses.size());
			for (ReadableMessage<SipServletResponse> response : _responses)
				l.add(response.getMessage());
			return l;
		}
	}
	
	protected static class ReadableMessage<E extends SipServletMessage>
	{
		private long _received;
		private boolean _read;
		private E _message;
		
		public ReadableMessage(E message)
		{
			_received = System.currentTimeMillis();
			_message = message;
			_read = false;
		}

		public boolean isRead()
		{
			return _read;
		}

		public void setRead(boolean read)
		{
			_read = read;
		}

		public E getMessage()
		{
			return _message;
		}

		public long getReceived()
		{
			return _received;
		}
	}
}
