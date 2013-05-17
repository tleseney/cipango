// ========================================================================
// Copyright 2008-2009 NEXCOM Systems
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

package org.cipango.kaleo.event;

import java.util.LinkedList;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipSession;

import org.cipango.kaleo.Constants;
import org.cipango.kaleo.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Notifier<T extends Resource> //implements Runnable
{
	/*
	private final Logger _log = LoggerFactory.getLogger(Notifier.class);
	
	private EventPackage<T> _eventPackage;
	
	private Thread _thread;
	private LinkedList<Notification> _notifications = new LinkedList<Notification>();
	
	public void notify(EventPackage<T> eventPackage, SipSession session)
	{
		Notification notification = new Notification();
		
		synchronized (_notifications)
		{
			_notifications.addLast(notification);
			_notifications.notifyAll();
		}
	}
	
	public void start()
	{
		_thread = new Thread(this);
		_thread.start();
	}
	
	public void run()
	{
		Thread.currentThread().setName("notifier");
		while (true)
		{
			Notification notification = null;
			try
			{
				synchronized (_notifications)
				{
					while (_notifications.isEmpty())
					{
						_notifications.wait();
					}
					notification = _notifications.removeFirst();
				}
				try
				{
					send(notification);
				}
				catch (Exception e)
				{
					_log.warn("Exception while sending notification {}", e);
				}
			}
			catch (InterruptedException e)
			{
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	protected void send(Notification notification)
	{
		try
		{
			SipSession session = notification.getSession();
			if (session.isValid())
			{
				SipServletRequest notify = session.createRequest(Constants.NOTIFY);
				notify.addHeader(Constants.EVENT, _eventPackage.getName());
				String type = notification.getContent().getType();
				
				ContentHandler handler = _eventPackage.getContentHandler(type);
				byte[] b = handler.getBytes(notification.getContent().getValue());
				notify.setContent(b, type);
				notify.send();
			}
		}
		catch (Exception e) 
		{
			_log.warn("Exception while sending notification {}", e);
		}
	}
	
	class Notification
	{
		private SipSession _session;
		private Resource.Content _content;
		
		public SipSession getSession() { return _session; }
		public Resource.Content getContent() { return _content; }
	}*/
}

/*
	private void sendNotify(Notification notification) 
	{
		try 
		{
			SipSession session = notification.getSession();
			if(session.isValid())
			{
				SipServletRequest notify = session.createRequest("NOTIFY");
				String type = notification.getContent().getType();
				ContentHandler handler = notification.getEventPackage().getContentHandler(type);
				byte[] b = handler.getBytes(notification.getContent().getValue());
				notify.addParameterableHeader(Constants.SUBSCRIPTION_STATE, notification.getSubscriptionStateHeader(), true);
				notify.addHeader(Constants.EVENT, notification.getEventPackage().getName());
				notify.setContent(b, type);
				notify.send();
			}
			else
			{
				//FIXME
			}
		} 
		catch (Throwable e) 
		{
			__log.error("Error while sending notifies",e);
		}
	}

	class Notification 
	{
		private SipSession _session;

		private EventPackage<?> _evtPackage;

		private int _expires;

		private Resource.Content _content;

		private Reason _reason;

		private Subscription.State _state;

		public Notification(
				EventPackage<?> evtPackage,
				SipSession session,
				int expires,
				Resource.Content content,
				Subscription.State state,
				Reason reason) 
		{
			_session = session;
			_evtPackage = evtPackage;
			_expires = expires;
			_content = content;
			_reason = reason;
			_state = state;
		}

		/**
		 * TODO: improve Subscription-State management
		 * @return
		 * @throws ServletParseException 
		 *
		public Parameterable getSubscriptionStateHeader() throws ServletParseException 
		{
			/*
			 * Very basic
			 *
			SipFactory factory = (SipFactory) getSession().getServletContext().getAttribute(SipServlet.SIP_FACTORY);

			Parameterable header = factory.createParameterable(_state.getName());

			if(_state.equals(Subscription.State.ACTIVE))
			{
				/*
   If the "Subscription-State" header value is "active", it means that
   the subscription has been accepted and (in general) has been
   authorized.  If the header also contains an "expires" parameter, the
   subscriber SHOULD take it as the authoritative subscription duration
   and adjust accordingly.  The "retry-after" and "reason" parameters
   have no semantics for "active".
				 
				header.setParameter(Constants.EXPIRES, new Integer(_expires).toString());

			}
			else if (_state.equals(Subscription.State.PENDING))
			{
				/*
   If the "Subscription-State" value is "pending", the subscription has
   been received by the notifier, but there is insufficient policy
   information to grant or deny the subscription yet.  If the header
   also contains an "expires" parameter, the subscriber SHOULD take it
   as the authoritative subscription duration and adjust accordingly.
   No further action is necessary on the part of the subscriber.  The
   "retry-after" and "reason" parameters have no semantics for
   "pending".
				 *
				header.setParameter(Constants.EXPIRES, new Integer(_expires).toString());
			}
			else if(_state.equals(Subscription.State.TERMINATED))
			{
				/*
If the "Subscription-State" value is "terminated", the subscriber
   should consider the subscription terminated.  The "expires" parameter
   has no semantics for "terminated".  If a reason code is present, the
   client should behave as described below.  If no reason code or an
   unknown reason code is present, the client MAY attempt to re-
   subscribe at any time (unless a "retry-after" parameter is present,
   in which case the client SHOULD NOT attempt re-subscription until
   after the number of seconds specified by the "retry-after"
   parameter).  The defined reason codes are:

   deactivated: The subscription has been terminated, but the subscriber
      SHOULD retry immediately with a new subscription.  One primary use
      of such a status code is to allow migration of subscriptions
      between nodes.  The "retry-after" parameter has no semantics for
      "deactivated".

   probation: The subscription has been terminated, but the client
      SHOULD retry at some later time.  If a "retry-after" parameter is
      also present, the client SHOULD wait at least the number of
      seconds specified by that parameter before attempting to re-
      subscribe.

   rejected: The subscription has been terminated due to change in
      authorization policy.  Clients SHOULD NOT attempt to re-subscribe.
      The "retry-after" parameter has no semantics for "rejected".

   timeout: The subscription has been terminated because it was not
      refreshed before it expired.  Clients MAY re-subscribe
      immediately.  The "retry-after" parameter has no semantics for
      "timeout".

   giveup: The subscription has been terminated because the notifier
      could not obtain authorization in a timely fashion.  If a "retry-
      after" parameter is also present, the client SHOULD wait at least
      the number of seconds specified by that parameter before
      attempting to re-subscribe; otherwise, the client MAY retry
      immediately, but will likely get put back into pending state.

   noresource: The subscription has been terminated because the resource
      state which was being monitored no longer exists.  Clients SHOULD
      NOT attempt to re-subscribe.  The "retry-after" parameter has no
      semantics for "noresource".
				 *
				if(_reason != null)
					header.setParameter(Constants.REASON, _reason.getName());

			}
			return header;
		}

		public SipSession getSession() 
		{
			return _session;
		}

		public EventPackage<?> getEventPackage() 
		{
			return _evtPackage;
		}

		public Resource.Content getContent() 
		{
			return _content;
		}

		public int getExpires()
		{
			return _expires;
		}
	}*/
