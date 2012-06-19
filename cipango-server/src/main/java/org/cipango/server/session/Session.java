package org.cipango.server.session;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.sip.Address;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipSessionBindingEvent;
import javax.servlet.sip.SipSessionBindingListener;
import javax.servlet.sip.URI;
import javax.servlet.sip.ar.SipApplicationRoutingRegion;

import org.cipango.server.SipConnection;
import org.cipango.server.SipRequest;
import org.cipango.server.SipResponse;
import org.cipango.server.SipServer;
import org.cipango.server.transaction.ServerTransaction;
import org.cipango.sip.AddressImpl;
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
	private String _handler;
	
	public Session(ApplicationSession applicationSession, String id, SipRequest request)
	{
		_created = System.currentTimeMillis();
		
		_applicationSession = applicationSession;
		_id = id;
		
		_callId = request.getCallId();
		_localParty = (Address) request.getTo().clone();
		_remoteParty = (Address) request.getFrom().clone();
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
		return null; //_applicationSession.getCallSession().getServer();
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
	}
	
	public void sendResponse(SipResponse response)
	{		
		if (isUA())
		{
			ServerTransaction tx = (ServerTransaction) response.getTransaction();
			
			if (tx == null || tx.isCompleted())
				throw new IllegalStateException("no valid transaction");
				
			updateState(response);
			
			tx.send(response);
		}
		else
		{
			 //TODO virtual
		}
	}
	
	private void setState(State state)
	{
		_state = state;
	}
	
	public void accessed()
	{
		_lastAccessed = System.currentTimeMillis();
	}
	
	private void updateState(SipResponse response)
	{
		SipRequest request = (SipRequest) response.getRequest();
		int status = response.getStatus();
		
		if (request.isInitial() && (request.isInvite() || request.isMethod(SipMethod.SUBSCRIBE)))
		{
			switch (_state)
			{
			case INITIAL:
				if (status < 300 && response.getToTag() != null)
				{
					switch (_role)
					{
						case UAS:
							_dialog = new DialogInfo();
							// cseq
							// secure
							// route 
							break;
						case UAC:
							// tag
							// route
							break;
						case PROXY:
							// tag
					}
						
					if (status < 200)
						setState(State.EARLY);
					else
						setState(State.CONFIRMED);
				}
				else 
				{
					if (_role == Role.UAC)
					{
						//resetDialog();
						setState(State.INITIAL);
					}
					else
						setState(State.TERMINATED);
				}
				break;
			case EARLY:
				if (200 <= status && status < 300)
					setState(State.CONFIRMED);
				else if (status >= 300)
				{
					if (_role == Role.UAC)
						setState(State.INITIAL); // TODO reset ?
					else
						setState(State.TERMINATED);
				}
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
		// TODO Auto-generated method stub
		return null;
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
	public String getCallId() {
		// TODO Auto-generated method stub
		return null;
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
	public boolean getInvalidateWhenReady() {
		// TODO Auto-generated method stub
		return false;
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SipApplicationRoutingRegion getRegion() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Address getRemoteParty() {
		// TODO Auto-generated method stub
		return null;
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
		
		_handler = name; // TODO check it exists
	}

	@Override
	public void setInvalidateWhenReady(boolean arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setOutboundInterface(InetSocketAddress arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setOutboundInterface(InetAddress arg0) {
		// TODO Auto-generated method stub
		
	}
	
	class DialogInfo
	{
		protected long _localCSeq = 1;
		protected long _remoteCSeq = -1;
		protected URI remoteTarget;
		protected LinkedList<String> _routeSet;
		protected boolean _secure = false;
	}
	
	@Override 
	public String toString()
	{
		return String.format("%s{l(%s)<->r(%s),%s,%s}", getId(), _localParty, _remoteParty, _state, _role);
	}
}
