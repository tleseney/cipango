package org.cipango.sipunit;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.URI;

import org.cipango.client.Dialog;

public class TestDialog extends Dialog
{
	public TestDialog(Dialog dialog)
	{
		setFactory(dialog.getFactory());
		setCredentials(dialog.getCredentials()); 
		setOutboundProxy(dialog.getOutboundProxy());
		setTimeout(dialog.getTimeout());
	}

	@Override
	public SipServletRequest createInitialRequest(String method, URI local, URI remote)
	{
		return TestAgent.decorate(super.createInitialRequest(method, local, remote));
	}

	@Override
	public SipServletRequest createRequest(String method)
	{
		return TestAgent.decorate(super.createRequest(method));
	}
}
