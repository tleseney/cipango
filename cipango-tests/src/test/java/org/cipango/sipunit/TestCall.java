package org.cipango.sipunit;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.URI;

import org.cipango.client.Call;

public class TestCall extends Call
{
	private TestAgent _agent;
	
	public TestCall(Call call)
	{
		setFactory(call.getFactory());
		setCredentials(call.getCredentials()); 
		setOutboundProxy(call.getOutboundProxy());
		setTimeout(call.getTimeout());
	}

	public TestAgent getAgent()
	{
		return _agent;
	}

	public void setAgent(TestAgent agent)
	{
		_agent = agent;
	}

	@Override
	public SipServletRequest createInitialRequest(String method, URI local, URI remote)
	{
		return _agent.decorate(super.createInitialRequest(method, local, remote));
	}

	@Override
	public SipServletRequest createAck()
	{
		return _agent.decorate(super.createAck());
	}

	@Override
	public SipServletRequest createCancel()
	{
		return _agent.decorate(super.createCancel());
	}

	@Override
	public SipServletRequest createRequest(String method)
	{
		return _agent.decorate(super.createRequest(method));
	}
}
