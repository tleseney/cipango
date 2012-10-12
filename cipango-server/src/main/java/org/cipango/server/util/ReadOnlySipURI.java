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

import javax.servlet.sip.SipURI;

public class ReadOnlySipURI extends SipURIProxy
{
    static final long serialVersionUID = -7475677998794418076L;
    
    public ReadOnlySipURI(SipURI uri)
    {
        super(uri);
    }
        
    @Override
    public void setUser(String user) 
	{
    	throw new IllegalStateException("Read-only");
	}
    
    @Override
    public void setUserPassword(String passwd) 
	{
    	throw new IllegalStateException("Read-only");
	}
    
    @Override
    public void setHost(String host) 
	{
    	throw new IllegalStateException("Read-only");
	}

    @Override
    public void setPort(int port) 
	{
    	throw new IllegalStateException("Read-only");
	}

    @Override
    public void setSecure(boolean secure) 
	{
    	throw new IllegalStateException("Read-only");
	}

    @Override
    public void setParameter(String name, String value) 
	{
    	throw new IllegalStateException("Read-only");
	}

    @Override
    public void removeParameter(String name) 
	{
    	throw new IllegalStateException("Read-only");
	}

    @Override
    public void setTransportParam(String param) 
	{
    	throw new IllegalStateException("Read-only");
	}

    @Override
    public void setMAddrParam(String param) 
	{
    	throw new IllegalStateException("Read-only");
	}

    @Override
    public void setMethodParam(String param) 
	{
    	throw new IllegalStateException("Read-only");
	}

    @Override
    public void setTTLParam(int ttl) 
	{
    	throw new IllegalStateException("Read-only");
	}

    @Override
    public void setUserParam(String param) 
	{
    	throw new IllegalStateException("Read-only");
	}

    @Override
    public void setLrParam(boolean lr) 
	{
    	throw new IllegalStateException("Read-only");
	}

    @Override
    public void setHeader(String name, String value) 
	{
    	throw new IllegalStateException("Read-only");
	}

    @Override
    public void removeHeader(String name) 
	{
    	throw new IllegalStateException("Read-only");
	}
}
