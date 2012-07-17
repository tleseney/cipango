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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.sip.Address;
import javax.servlet.sip.Proxy;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.URI;
import javax.servlet.sip.annotation.SipServlet;

import org.cipango.test.common.SipServletTestCase;

@SipServlet (name="org.cipango.test.B2bHelperProxyServlet")
public class B2bHelperProxyServlet  extends SipServletTestCase
{

	public static final String PROXY_URIS = "proxy-uris";
	
	@Override
	protected void doRequest(SipServletRequest request) throws ServletException, IOException
	{
		Iterator<Address> it = request.getAddressHeaders(PROXY_URIS);
		List<URI> l = new ArrayList<URI>();
		while (it.hasNext())
			l.add(it.next().getURI());
		Proxy proxy = request.getProxy();
		proxy.setRecordRoute(false);
		proxy.setParallel(true);
		proxy.proxyTo(l);
	}

}
