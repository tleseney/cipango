// ========================================================================
// Copyright 2008-2009 NEXCOM Systems
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

package org.cipango.server.sipapp.rules.request;

import javax.servlet.sip.SipURI;
import javax.servlet.sip.URI;

public class Port implements Extractor 
{
	public Port(String token) 
	{
		if (!token.equals("uri")) 
			throw new IllegalArgumentException("Invalid expression: port after " + token);
	}
	
	public Object extract(Object input) 
	{
		URI uri = (URI) input;
		if (uri.isSipURI()) 
		{
	        SipURI sipuri = (SipURI) uri;
	        int port = sipuri.getPort();
	        if (port < 0) 
	        {
	            String scheme = sipuri.getScheme();
	            if (scheme.equals("sips")) 
	                return "5061"; 
	            else
	                return "5060";
	        }
	        else 
	        {
	            return Integer.toString(port);
	        }
	    } 
		else 
		{
	        return null;
	    }
	}
}
