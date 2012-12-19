// ========================================================================
// Copyright 2012 NEXCOM Systems
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
package org.cipango.sipunit;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.URI;

import org.cipango.client.Call;
import org.junit.Ignore;

@Ignore
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
