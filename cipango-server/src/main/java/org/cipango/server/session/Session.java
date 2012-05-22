package org.cipango.server.session;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Enumeration;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.sip.Address;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.URI;
import javax.servlet.sip.ar.SipApplicationRoutingRegion;

import org.cipango.server.SipRequest;
import org.cipango.server.SipServer;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class Session implements SipSessionIf
{
	private static final Logger LOG = Log.getLogger(Session.class);
	
	private String _id;
	private ApplicationSession _applicationSession;

	public Session(ApplicationSession applicationSession)
	{
		_applicationSession = applicationSession;
	}
	
	public void handleRequest(SipRequest request) throws ServletException, IOException
	{
		LOG.info("handling request");
		
		getServer().handle(request);
	}
	
	protected SipServer getServer()
	{
		return _applicationSession.getCallSession().getServer();
	}
	
	@Override
	public SipServletRequest createRequest(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SipApplicationSession getApplicationSession() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getAttribute(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getCallId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getCreationTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean getInvalidateWhenReady() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public long getLastAccessedTime() {
		// TODO Auto-generated method stub
		return 0;
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

	@Override
	public ServletContext getServletContext() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public State getState() {
		// TODO Auto-generated method stub
		return null;
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

	@Override
	public boolean isValid() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void removeAttribute(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setAttribute(String arg0, Object arg1) {
		// TODO Auto-generated method stub
		
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

}
