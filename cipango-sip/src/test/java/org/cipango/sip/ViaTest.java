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

package org.cipango.sip;

import java.util.Map;

import org.eclipse.jetty.util.ajax.JSON;
import org.junit.Test;
import static org.junit.Assert.*;

public class ViaTest 
{
	public String vias[][]  = {
		{ "SIP/2.0/UDP pc33.atlanta.com;branch=z9hG4bK776asdhds", "UDP", "pc33.atlanta.com", "-1" , "{\"branch\":\"z9hG4bK776asdhds\"}" },
		{ "SIP/2.0/UDP pc33.atlanta.com", "UDP", "pc33.atlanta.com", "-1", null },
		{ "SIP/2.0/UDP 192.0.2.1:5060;received=192.0.2.207;branch=z9hG4bK77asjd" , "UDP", "192.0.2.1", "5060", "{\"branch\":\"z9hG4bK77asjd\", \"received\":\"192.0.2.207\"}" },
		{ "SIP/2.0/UDP 192.0.2.1:5060;received=192.0.2.207" , "UDP", "192.0.2.1", "5060", "{\"received\":\"192.0.2.207\"}" },
		{ "SIP  /   2.0   /   UDP 	 192.0.2.1:5060   ;     received   =   192.0.2.207 ; foo = \"bar\"" , "UDP", "192.0.2.1", "5060", "{\"received\":\"192.0.2.207\", \"foo\":\"bar\"}" },
		{ "SIP/2.0/UDP [2001:db8::9:1]:5060;branch=z9hG4bKas3-111", "UDP", "2001:db8::9:1", "5060", null }
	};
	
	@Test
	public void testParse() throws Exception
	{
		for (int i = 0; i < vias.length; i++)
		{
			Via via = new Via(vias[i][0]);
			via.parse();
			
			assertEquals(vias[i][1], via.getTransport());
			assertEquals(vias[i][2], via.getHost());
			assertEquals(Integer.parseInt(vias[i][3]), via.getPort());	
			
			String s = vias[i][4];
			if (s != null)
			{
				@SuppressWarnings("unchecked")
				Map<String, String> params = (Map<String, String>)JSON.parse(s);
				for (Map.Entry<String, String> p : params.entrySet())
				{
					assertEquals(p.getValue(), via.getParameter(p.getKey()));
				}
			}
		}
	}
}
