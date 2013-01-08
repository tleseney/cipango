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
package org.cipango.dns;

import junit.framework.Assert;

import org.cipango.dns.util.Inet6Util;
import org.junit.Test;

public class InetUtilTest
{
	private static final String[] VALID_IPv4 = 
		{ "192.168.1.1", "1.0.0.1", "255.255.255.255" };
	
	private static final String[] INVALID_IPv4 = 
		{ "555.168.1.1", "255-255-255-255", "a.b.c.d" };

	private static final String[] VALID_IPv6 = 
		{ "[::1]", "::1", "2001:0db8:0000:85a3:0000:0000:ac1f:8001", 
		"2001:db8:0:85a3::ac1f:8001", "2001:db8:aaaa:bbbb:cccc:dddd:eeee:AaAa", 
		"::FFFF:129.144.52.38", "::192.9.5.5" };
	
	private static final String[] INVALID_IPv6 = 
		{ "[::1", "::1]", "a.b.c.d", "001:0db8::85a3::0000:ac1f:8001" };

	
	@Test
	public void testIsNumericalIpv4Address()
	{
		for (String ip : VALID_IPv4)
			Assert.assertTrue("Expect valid IP address: " + ip, Inet6Util.isValidIPV4Address(ip));
		
		for (String ip : INVALID_IPv4)
			Assert.assertFalse("Expect invalid IP address: " + ip, Inet6Util.isValidIPV4Address(ip));
	}
	
	@Test
	public void testIsNumericalIpv6Address()
	{
		for (String ip : VALID_IPv6)
			Assert.assertTrue("Expect valid IP address: " + ip, Inet6Util.isValidIP6Address(ip));
		
		for (String ip : INVALID_IPv6)
			Assert.assertFalse("Expect invalid IP address: " + ip, Inet6Util.isValidIP6Address(ip));
	}
}
