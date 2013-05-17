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
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.sip.B2buaHelper;
import javax.servlet.sip.ServletTimer;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipApplicationSessionEvent;
import javax.servlet.sip.SipApplicationSessionListener;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.TimerListener;
import javax.servlet.sip.TimerService;
import javax.servlet.sip.UAMode;
import javax.servlet.sip.SipSession.State;
/**
 * This click-to-dial service realizes the following call-flow in first approach.
 * <pre>
     A                Cipango                B
     |(1) INVITE no SDP  |                   |
     |<------------------|                   |
     |(2) 200 offer1     |                   |
     |------------------>|                   |
     |                   |(3) INVITE offer1  |
     |                   |------------------>|
     |                   |(4) 200 OK answer1 |
     |                   |<------------------|
     |                   |(5) ACK            |
     |                   |------------------>|
     |(6) ACK answer1    |                   |
     |<------------------|                   |
     |(7) RTP            |                   |
     |.......................................|
     | (8) BYE           |                   |
     |------------------>|(9) BYE            |
     |                   |------------------>|
     |                   |(10) 200/BYE       |
     |                   |<------------------|
     |(11) 200/BYE       |                   |
     |<------------------|                   |
  </pre>
  In reality, the first and third messages are sent back to the ProxyRegistrarServlet which resolves
  the AOR and proxy the message.
  This behavior is due to the default application router.
 */
public class ClickToDialSipServlet extends SipServlet implements ClickToDialService, TimerListener,SipApplicationSessionListener
{

	private static final String RESP_INV = "InviteResponse";
	
	private SipFactory _sipFactory;
	private TimerService _timerService;
	private List<SipApplicationSession> _calls;
	private B2buaHelper _b2bHelper;
	
	@Override
	public void init()
	{
		_sipFactory = (SipFactory) getServletContext().getAttribute(SIP_FACTORY);
		_timerService = (TimerService) getServletContext().getAttribute(TIMER_SERVICE);
		getServletContext().setAttribute(ClickToDialService.class.getName(), this);
		_calls = new ArrayList<SipApplicationSession>();
	}
		
	public void call(String from, String to) throws ServletException, IOException
	{
		SipApplicationSession session = _sipFactory.createApplicationSession();
		synchronized (_calls)
		{
			_calls.add(session);
		}	
		SipServletRequest request = _sipFactory.createRequest(session, "INVITE", from, to);
		if (_b2bHelper == null)
			_b2bHelper = request.getB2buaHelper();
		request.getSession().setHandler(getServletName());
		request.send();
	}

	@Override
	protected void doRequest(SipServletRequest request) throws ServletException, IOException
	{
		
		B2buaHelper b2bHelper = request.getB2buaHelper();
		SipSession leg2 = b2bHelper.getLinkedSession(request.getSession());
		
		// If the call is not established, refuse reINVITE and send CANCEL on BYE.
		if (leg2.getState() == State.EARLY || leg2.getState() == State.INITIAL)
		{
			String method = request.getMethod();
			if ("BYE".equals(method))
			{
				List<SipServletMessage> list = b2bHelper.getPendingMessages(leg2, UAMode.UAC);
				if (list.size() == 1)
				{
					SipServletRequest invite = (SipServletRequest) list.get(0);
					invite.createCancel().send();
					request.createResponse(SipServletResponse.SC_OK).send();
					leg2.getApplicationSession().invalidate();
				}
				else
					log("Could not found initial INVITE");
			}
			else
				request.createResponse(SipServletResponse.SC_REQUEST_PENDING).send();
		}
		else
			request.getB2buaHelper().createRequest(leg2, request, null).send();
	}

	
	@Override
	protected void doResponse(SipServletResponse response) throws ServletException, IOException
	{
		try 
		{
			if (response.getRequest().isInitial())
			{
				doInitialInviteResponse(response);
				return;
			}
			
			B2buaHelper b2bHelper = response.getRequest().getB2buaHelper();
			SipServletRequest peerRequest = b2bHelper.getLinkedSipServletRequest(response.getRequest());
		
			// Peer request could be null is case of 4xx/INVITE response by leg B.
			if (peerRequest != null)
			{
				peerRequest.createResponse(response.getStatus(), response.getReasonPhrase()).send();
			}
		}
		catch (Throwable e) 
		{
			log("Unable to process response:\n" + response, e);
		}
	}
	
	
	protected void doInitialInviteResponse(SipServletResponse response) throws ServletException, IOException
	{

		B2buaHelper b2bHelper = response.getRequest().getB2buaHelper();
		SipSession leg1 = response.getSession();
		SipSession leg2 = b2bHelper.getLinkedSession(leg1);
		int status = response.getStatus();
		if (leg2 == null)
		{
			if (status >= SipServletResponse.SC_OK 
					&& status < SipServletResponse.SC_MULTIPLE_CHOICES)
			{
				// Create second leg								
				SipServletRequest request = _sipFactory.createRequest(response.getApplicationSession(),
						response.getMethod(), 
						response.getTo(), 
						response.getFrom());
				copy(response, request);

				b2bHelper.linkSipSessions(leg1, request.getSession());

				request.setAttribute(RESP_INV, response);
				request.getSession().setHandler(getServletName());

				// if leg2 took more than 10s to pick up call, cancel it.
				ServletTimer cancelCallTask = _timerService.createTimer(leg1.getApplicationSession(), 15000,
						false, new CancelCallTask(leg1, request));
				
				leg1.setAttribute(CancelCallTask.class.getName(), cancelCallTask);
				
				request.send();
			}
			else if (response.getStatus() >= SipServletResponse.SC_MULTIPLE_CHOICES)
			{
				response.getApplicationSession().invalidate();
			}
		}
		else
		{
			if (status >= SipServletResponse.SC_OK 
					&& status < SipServletResponse.SC_MULTIPLE_CHOICES)
			{	
				((ServletTimer) leg2.getAttribute(CancelCallTask.class.getName())).cancel();
				
				// ACK to leg2
				SipServletResponse leg2Resp = (SipServletResponse) response.getRequest().getAttribute(RESP_INV);
				SipServletRequest request = leg2Resp.createAck();
				copy(response, request);
				request.send();
				
				// ACK to leg1
				response.createAck().send();
			}
			else if (status >= SipServletResponse.SC_MULTIPLE_CHOICES)
			{
				leg2.createRequest("BYE").send();
				leg1.getApplicationSession().invalidate();
			}	
		}
	}
	
	private void copy(SipServletMessage source, SipServletMessage destination) throws UnsupportedEncodingException, IOException
	{
		if (source.getContentLength() > 0)
		{
			destination.setContent(source.getRawContent(), source.getContentType());
		}
	}

	public void timeout(ServletTimer timer)
	{
		((Runnable) timer.getInfo()).run();
	}
	
	class CancelCallTask implements Runnable, Serializable
	{
		private SipSession _session;
		private SipServletRequest _request;
		
		public CancelCallTask(SipSession session, SipServletRequest request)
		{
			_session = session;
			_request = request;
		}

		public void run()
		{
			try
			{
				_session.createRequest("BYE").send();
				_request.createCancel().send();
				_session.getApplicationSession().invalidate();
			}
			catch (Throwable e)
			{
				log("Unable to cancel call", e);
			}
		}
	}

	public List<Call> getCalls()
	{
		List<Call> calls = new ArrayList<Call>();
		synchronized (_calls)
		{
			Iterator<SipApplicationSession> it = _calls.iterator();
			while (it.hasNext())
			{
				SipApplicationSession session = (SipApplicationSession) it.next();
				if (session.isValid())
					calls.add(new Call(session, _b2bHelper));
				else
				{
					log("Session " + session + " is not valid, so remove it");
					it.remove();
				}
			}
		}
		return calls;
	}

	public void sessionCreated(SipApplicationSessionEvent arg0)
	{
	}
	public void sessionDestroyed(SipApplicationSessionEvent e)
	{
		synchronized (_calls)
		{
			_calls.remove(e.getApplicationSession());
		}
	}

	public void sessionExpired(SipApplicationSessionEvent e)
	{
	}

	public void sessionReadyToInvalidate(SipApplicationSessionEvent e)
	{	
	}


}
