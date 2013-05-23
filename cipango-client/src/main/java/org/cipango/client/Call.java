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
				updateDialog(response);
				return response.createAck();
			}
		}
		return null;
	}
	
	public SipServletRequest createPrack()
	{
		throw new UnsupportedOperationException("Not implemented yet"); // TODO
	}
	
	public SipServletRequest createBye()
	{
		return createRequest(SipMethods.BYE);
	}
	
	/**
	 * Prepare a CANCEL request.
	 * 
	 * Note that it is possible to create a CANCEL is no response was received
	 * by the session. This is permitted because the session cannot know if a
	 * 100 response was received. As described in JSR 289 (11.1.9), it is the
	 * responsibility of the container to check that sending this request is
	 * appropriate and eventually to delay it.
	 */
	public SipServletRequest createCancel()
	{
		if (_session != null)
		{
			SipServletResponse response = getSessionHandler().getLastResponse();
			SipServletRequest request = (SipServletRequest) _session
					.getAttribute(INITIAL_REQUEST_ATTRIBUTE);
				return request.createCancel();
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
		throw new UnsupportedOperationException("Not implemented yet"); // TODO: See RFC3515.
	}
}
