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

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import static org.junit.Assert.*;

public class ViaTest 
{
	public ViaData vias[]  = 
	{
		new ViaData("SIP/2.0/UDP pc33.atlanta.com;branch=z9hG4bK776asdhds", "UDP", "pc33.atlanta.com", -1 , new String[][] { {"branch", "z9hG4bK776asdhds"} }),
		new ViaData("SIP/2.0/UDP pc33.atlanta.com", "UDP", "pc33.atlanta.com", -1, null),
		new ViaData("SIP/2.0/UDP 192.0.2.1:5060;received=192.0.2.207;branch=z9hG4bK77asjd" , "UDP", "192.0.2.1", 5060,  new String[][]  { {"branch", "z9hG4bK77asjd" }, {"received" , "192.0.2.207"} }),
		new ViaData("SIP/2.0/UDP 192.0.2.1:5060;received=192.0.2.207" , "UDP", "192.0.2.1", 5060,   new String[][]  {{"received", "192.0.2.207"}}),
		new ViaData("SIP  /   2.0   /   UDP 	 192.0.2.1:5060   ;     received   =   192.0.2.207 ; foo = \"bar\"" , "UDP", "192.0.2.1", 5060,  new String[][]  {{"received", "192.0.2.207"}, {"foo", "bar"}}),
		new ViaData("SIP/2.0/UDP [2001:db8::9:1]:5060;branch=z9hG4bKas3-111", "UDP", "2001:db8::9:1", 5060, null)
	};
	
	@Test
	public void testParse() throws Exception
	{
		for (int i = 0; i < vias.length; i++)
		{
			Via via = new Via(vias[i]._string);
			via.parse();
			
			assertEquals(vias[i]._transport, via.getTransport());
			assertEquals(vias[i]._host, via.getHost());
			assertEquals(vias[i]._port, via.getPort());	
			
			for (Map.Entry<String, String> p : vias[i]._parameters.entrySet())
			{
				assertEquals(p.getValue(), via.getParameter(p.getKey()));
			}

		}
	}
	
	class ViaData
	{
		private String _string;
		private String _transport;
		private String _host;
		private int _port;
		private Map<String, String> _parameters;
		
		public ViaData(String string, String transport, String host, int port, String[][] parameters)
		{
			_string = string;
			_transport = transport;
			_host = host;
			_port = port;
			_parameters = new HashMap<String, String>();
			if (parameters != null)
				for (String[] p : parameters)
					_parameters.put(p[0], p[1]);
		}
	}
}
