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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.sip.Address;
import javax.servlet.sip.B2buaHelper;
import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.TooManyHopsException;
import javax.servlet.sip.UAMode;
import javax.servlet.sip.ar.SipApplicationRoutingDirective;

import org.cipango.server.session.ApplicationSession;
import org.cipango.server.session.Session;
import org.cipango.server.session.SessionManager;
import org.cipango.server.transaction.ClientTransaction;
import org.cipango.server.transaction.ServerTransaction;
import org.cipango.server.transaction.Transaction.State;
import org.cipango.server.util.ContactAddress;
import org.cipango.sip.AddressImpl;
import org.cipango.sip.SipFields.Field;
import org.cipango.sip.SipHeader;

public class B2bHelper implements B2buaHelper
{
	private static final B2bHelper __instance = new B2bHelper();

	public static B2bHelper getInstance()
	{
		return __instance;
	}

	@Override
	public SipServletRequest createCancel(SipSession sipSession)
	{
		if (sipSession == null)
			throw new NullPointerException("SipSession is null");
		
		Session session = ((SessionManager.SipSessionIf) sipSession).getSession();
		for (ClientTransaction tx : session.getClientTransactions())
		{
			if (tx.getRequest().isInitial())
				return tx.getRequest().createCancel();
		}
		
		// Case fork
		Iterator<Session> it = findCloneSessions(session, true).iterator();
		while (it.hasNext())
		{
			Session session2 = (Session) it.next();
			for (ClientTransaction tx : session2.getClientTransactions())
			{
				if (tx.getRequest().isInitial())
					return tx.getRequest().createCancel();
			}
		}
		
		return null;
	}

	@Override
	public SipServletRequest createRequest(SipServletRequest origRequest)
	{
		SipRequest srcRequest = (SipRequest) origRequest;
		ApplicationSession appSession = srcRequest.appSession();

		AddressImpl local = (AddressImpl) srcRequest.from().clone();
		local.setParameter(AddressImpl.TAG, appSession.newUASTag());

		AddressImpl remote = (AddressImpl) srcRequest.to().clone();
		remote.removeParameter(AddressImpl.TAG);

		String callId = appSession.getSessionManager().newCallId();

		Session session = appSession.createUacSession(callId, local, remote);
		session.setHandler(appSession.getContext().getServletHandler()
				.getDefaultServlet());

		SipRequest request = session.getUa().createRequest(srcRequest);
		request.setRoutingDirective(SipApplicationRoutingDirective.CONTINUE,
				srcRequest);

		return request;
	}

	@Override
	public SipServletRequest createRequest(SipServletRequest origRequest,
			boolean linked, Map<String, List<String>> headerMap)
			throws IllegalArgumentException, TooManyHopsException
	{
		if (origRequest == null)
			throw new NullPointerException("Original request is null");
		if (!origRequest.isInitial())
			throw new IllegalArgumentException(
					"Original request is not initial");

		int mf = origRequest.getMaxForwards();
		if (mf == 0)
			throw new TooManyHopsException(
					"Max-Forwards of original request is equal to 0");

		SipRequest request = (SipRequest) createRequest(origRequest);
		addHeaders(request, headerMap);

		if (linked)
			linkRequest((SipRequest) origRequest, request);

		return request;
	}

	@Override
	public SipServletRequest createRequest(SipSession sipSession,
			SipServletRequest origRequest, Map<String, List<String>> headerMap)
			throws IllegalArgumentException
	{
		if (sipSession == null)
			throw new NullPointerException("SipSession is null");
		if (!sipSession.getApplicationSession().equals(origRequest.getApplicationSession()))
			throw new IllegalArgumentException("SipSession " + sipSession 
					+ " does not belong to same application session as original request");

		SipSession linkedSession = getLinkedSession(origRequest.getSession());
		if (linkedSession != null && !linkedSession.equals(sipSession))
			throw new IllegalArgumentException("Original request is already linked to another sipSession");
		if (getLinkedSipServletRequest(origRequest) != null)
			throw new IllegalArgumentException("Original request is already linked to another request");
		
		Session session = ((SessionManager.SipSessionIf) sipSession).getSession();
		SipRequest srcRequest = (SipRequest) origRequest;
		
		SipRequest request = (SipRequest) session.getUa().createRequest(srcRequest);
		addHeaders(request, headerMap);
		
		linkRequest(srcRequest, request);
		
		return request;
	}

	@Override
	public SipServletResponse createResponseToOriginalRequest(SipSession sipSession,
			int status, String reason)
	{
		if (sipSession == null)
			throw new NullPointerException("SipSession is null");
		
		if (!sipSession.isValid())
			throw new IllegalArgumentException("SipSession " + sipSession + " is not valid");
		
		Session session = ((SessionManager.SipSessionIf) sipSession).getSession();
		Iterator<Session> it = findCloneSessions(session, false).iterator();
		while (it.hasNext())
		{
			Session session2 = (Session) it.next();
			
			for (ServerTransaction tx : session2.getServerTransactions())
			{
				SipRequest request = tx.getRequest();
				if (request.isInitial())
				{
					if (!session2.equals(session)
							|| (tx.getState() == State.ACCEPTED && session.getState() != SipSession.State.CONFIRMED))
					{
						if (status >= 300 && tx.getState() != State.PROCEEDING)
							throw new IllegalStateException("Cannot send response with status " + status 
									+ " since final response has already been sent");
						return new SipResponse(request, status, reason, session);
					}
					else
					{
						return request.createResponse(status, reason);
					}
				}
			}
		}
		return null;
	}

	@Override
	public SipSession getLinkedSession(SipSession sipSession)
	{
		if (sipSession == null)
			throw new NullPointerException("SipSession is null");
		
		if (!sipSession.isValid())
			throw new IllegalArgumentException("SipSession " + sipSession + " is not valid");
		return ((SessionManager.SipSessionIf) sipSession).getSession().getLinkedSession();
	}

	@Override
	public SipServletRequest getLinkedSipServletRequest(SipServletRequest request)
	{
		return ((SipRequest) request).getLinkedRequest();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public List<SipServletMessage> getPendingMessages(SipSession sipSession,
			UAMode mode)
	{
		if (sipSession == null)
			throw new NullPointerException("SipSession is null");
		
		if (!sipSession.isValid())
			throw new IllegalArgumentException("SipSession " + sipSession + " is not valid");
		
		Session session = ((SessionManager.SipSessionIf) sipSession).getSession();
		
		if (session.isProxy())
			throw new IllegalArgumentException("SipSession " + session + " is proxy");
		if (session.getUa() == null)
			session.createUA(mode);
		
		List<SipServletMessage> messages = new ArrayList<SipServletMessage>();
		
		if (mode == UAMode.UAS)
		{
			for (ServerTransaction tx : session.getServerTransactions())
			{
				if (!tx.getRequest().isCommitted())
					messages.add(tx.getRequest());
			}
		}
		else 
		{
			for (ClientTransaction tx : session.getClientTransactions())
			{
				if (!tx.isCompleted())
					messages.add(tx.getRequest());
			}
		}
		
		messages.addAll(session.getUa().getUncommitted2xx(mode));
		messages.addAll(session.getUa().getUncommitted1xx(mode));
		
		Collections.sort(messages, new Comparator() {
			public int compare(Object message1, Object message2)
			{
				long cseq1 = ((SipMessage) message1).getCSeq().getNumber();
				long cseq2 = ((SipMessage) message2).getCSeq().getNumber();
				
				return (int) (cseq1 - cseq2);
			}
		});
		return messages;
	}

	@Override
	public void linkSipSessions(SipSession sipSession1, SipSession sipSession2)
	{
		Session session1 = ((SessionManager.SipSessionIf) sipSession1).getSession();
		Session session2 = ((SessionManager.SipSessionIf) sipSession2).getSession();
		checkNotTerminated(session1);
		checkNotTerminated(session2);	
		
		Session linked1 = session1.getLinkedSession();
		if (linked1 != null && !linked1.equals(session2))
			throw new IllegalArgumentException("SipSession " + sipSession1 + " is already linked to " + linked1);
		
		Session linked2 = session2.getLinkedSession();
		if (linked2 != null && !linked2.equals(session1))
			throw new IllegalArgumentException("SipSession " + sipSession2 + " is already linked to " + linked2);
		
		session1.setLinkedSession(session2);
		session2.setLinkedSession(session1);
	}

	@Override
	public void unlinkSipSessions(SipSession sipSession)
	{
		if (sipSession == null)
			throw new NullPointerException("SipSession is null");
		
		Session session = ((SessionManager.SipSessionIf) sipSession).getSession();
		checkNotTerminated(session);
		
		Session linked = session.getLinkedSession();
		if (linked == null)
			throw new IllegalArgumentException("SipSession " + session + " has no linked SipSession");

		linked.setLinkedSession(null);
		session.setLinkedSession(null);
	}

	protected void addHeaders(SipRequest request,
			Map<String, List<String>> headerMap)
	{
		if (headerMap != null)
		{
			for (Entry<String, List<String>> entry : headerMap.entrySet())
			{
				String name = SipHeader.getFormattedName(entry.getKey());
				SipHeader header = SipHeader.CACHE.get(name);

				if (header != null && header.isSystem())
				{
					if (header == SipHeader.FROM || header == SipHeader.TO)
					{
						List<String> l = entry.getValue();
						if (l.size() > 0)
						{
							try
							{
								Address address = new AddressImpl(l.get(0), true);
								mergeFromTo(address, request.getFields().getField(header).asAddress());
							}
							catch (ServletException e)
							{
								throw new IllegalArgumentException("Invalid "
										+ header + " header ", e);
							}
							catch (ParseException e)
							{
								throw new IllegalArgumentException("Invalid address ", e);
							}
						}
					}
					else if (header == SipHeader.ROUTE && request.isInitial())
					{
						request.getFields().remove(SipHeader.ROUTE.asString());
						for (String route : entry.getValue())
						{
							request.getFields().addString(SipHeader.ROUTE.asString(), route, false);
						}
					}
					else
					{
						throw new IllegalArgumentException("Header " + name
								+ " is system.");
					}
				}
				else if (header != null && header == SipHeader.CONTACT)
				{
					List<String> contacts = entry.getValue();
					if (contacts.size() > 0)
					{
						try
						{
							Field contact = request.getFields().getField(header);
							if (contact != null)
							{
								try
								{
									Address dest = contact.asAddress();
									AddressImpl source = new AddressImpl(contacts.get(0), true);
									mergeContact(source, dest);
								}
								catch (ParseException e)
								{
									throw new IllegalArgumentException(
											"Invalid Contact header: "
													+ contacts.get(0));
								}
							}
							else if (request.isRegister())
								request.getFields().add(
										SipHeader.CONTACT.asString(),
										new AddressImpl(contacts.get(0)), false);
						}
						catch (ServletParseException e)
						{
							throw new IllegalArgumentException(
									"Invalid Contact header: "
											+ contacts.get(0));
						}
					}
				}
				else
				{
					request.getFields().remove(name);
					for (String value : entry.getValue())
					{
						request.getFields().addString(name, value, false);
					}
				}
			}
		}
	}
	
	private void checkNotTerminated(Session session)
	{
		if (session.isTerminated())
			throw new IllegalArgumentException("SipSession " + session + " is terminated");
	}
	
	private static List<Session> findCloneSessions(Session session, boolean uac)
	{
		Iterator<?> it = session.appSession().getSessions("sip");
		List<Session> l = new ArrayList<Session>();
		while (it.hasNext())
		{
			Session sipSession = (Session) it.next();
			boolean sameTag;
			if (uac)
				sameTag = sipSession.getLocalParty().equals(session.getLocalParty());
			else
				sameTag = sipSession.getRemoteParty().equals(session.getRemoteParty());
			
			if (sameTag && sipSession.getCallId().equals(session.getCallId()))
				l.add(sipSession);
			
		}
		return l;
	}

	private void linkRequest(SipRequest request1, SipRequest request2)
	{
		request1.setLinkedRequest(request2);
		request2.setLinkedRequest(request1);
		
		linkSipSessions(request1.session(), request2.session());
	}
	
	public static void mergeContact(Address src, Address dest) throws ServletParseException
	{
		SipURI srcUri = (SipURI) src.getURI();
		SipURI destUri = (SipURI) dest.getURI();
		
		String user = srcUri.getUser();
		if (user != null)
			destUri.setUser(user);
		
		Iterator<String> it = srcUri.getHeaderNames();
		while (it.hasNext())
		{
			String name = it.next();
			destUri.setHeader(name, srcUri.getHeader(name));
		}
		
		it = srcUri.getParameterNames();
		while (it.hasNext())
		{
			String name = it.next();
			if (!ContactAddress.isReservedUriParam(name))
				destUri.setParameter(name, srcUri.getParameter(name));
		}
		String displayName = src.getDisplayName();
		if (displayName != null)
			dest.setDisplayName(displayName);
		
		it = src.getParameterNames();
		while (it.hasNext())
		{
			String name = it.next();
			dest.setParameter(name, src.getParameter(name));
		}
	}
	
	private static void mergeFromTo(Address src, Address dest)
	{
		dest.setURI(src.getURI());
		dest.setDisplayName(src.getDisplayName());
		
		List<String> params = new ArrayList<String>();
		Iterator<String> it = src.getParameterNames();
		while (it.hasNext())
			params.add(it.next());

		it = dest.getParameterNames();
		while (it.hasNext())
			params.add(it.next());
		
		for (String param : params)
		{
			if (!param.equalsIgnoreCase(AddressImpl.TAG))
				dest.setParameter(param, src.getParameter(param));
		}
	}
}
