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
		{
			synchronized (_responses)
			{
				_responses.add(new ReadableMessage<SipServletResponse>(response));
				_responses.notifyAll();
			}
		}
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
			synchronized (_requests)
			{
				for (ReadableMessage<SipServletRequest> request : _requests)
					if (request.getMessage().equals(message))
						request.setRead(true);
			}
		}
		else
		{
			synchronized (_responses)
			{
				for (ReadableMessage<SipServletResponse> response : _responses)
					if (response.getMessage().equals(message))
						response.setRead(true);
			}	
		}
	}
	
	protected SipServletRequest getUnreadRequest()
	{
		synchronized (_requests)
		{
			for (ReadableMessage<SipServletRequest> request : _requests)
			{
				if (!request.isRead())
					return request.getMessage();
			}
		}
		return null;
	}

	protected SipServletResponse getUnreadResponse()
	{
		synchronized (_responses)
		{
			for (ReadableMessage<SipServletResponse> response : _responses)
			{
				if (!response.isRead())
				{
					return response.getMessage();
				}
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
		return waitForResponse(markRead, getTimeout());
	}
	
	protected SipServletResponse waitForResponse(boolean markRead, long duration)
	{
		SipServletResponse response = getUnreadResponse();
		if (response == null)
		{
			doWait(_responses, duration);
			response = getUnreadResponse();
		}
		if (markRead)
			setRead(response);
		return response;
	}
	
	public SipServletResponse waitForFinalResponse()
	{
		long end = System.currentTimeMillis() + getTimeout();

		while (System.currentTimeMillis() <= end)
		{
			SipServletResponse response = waitForResponse(false, end - System.currentTimeMillis());
			if (response.getStatus() >= 200)
			{
				setRead(response);
				return response;
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

		@Override
		public String toString()
		{
			return _message.toString();
		}
	}
}
