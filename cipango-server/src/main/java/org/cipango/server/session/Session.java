package org.cipango.server.session;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
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

import org.cipango.server.SipRequest;
import org.cipango.server.SipServer;
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
	
	public Session(ApplicationSession applicationSession, String id, SipRequest request)
	{
		_created = System.currentTimeMillis();
		
		_applicationSession = applicationSession;
		_id = id;
		
		_callId = request.getCallId();
		_localParty = request.getFrom(); // TODO 
		_remoteParty = request.getTo(); // TODO
	}
	
	public void handleRequest(SipRequest request) throws ServletException, IOException
	{
		LOG.info("handling request");
		
		
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
	
	@Override
	public SipServletRequest createRequest(String arg0) {
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
	public Address getLocalParty() {
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

	@Override
	public void setHandler(String arg0) throws ServletException {
		// TODO Auto-generated method stub
		
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

	@Override 
	public String toString()
	{
		return String.format("%s{l(%s)<->r(%s),%s,%s}", getId(), _localParty, _remoteParty, _state, _role);
	}
}
