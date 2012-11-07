package org.cipango.server.session;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.sip.Address;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipSessionBindingEvent;
import javax.servlet.sip.SipSessionBindingListener;
import javax.servlet.sip.UAMode;
import javax.servlet.sip.URI;
import javax.servlet.sip.ar.SipApplicationRoutingRegion;

import org.cipango.server.SipConnection;
import org.cipango.server.SipConnector;
import org.cipango.server.SipMessage;
import org.cipango.server.SipRequest;
import org.cipango.server.SipResponse;
import org.cipango.server.SipServer;
import org.cipango.server.servlet.SipServletHolder;
import org.cipango.server.sipapp.SipAppContext;
import org.cipango.server.transaction.ClientTransaction;
import org.cipango.server.transaction.ClientTransactionListener;
import org.cipango.server.transaction.ServerTransaction;
import org.cipango.server.util.ReadOnlyAddress;
import org.cipango.sip.AddressImpl;
import org.cipango.sip.SipFields;
import org.cipango.sip.SipHeader;
import org.cipango.sip.SipMethod;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class Session implements SipSessionIf
{
	enum Role { UNDEFINED, UAC, UAS, PROXY }
	
	private static final Logger LOG = Log.getLogger(Session.class);
	
	private final String _id;
	private final ApplicationSession _applicationSession;

	protected State _state = State.INITIAL;
	protected Role _role = Role.UNDEFINED;
	
	protected String _callId;
	protected Address _localParty;
	protected Address _remoteParty;
	
	protected Map<String, Object> _attributes;
	
	private boolean _valid = true;
	private final long _created;
	private long _lastAccessed;
	
	private DialogInfo _dialog;
	private SipServletHolder _handler;
	
	private boolean _invalidateWhenReady = true;
	
	public Session(ApplicationSession applicationSession, String id, SipRequest request)
	{
		this(applicationSession, 
				id,
				request.getCallId(),
				request.to().clone(),
				(Address) request.getFrom().clone());
	}
	
	public Session(ApplicationSession applicationSession, String id, String callId, Address local, Address remote)
	{
		_applicationSession = applicationSession;
		_id = id;
		_created = System.currentTimeMillis();
		_callId = callId;
		_localParty = local;
		_remoteParty = remote;
	}
	
	public ApplicationSession appSession()
	{
		return _applicationSession;
	}
	
	public boolean isUA()
	{
		return _role == Role.UAS || _role == Role.UAC;
	}
	
	public boolean isProxy()
	{
		return _role == Role.PROXY;
	}
	
	protected SipServer getServer()
	{
		return _applicationSession.getSessionManager().getSipAppContext().getServer();
	}
	
	private void checkValid()
	{
		if (!_valid)
			throw new IllegalStateException("SipSession has been invalidated");
	}
	
	protected Object doPutOrRemove(String name, Object value)
	{
		if (value == null)
		{	
			return _attributes != null ? _attributes.remove(name) : null;
		}
		else
		{
			if (_attributes == null)
				_attributes = new HashMap<String, Object>();
			return _attributes.put(name, value);
		}
	}
	
	private void bindValue(String name, Object value)
	{
		if (value != null && value instanceof SipSessionBindingListener)
			((SipSessionBindingListener) value).valueBound(new SipSessionBindingEvent(this, name));
	}
	
	private void unbindValue(String name, Object value)
	{
		if (value != null && value instanceof SipSessionBindingListener)
			((SipSessionBindingListener) value).valueUnbound(new SipSessionBindingEvent(this, name));
	}
	
	public void setProxy()
	{
		if (isUA())
			throw new IllegalStateException("Session is " + _role);
		
		_role = Role.PROXY;
	}
	
	public void setUAS()
	{
		if (isUA())
			return;
		
		if (_role != Role.UNDEFINED)
			throw new IllegalStateException("session is " + _role);
		
		_role = Role.UAS;
		
		String tag = _localParty.getParameter(AddressImpl.TAG);
		if (tag == null)
		{
			tag = _applicationSession.newUASTag();
			_localParty.setParameter(AddressImpl.TAG, tag);
		}
		_dialog = new DialogInfo();
	}
	
	public void createUA(UAMode mode)
	{
		if (_role != Role.UNDEFINED)
			throw new IllegalStateException("Session is " + _role);
		
		_role = mode == UAMode.UAC ? Role.UAC : Role.UAS;
		_dialog = new DialogInfo();
	}
	
	public void sendResponse(SipResponse response)
	{		
		if (isUA())
		{
			ServerTransaction tx = (ServerTransaction) response.getTransaction();
			
			if (tx == null || tx.isCompleted())
				throw new IllegalStateException("no valid transaction");
				
			updateState(response, false);
			
			tx.send(response);
		}
		else
		{
			 //TODO virtual
		}
	}
	
	public ClientTransaction sendRequest(SipRequest request, ClientTransactionListener listener) throws IOException
	{
		accessed();
		
		SipServer server = getServer();
		//server.customizeRequest(request);
		
		request.setCommitted(true);
		
		return server.getTransactionManager().sendRequest(request, listener);
	}
		
	public ClientTransaction sendRequest(SipRequest request) throws IOException
	{
		if (!isUA())
			throw new IllegalStateException("Session is not UA");
		
		ClientTransaction tx = sendRequest(request, _dialog);

		return tx;
	}
	
	private void setState(State state)
	{
		_state = state;
	}
	
	
	public void invokeServlet(SipResponse response)
	{
		try
		{
			if (isValid())
				_applicationSession.getSessionManager().getSipAppContext().handle(response);
		}
		catch (Throwable t)
		{
			LOG.debug(t);
		}
	}
	
	public void accessed()
	{
		_lastAccessed = System.currentTimeMillis();
	}
		
	public void updateState(SipResponse response, boolean uac)
	{
		SipRequest request = (SipRequest) response.getRequest();
		int status = response.getStatus();
				
		if (request.isInitial() && (request.isInvite() || request.isSubscribe()))
		{
			switch (_state)
			{
			case INITIAL:
				if (status < 300)
				{
					// In UAS mode, the to tag has been set yet.
					if ((!uac && isUA()) || response.to().getTag() != null)
					{	
						if (_dialog != null)
							_dialog.createDialog(response, uac);
// FIXME				else if (isProxy())
//							createProxyDialog(response);
						
						if (status < 200)
							setState(State.EARLY);
						else
							setState(State.CONFIRMED);
					}
				}
				else
				{
					if (uac)
					{
						// FIXME _dialog.resetDialog();
						setState(State.INITIAL);
					}
					else
					{
						setState(State.TERMINATED);
					}
				}
				break;
			case EARLY:
				if (200 <= status && status < 300)
				{
					setState(State.CONFIRMED);
				}
				else if (status >= 300)
				{
					if (uac)
						setState(State.INITIAL);
					else
						setState(State.TERMINATED);
				}
				break;
			default:
				break;
			}
		}
		else if (request.isBye() && response.is2xx())
		{
			setState(State.TERMINATED);
		}
	}
	
	private void createDialog(SipResponse response)
	{
		switch (_role)
		{
			case UAS:
				
				
		}
	}
	
	public Address getContact(SipConnection connection)
	{
		URI uri = connection.getConnector().getURI();
		uri.setParameter("appid", _applicationSession.getId());
		return new AddressImpl(connection.getConnector().getURI());
	}
	
	public SipServletRequest createRequest(String method) 
	{
		checkValid();
		if (!isUA())
			throw new IllegalStateException("session is " + _role);

		return _dialog.createRequest(method);
	}
	
	public SipServletHolder getHandler()
	{
		return _handler;
	}
	
	public void setHandler(SipServletHolder handler)
	{
		_handler = handler;
	}
	

	/**
	 * @see SipSession#getApplicationSession()
	 */
	public SipApplicationSession getApplicationSession() 
	{
		return _applicationSession;
	}

	/**
	 * @see SipSession#getAttribute(String)
	 */
	public synchronized Object getAttribute(String name) 
	{
		checkValid();
		if (name == null)
			throw new NullPointerException("Attribute name is null");
		
		return _attributes != null ? _attributes.get(name) : null;
	}

	/**
	 * @see SipSession#getAttributeNames()
	 */
	public synchronized Enumeration<String> getAttributeNames() 
	{
		checkValid();
		if (_attributes == null)
			return Collections.emptyEnumeration();
		return Collections.enumeration(new ArrayList<String>(_attributes.keySet()));
	}

	@Override
	public String getCallId() 
	{
		return _callId;
	}

	/**
	 * @see SipSession#getCreationTime()
	 */
	public long getCreationTime() 
	{
		return _created;
	}

	/**
	 * @see SipSession#getId()
	 */
	public String getId() 
	{
		return _id;
	}

	@Override
	public boolean getInvalidateWhenReady() 
	{
		return _invalidateWhenReady;
	}

	/**
	 * @see SipSession#getLastAccessedTime()
	 */
	public long getLastAccessedTime() 
	{
		return _lastAccessed;
	}

	@Override
	public Address getLocalParty() 
	{
		return new ReadOnlyAddress(_localParty);
	}

	@Override
	public SipApplicationRoutingRegion getRegion() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Address getRemoteParty()
	{
		return new ReadOnlyAddress(_remoteParty);
	}
	
	public String getLocalTag()
	{
		return _localParty.getParameter(AddressImpl.TAG);
	}

	/**
	 * @see SipSession#getServletContext()
	 */
	public ServletContext getServletContext() 
	{
		return _applicationSession.getSessionManager().getContext();
	}

	/**
	 * @see SipSession#getState()
	 */
	public State getState() 
	{
		return _state;
	}

	@Override
	public URI getSubscriberURI() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void invalidate() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isReadyToInvalidate() {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * @see SipSession#isValid()
	 */
	public boolean isValid() 
	{
		return _valid;
	}

	/**
	 * @see SipSession#removeAttribute(String)
	 */
	public void removeAttribute(String name) 
	{
		setAttribute(name, null);
	}

	/**
	 * @see SipSession#setAttribute(String, Object)
	 */
	public void setAttribute(String name, Object value) 
	{
		Object old = null;
		synchronized (this)
		{
			checkValid();
			old = doPutOrRemove(name, value);
		}
		if (value == null || !value.equals(old))
		{
			if (old != null)
				unbindValue(name, value);
			if (value != null)
				bindValue(name, value);
			
			_applicationSession.getSessionManager().doSessionAttributeListeners(this, name, old, value);
		}
	}

	/**
	 * @see SipSession#setHandler(String)
	 */
	public void setHandler(String name) throws ServletException 
	{
		checkValid();
		
		SipAppContext context = _applicationSession.getContext();
		SipServletHolder handler = context.getSipServletHandler().getHolder(name);
		
		if (handler == null)
			throw new ServletException("No handler named " + name);
		
		setHandler(handler);
	}

	@Override
	public void setInvalidateWhenReady(boolean invalidateWhenReady) 
	{
		_invalidateWhenReady = invalidateWhenReady;
	}

	@Override
	public void setOutboundInterface(InetSocketAddress arg0) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void setOutboundInterface(InetAddress arg0) {
		// TODO Auto-generated method stub	
	}
	
	public DialogInfo getUa()
	{
		return _dialog;
	}
	
	public class DialogInfo implements ClientTransactionListener
	{
		protected long _localCSeq = 1;
		protected long _remoteCSeq = -1;
		protected URI _remoteTarget;
		protected LinkedList<String> _routeSet;
		protected boolean _secure = false;
		
		public SipServletRequest createRequest(String method)
		{
			SipMethod sipMethod = SipMethod.lookAheadGet(method.getBytes(), 0, method.length());
			if (sipMethod == SipMethod.ACK || sipMethod == SipMethod.CANCEL)
				throw new IllegalArgumentException("Forbidden request method " + method);
		
			if (_state == State.TERMINATED)
				throw new IllegalStateException("Cannot create request in TERMINATED state");
			else if (_state == State.INITIAL && _role == Role.UAS)
				throw new IllegalStateException("Cannot create request in INITIAL state and UAS mode");
			
			return createRequest(sipMethod, method, _localCSeq++);
		}
		
		public SipServletRequest createAck()
		{
			return createRequest(SipMethod.ACK, SipMethod.ACK.asString(), _localCSeq);
		}
		
		public SipServletRequest createRequest(SipMethod sipMethod, String method, long cseq)
		{
			SipRequest request = new SipRequest();
			request.setMethod(sipMethod, method);
			
			setDialogHeaders(request, cseq);
			
			request.setSession(Session.this);

			return request;
		}
		
		protected void setDialogHeaders(SipRequest request, long cseq)
		{
			SipFields fields = request.getFields();
			
			fields.set(SipHeader.FROM, _localParty);
			fields.set(SipHeader.TO, _remoteParty);
			
			if (_remoteTarget != null)
				request.setRequestURI((URI) _remoteTarget.clone());
			else if (request.getRequestURI() == null)
				request.setRequestURI(request.to().getURI());
			
			if (_routeSet != null)
			{
				fields.remove(SipHeader.ROUTE.asString());
				
				for (String route: _routeSet)
				{
					fields.add(SipHeader.ROUTE.asString(), route);
				}
			}
			fields.set(SipHeader.CALL_ID, _callId);
			fields.set(SipHeader.CSEQ, cseq + " " + request.getMethod());
			fields.set(SipHeader.MAX_FORWARDS, "70");
			
			if (request.needsContact())
				fields.set(SipHeader.CONTACT, getContact());
		}
		
		protected void createDialog(SipResponse response, boolean uac)
		{
			if (uac)
			{
				String tag = response.to().getTag();
                _remoteParty.setParameter(AddressImpl.TAG, tag);
                
                //System.out.println("Created dialog: " + tag);
                setRoute(response, true);
			}
			else
			{
				String tag = _applicationSession.newUASTag();
				_localParty.setParameter(AddressImpl.TAG, tag);
				                
                SipRequest request = (SipRequest) response.getRequest();
    			
				_remoteCSeq = request.getCSeq().getNumber();
				_secure = request.isSecure() && request.getRequestURI().getScheme().equals("sips");
				
				setRoute(request, false);
			}
		}
		
		protected void setRoute(SipMessage message, boolean reverse)
		{
			ListIterator<String> routes = message.getFields().getValues(SipHeader.RECORD_ROUTE.asString());
			_routeSet = new LinkedList<String>();
			while (routes.hasNext())
			{
				if (reverse)
					_routeSet.addFirst(routes.next());
				else
					_routeSet.addLast(routes.next());
			}
		}
		
		public Address getContact()
		{
			return getContact(getServer().getConnectors()[0]);
		}
		
		public Address getContact(SipConnector connector)
		{
			Address address = new AddressImpl((URI) connector.getURI().clone());
			//address.getURI().setParameter(ID.APP_SESSION_ID_PARAMETER, _appSession.getAppId());
			return address;
		}

		@Override
		public void handleResponse(SipResponse response)
		{
			if (response.getStatus() == 100)
				return;

// FIXME			
//			if (!isSameDialog(response))
//			{
//				Session derived = _appSession.getSession(response);
//				if (derived == null)
//				{
//					derived = _appSession.createDerivedSession(Session.this);
//					if (_linkedSessionId != null)
//					{
//						Session linkDerived = _appSession.createDerivedSession(getLinkedSession());
//						linkDerived.setLinkedSession(derived);
//						derived.setLinkedSession(linkDerived);
//					}
//				}
//				derived._ua.handleResponse(response);
//				return;
//			}
			
			response.setSession(Session.this);
			
			accessed();
			
//	FIXME		if (response.isInvite() && response.is2xx())
//			{
//				long cseq = response.getCSeq().getNumber();
//				ClientInvite invite = getClientInvite(cseq, true);
//				
//				if (invite._2xx != null || invite._ack != null)
//				{
//					if (invite._ack != null)
//					{
//						try
//						{
//							ClientTransaction tx = (ClientTransaction) invite._ack.getTransaction();
//							getServer().getConnectorManager().send(invite._ack, tx.getConnection());
//						}
//						catch (Exception e)
//						{
//							LOG.ignore(e);
//						}
//					}
//					return;
//				}
//				else
//				{
//					invite._2xx = response;
//				}
//			}
//			else if (response.isReliable1xx())
//			{
//				long rseq = response.getRSeq();
//				if (_remoteRSeq != -1 && (_remoteRSeq + 1 != rseq))
//				{
//					if (LOG.isDebugEnabled())
//						LOG.debug("Dropping 100rel with rseq {} since expecting {}", rseq, _remoteRSeq+1);
//					return;
//				}
//				_remoteRSeq = rseq;
//				long cseq = response.getCSeq().getNumber();
//				ClientInvite invite = getClientInvite(cseq, true);
//				invite.addReliable1xx(response);
//			}
//			else
//				response.setCommitted(true);
			
			updateState(response, true);
			
			if (isTargetRefresh(response))
				setRemoteTarget(response);
			
			if (isValid())
				invokeServlet(response);
			
		}
		
		private boolean isTargetRefresh(SipMessage message)
		{
			if (message instanceof SipResponse)
			{
				SipResponse response = (SipResponse) message;
				if (response.getStatus() >= 300)
					return false;
			}
			// NOTIFY is target refresh according to bug 699 in RFC 6665
			return message.isInvite() || message.isSubscribe() || message.isNotify() 
					|| message.isNotify() || message.isMethod(SipMethod.REFER);
		}
		
		protected void setRemoteTarget(SipMessage message) 
		{
			Address contact = (Address) message.getFields().get(SipHeader.CONTACT);
			if (contact != null)
				_remoteTarget = contact.getURI();
		}

		@Override
		public void customizeRequest(SipRequest request, SipConnection connection)
		{
			// TODO Auto-generated method stub
			
		}
	}
	
	@Override 
	public String toString()
	{
		return String.format("%s{l(%s)<->r(%s),%s,%s}", getId(), _localParty, _remoteParty, _state, _role);
	}
}
