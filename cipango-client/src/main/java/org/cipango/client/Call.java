package org.cipango.client;

import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.URI;

/**
 * A SIP Call abstraction.
 */
public class Call extends AbstractDialog
{

	@Override
	public SipServletRequest createInitialRequest(URI local, URI remote)
	{
		if (_session != null)
			return null;
		
		SipApplicationSession appSession = getFactory().createApplicationSession();
		return getFactory().createRequest(appSession, SipMethods.INVITE, local, remote);
	}
	
	public SipServletRequest createAck()
	{
		if (_session != null)
			return null;
		
		return getFactory().createRequest(_session.getApplicationSession(),
				SipMethods.ACK, _session.getLocalParty(),
				_session.getRemoteParty());
	}
	
	public SipServletRequest createBye()
	{
		if (_session != null)
			return null;
		
		return getFactory().createRequest(_session.getApplicationSession(),
				SipMethods.BYE, _session.getLocalParty(),
				_session.getRemoteParty());
	}
	
	public void cancel()
	{
		// TODO
	}
}
