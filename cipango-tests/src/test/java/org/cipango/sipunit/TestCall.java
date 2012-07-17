package org.cipango.sipunit;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.URI;

import org.cipango.client.Call;

public class TestCall extends Call
{
	public TestCall(Call call)
	{
		setFactory(call.getFactory());
		setCredentials(call.getCredentials()); 
		setOutboundProxy(call.getOutboundProxy());
		setTimeout(call.getTimeout());
	}

	@Override
	public SipServletRequest createInitialRequest(String method, URI local, URI remote)
	{
		return TestAgent.decorate(super.createInitialRequest(method, local, remote));
	}

	@Override
	public SipServletRequest createAck()
	{
		return TestAgent.decorate(super.createAck());
	}

	@Override
	public SipServletRequest createCancel()
	{
		return TestAgent.decorate(super.createCancel());
	}

	@Override
	public SipServletRequest createRequest(String method)
	{
		return TestAgent.decorate(super.createRequest(method));
	}
}
