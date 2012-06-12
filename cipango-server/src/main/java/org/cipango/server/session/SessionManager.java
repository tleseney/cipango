package org.cipango.server.session;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.servlet.ServletContext;
import javax.servlet.sip.SipSessionAttributeListener;
import javax.servlet.sip.SipSessionBindingEvent;
import javax.servlet.sip.SipSessionListener;

import org.cipango.util.StringUtil;
import org.eclipse.jetty.util.component.AbstractLifeCycle;

public class SessionManager extends AbstractLifeCycle
{
	private Random _random = new Random();
	private ConcurrentHashMap<String, ApplicationSession> _appSessions = new ConcurrentHashMap<String, ApplicationSession>();
	
	protected final List<SipSessionAttributeListener> _sessionAttributeListeners = new CopyOnWriteArrayList<SipSessionAttributeListener>();
	protected final List<SipSessionListener> _sessionListeners = new CopyOnWriteArrayList<SipSessionListener>();
	
	private ServletContext _context;
	
	@Override
	protected void doStart() throws Exception
	{
		super.doStart();
	}
	
	public ServletContext getContext()
	{
		return _context;
	}
	
	public ApplicationSession createApplicationSession()
	{
		ApplicationSession appSession = new ApplicationSession(newApplicationSessionId());
		_appSessions.put(appSession.getId(), appSession);
		return appSession;
	}
	
	public ApplicationSession getApplicationSession(String id)
	{
		return _appSessions.get(id);
	}
	
	protected String newApplicationSessionId()
	{
		long r = _random.nextInt();
		if (r<0)
			r = -r;
		return StringUtil.toBase62String2(r);
	}
	
	public void doSessionAttributeListeners(Session session, String name, Object old, Object value)
	{
		if (!_sessionAttributeListeners.isEmpty())
		{
			SipSessionBindingEvent event = new SipSessionBindingEvent(session, name);
			
			for (SipSessionAttributeListener l : _sessionAttributeListeners)
			{
				if (value == null)
					l.attributeRemoved(event);
				else if (old == null)
					l.attributeAdded(event);
				else
					l.attributeReplaced(event);
			}
		}
	}
}
