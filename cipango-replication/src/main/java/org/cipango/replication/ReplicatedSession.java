// ========================================================================
// Copyright 2006-2013 NEXCOM Systems
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
package org.cipango.replication;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import javax.servlet.sip.Address;
import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipSessionActivationListener;
import javax.servlet.sip.SipSessionEvent;
import javax.servlet.sip.UAMode;
import javax.servlet.sip.URI;
import javax.servlet.sip.ar.SipApplicationRoutingRegion;

import org.cipango.replication.ReplicatedAppSession.ProxyAppSession;
import org.cipango.server.SipRequest;
import org.cipango.server.servlet.SipServletHandler;
import org.cipango.server.session.ApplicationSession;
import org.cipango.server.session.Session;
import org.cipango.server.session.SessionManager;
import org.cipango.server.sipapp.SipAppContext;
import org.cipango.sip.AddressImpl;
import org.cipango.sip.URIFactory;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class ReplicatedSession extends Session implements Serializable
{
	
	public static final String CREATED = "created";
	public static final String ATTRIBUTES = "attributes";
	public static final String ACCESSED = "accessed";
	public static final String INVALIDATE_WHEN_READY = "invalidateWhenReady";
	public static final String ROLE = "role";
	public static final String STATE = "state";
	public static final String LOCAL_PARTY = "localParty";
	public static final String REMOTE_PARTY = "remoteParty";
	public static final String REMOTE_TARGET = "remoteTarget";
	public static final String CALL_ID = "call-ID";
	public static final String LOCAL_CSEQ = "localCSeq";
	public static final String REMOTE_CSEQ = "remoteCSeq";
	public static final String ROUTE_SET = "routeSet";
	public static final String SECURE = "secure";
	public static final String RSEQ = "rseq";
	public static final String REMOTE_RSEQ = "remoteRSeq";
	public static final String LOCAL_RSEQ = "localRSeq";
	public static final String HANDLER_NAME = "handlerName";
	public static final String LINK_SESSION_ID = "linkSessionId";
	public static final String SUBSCRIBER_URI = "subscriberUri";
	public static final String REGION = "region";
	
	private final static Logger __log = Log.getLogger(ReplicatedSession.class);
	
	private byte[] _serializeAttributes;
	
	public ReplicatedSession(ApplicationSession applicationSession, String id, SipRequest request)
	{
		super(applicationSession, id, request);
	}
	
	public ReplicatedSession(ApplicationSession applicationSession, String id, String callId, Address local, Address remote)
	{
		super(applicationSession, id, callId, local, remote);
	}
	
	public ReplicatedSession(String id, Session other)
	{
		super(id, other);
	}
	
	public ReplicatedSession(ApplicationSession appSession, String id, Map<String, Object> data) throws ServletParseException, ParseException, ClassNotFoundException, IOException
	{
		super(appSession, 
				id,
				(String) data.get(CALL_ID),
				getAddress(data.get(LOCAL_PARTY)),
				getAddress(data.get(REMOTE_PARTY)),
				(Long) data.get(CREATED),
				(Long) data.get(ACCESSED));
		
		Role role = Role.valueOf((String) data.get(ROLE));		
		_state = State.valueOf(getValue(data, STATE, State.CONFIRMED.toString()));		
		
		if (role == Role.UAC || role == Role.UAS)
		{
			createUA(role == Role.UAC ? UAMode.UAC : UAMode.UAS);
			((ReplicatedDialogInfo) getUa()).putData(data);
		}
		else
			_role = role;
		
		_serializeAttributes = (byte[]) data.get(ATTRIBUTES);
		String handlerName = (String) data.get(HANDLER_NAME);
		SipServletHandler sipServletHandler = (SipServletHandler) appSession.getContext().getServletHandler();
		setHandler(sipServletHandler.getHolder(handlerName));
		setInvalidateWhenReady(getValue(data, INVALIDATE_WHEN_READY, false));
		_linkedSessionId = (String) data.get(LINK_SESSION_ID);
		_subscriberURI = getUri(data.get(SUBSCRIBER_URI));
		byte[] value = (byte[]) data.get(REGION);
		if (value != null)
			_region = (SipApplicationRoutingRegion) Serializer.deserialize(value);
		else
			_region = SipApplicationRoutingRegion.NEUTRAL_REGION;
	}
	
	public Map<String, Object> getData() throws IOException
	{	
		Map<String, Object> data = new HashMap<String, Object>();
		data.put(CREATED, getCreationTime());
		data.put(ACCESSED, getLastAccessedTime());
		data.put(ROLE, _role.toString());
		data.put(STATE, getState().toString());
		
		if (_localParty != null)
			data.put(LOCAL_PARTY, _localParty.toString());
		if (_remoteParty != null)
			data.put(REMOTE_PARTY, _remoteParty.toString());
		
		if (isUA())
			((ReplicatedDialogInfo) getUa()).getData(data);

		data.put(CALL_ID, getCallId());
		if( _attributes != null)
			data.put(ATTRIBUTES, Serializer.serialize(_attributes));
		data.put(HANDLER_NAME, getHandler().getName());
		
		if (getInvalidateWhenReady())
			data.put(INVALIDATE_WHEN_READY, getInvalidateWhenReady());
		if (_linkedSessionId != null)
			data.put(LINK_SESSION_ID, _linkedSessionId);
		if (_subscriberURI != null)
			data.put(SUBSCRIBER_URI, _subscriberURI.toString());
		if (_region != null && _region != SipApplicationRoutingRegion.NEUTRAL_REGION)
			data.put(REGION, Serializer.serialize(_region)); // SipApplicationRoutingRegion is serializable
		return data;
	}
	
	@Override
	protected DialogInfo newDialogInfo()
	{
		return new ReplicatedDialogInfo();
	}
	
	@SuppressWarnings("unchecked")
	private static <T> T getValue(Map<String, Object> data, String property, T defaultValue)
	{
		T value = (T) data.get(property);
		if (value == null)
			return defaultValue;
		return value;
	}
	
	private static Address getAddress(Object value) throws ServletParseException, ParseException
	{
		if (value == null)
			return null;
		return new AddressImpl((String) value, true);
	}
	
	private static URI getUri(Object value) throws ServletParseException, ParseException
	{
		if (value == null)
			return null;
		return URIFactory.parseURI((String) value);
	}
	
	public void notifyActivationListener(boolean activate)
	{
		if (_attributes == null)
			return;
		
		Iterator<Object> it = _attributes.values().iterator();
		while (it.hasNext())
		{
			Object value = (Object) it.next();
			if (value instanceof SipSessionActivationListener)
			{
				if (activate)
					((SipSessionActivationListener) value).sessionDidActivate(new SipSessionEvent(this));	
				else
					((SipSessionActivationListener) value).sessionWillPassivate(new SipSessionEvent(this));	
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	public void deserializeIfNeeded() throws ClassNotFoundException, IOException
	{
		if (_serializeAttributes != null)
		{
			_attributes = (Map<String, Object>) Serializer.deserialize(_serializeAttributes);
			_serializeAttributes = null;
		}
	}
	
	
	private Object writeReplace() throws ObjectStreamException
	{
		return new ProxySession(getApplicationSession().getId(), getId());
	}
	
	
	public static void main(String[] a) throws Exception
	{
		SipAppContext context = new SipAppContext();
		SessionManager sessionManager = context.getSessionHandler().getSessionManager();
		sessionManager.setSipAppContext(context);
		ReplicatedAppSession session = new ReplicatedAppSession(context.getSessionHandler().getSessionManager(), "123");
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(os);
		oos.writeObject(Serializer.serialize(session));
		System.out.println(new String(os.toByteArray()));
	}
		
	public class ReplicatedDialogInfo extends DialogInfo 
	{
		public void putData(Map<String, Object> data) throws ServletParseException, ParseException 
		{
			_remoteTarget = getUri(data.get(REMOTE_TARGET));
			_localCSeq = getValue(data, LOCAL_CSEQ, 1l);
			_remoteCSeq = getValue(data, REMOTE_CSEQ, -1l);
			String[] routeSet = (String[]) data.get(ROUTE_SET);
			if (routeSet != null)
			{
				_routeSet = new LinkedList<String>();
				for (int i = 0; i < routeSet.length; i++)
					_routeSet.add(routeSet[i]);
			}
			_secure = getValue(data, SECURE, false);
			
			_remoteRSeq = getValue(data, REMOTE_RSEQ, -1l);
			_localRSeq = getValue(data, LOCAL_RSEQ, 1l);
		}
		
		public void getData(Map<String, Object> data)
		{
			if (_remoteTarget != null)
				data.put(REMOTE_TARGET,_remoteTarget.toString());
			data.put(LOCAL_CSEQ, _localCSeq);
			data.put(REMOTE_CSEQ, _remoteCSeq);
			if (_routeSet != null && !_routeSet.isEmpty())
				data.put(ROUTE_SET, _routeSet.toArray(new String[] {}));
			if (_secure)
				data.put(SECURE, _secure);
			data.put(LOCAL_RSEQ, _localRSeq);
			data.put(REMOTE_RSEQ, _remoteRSeq);
		}
	}


	public static class ProxySession implements Serializable
	{
		private static final long serialVersionUID = 1L;
		
		private String _appSessionId; 
		private String _id; 
		
		public ProxySession()
		{
		}
		
		public ProxySession(String appSessionId, String id)
		{
			_appSessionId = appSessionId;
			_id = id;
		}
		
		private Object readResolve() throws ObjectStreamException 
		{
			SessionManager sessionManager = ProxyAppSession.getSessionManager();
			if (sessionManager == null)
			{
				__log.warn("Could not session manager in local thread");
				return null;
			}
			
			ApplicationSession applicationSession = sessionManager.getApplicationSession(_appSessionId);
			if (applicationSession == null)
			{
				__log.warn("Could not application session with ID " + _appSessionId);
				return null;
			}
			SipSession session = applicationSession.getSipSession(_id);
			if (session == null)
				__log.warn("Could not application session with ID " + _id);
			return session;
		}

		
	}

}
