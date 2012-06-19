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
import javax.servlet.sip.TelURL;
import javax.servlet.sip.URI;

public class Tel implements Extractor 
{	
	public Tel(String token) 
	{
		if (!token.equals("uri"))
			throw new IllegalArgumentException("Invalid expression: tel after " + token);
	}
	
	public Object extract(Object input) 
	{
		URI uri = (URI) input;
		if (uri.isSipURI()) 
		{
	        SipURI sipuri = (SipURI) uri;
	        if ("phone".equals(sipuri.getParameter("user"))) 
	            return stripVisuals(sipuri.getUser()); 
	    } 
		else if ("tel".equals(uri.getScheme())) 
		{
	        return stripVisuals(((TelURL) uri).getPhoneNumber());
	    }
	    return null;
	}

	private String stripVisuals(String s) 
	{
	    StringBuffer buf = new StringBuffer(s.length());
	    for (int i = 0; i < s.length(); i++) 
	    {
	        char c = s.charAt(i);
	        if ("-.()".indexOf(c) < 0) 
	            buf.append(c);
	    }
	    return buf.toString();
	}
}
