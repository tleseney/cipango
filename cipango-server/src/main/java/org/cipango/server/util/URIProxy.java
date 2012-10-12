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

package org.cipango.server.util;

import java.io.Serializable;
import java.util.Iterator;

import javax.servlet.sip.URI;

public class URIProxy implements URI, Serializable
{
    static final long serialVersionUID = -8180882040956292252L;
    
	private URI _uri;
	
	public URIProxy(URI uri)
	{
		_uri = uri;
	}
	
	public String getParameter(String arg0)
	{
		return _uri.getParameter(arg0);
	}

	public Iterator<String> getParameterNames()
	{
		return _uri.getParameterNames();
	}

	public String getScheme()
	{
		return _uri.getScheme();
	}

	public boolean isSipURI()
	{
		return _uri.isSipURI();
	}

	public void removeParameter(String arg0)
	{
		_uri.removeParameter(arg0);
	}

	public void setParameter(String arg0, String arg1)
	{
		_uri.setParameter(arg0, arg1);
	}
	
    public javax.servlet.sip.URI clone()
    {
        return _uri.clone();
    }
    
    public String toString()
    {
    	return _uri.toString();
    }
    
    public boolean equals(Object o)
    {
    	return _uri.equals(o);
    }
}
