// ========================================================================
// Copyright 2006-2013 NEXCOM Systems
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
package org.cipango.server.dns;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.cipango.server.Transport;

/**
 * DNS resolved based on java system DNS resolver.
 * Only A and AAAA requests are used for resolution.
 */
public class SystemDnsResolver implements DnsResolver
{

	/**
	 * Returns a list with one element resolved using A or AAAA request.
	 */
	@Override
	public List<Hop> getHops(Hop hop) throws IOException
	{
		if (hop.getTransport() == null)
			hop.setTransport(hop.isSecure() ? Transport.TLS : Transport.UDP);
		if (!hop.isPortSet())
			hop.setPort(hop.getTransport().getDefaultPort());
		hop.setAddress(InetAddress.getByName(hop.getHost()));
		return Collections.singletonList(hop);
	}

	@Override
	public void setEnableTransports(Collection<Transport> transports)
	{
	}

	@Override
	public Collection<Transport> getEnableTransports()
	{
		return null;
	}

}
