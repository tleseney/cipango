package org.cipango.client;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.URI;

/**
 * A SIP Call abstraction.
 */
public class Call extends Dialog
{

	public SipServletRequest createInitialInvite(URI local, URI remote)
	{
		return createInitialRequest(SipMethods.INVITE, local, remote);
	}
	
	public SipServletRequest createAck()
	{
		for (SipServletResponse response : getSessionHandler().getResponses())
		{
			if (response.getStatus() >= 200 && response.getStatus() < 300 && !response.isCommitted())
			{
				return response.createAck();
			}
		}
		return null;
	}
	
	public SipServletRequest createPrack()
	{
		return null; // TODO
	}
	
	public SipServletRequest createBye()
	{
		return createRequest(SipMethods.BYE);
	}
	
	public SipServletRequest createCancel()
	{
		if (_session != null)
		{
			SipServletResponse response = getSessionHandler().getLastResponse();
			if (response != null && response.getStatus() < SipServletResponse.SC_OK)
			{
				return response.getRequest().createCancel();
			}
		}
		return null;
	}
	
	/**
	 * Proceed to an unattended transfer towards <code>remote</code>.
	 * 
	 * @param remote
	 */
	public void transfer(URI remote)
	{
		// TODO: See RFC3515.
	}
}
