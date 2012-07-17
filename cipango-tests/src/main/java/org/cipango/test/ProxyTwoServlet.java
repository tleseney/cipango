// ========================================================================
// Copyright 2010 NEXCOM Systems
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
package org.cipango.test;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.sip.Proxy;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.URI;
import javax.servlet.sip.annotation.SipServlet;

import org.cipango.test.common.AbstractServlet;

@SipServlet (name="org.cipango.test.ProxyTwoServlet")
public class ProxyTwoServlet extends AbstractServlet
{

	public void testTwoProxy(SipServletRequest request) throws Throwable
	{
		Proxy proxy = request.getProxy();
		proxy.setRecordRoute(false);
		proxy.setSupervised(false);
		List<URI> l = new ArrayList<URI>();
		l.add(request.getRequestURI());
		l.add(request.getAddressHeader("proxy").getURI());
		proxy.proxyTo(l);
	}
	
	
	
}
