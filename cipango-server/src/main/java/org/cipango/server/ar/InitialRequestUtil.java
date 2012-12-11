// ========================================================================
// Copyright 2008-2012 NEXCOM Systems
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

package org.cipango.server.ar;

import java.io.IOException;

import javax.servlet.sip.SipURI;

import org.cipango.server.SipRequest;
import org.cipango.sip.AddressImpl;

public class InitialRequestUtil 
{

	private static final String POPPED_ROUTE = "popped-route";
	private static final String REMOTE_ADDR = "remote-addr";
	private static final String REMOTE_PORT = "remote-port";
	private static final String TRANSPORT = "transport";
	
	public static void encode(SipURI uri, SipRequest request) throws IOException
	{
		setParameter(uri, POPPED_ROUTE, request.getInitialPoppedRoute());
		if (request.getInitialRemoteAddr() != null)
		{
			setParameter(uri, REMOTE_ADDR, request.getInitialRemoteAddr());
			setParameter(uri, REMOTE_PORT, request.getInitialRemotePort());
			setParameter(uri, TRANSPORT, request.getInitialTransport());
		}
	}
	
	private static void setParameter(SipURI uri, String name, Object value)
	{
		if (value != null)
			uri.setParameter(name, value.toString());
	}
	

	public static void decode(SipURI uri, SipRequest request) throws Exception
	{
		String route = uri.getParameter(POPPED_ROUTE);
		if (route != null)
			request.setInitialPoppedRoute(new AddressImpl(route, true));
		
		String remoteAddr = uri.getParameter(REMOTE_ADDR);
		if (remoteAddr != null)
		{
			request.setInitialRemoteAddr(remoteAddr);
			request.setInitialRemotePort(Integer.parseInt(uri.getParameter(REMOTE_PORT)));
			request.setInitialTransport(uri.getParameter(TRANSPORT));
		}
	}
}
