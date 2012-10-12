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

import java.util.Iterator;
import javax.servlet.sip.SipURI;

public class SipURIProxy extends URIProxy implements SipURI
{
    static final long serialVersionUID = -8783776759537945045L;
    
    private SipURI _uri;
    
    public SipURIProxy(SipURI uri)
    {
    	super(uri);
        _uri = uri;
    }
    
    public String getUser()
    {
        return _uri.getUser();
    }

    public void setUser(String user)
    {
        _uri.setUser(user);
    }

    public String getUserPassword()
    {
       return _uri.getUserPassword();
    }

    public void setUserPassword(String passwd)
    {
       _uri.setUserPassword(passwd);
    }

    public String getHost()
    {
        return _uri.getHost();
    }

    public void setHost(String host)
    {
       _uri.setHost(host);
    }

    public int getPort()
    {
        return _uri.getPort();
    }

    public void setPort(int port)
    {
        _uri.setPort(port);
    }

    public boolean isSecure()
    {
        return _uri.isSecure();
    }

    public void setSecure(boolean secure)
    {
        _uri.setSecure(secure);
    }

    public String getTransportParam()
    {
        return _uri.getTransportParam();
    }

    public void setTransportParam(String param)
    {
        _uri.setTransportParam(param);
    }

    public String getMAddrParam()
    {
        return _uri.getMAddrParam();
    }

    public void setMAddrParam(String param)
    {
        _uri.setTransportParam(param);
    }

    public String getMethodParam()
    {
        return _uri.getMethodParam();
    }

    public void setMethodParam(String param)
    {
        _uri.setMethodParam(param);
    }

    public int getTTLParam()
    {
        return _uri.getTTLParam();
    }

    public void setTTLParam(int ttl)
    {
        _uri.setTTLParam(ttl);
    }

    public String getUserParam()
    {
        return _uri.getUserParam();
    }

    public void setUserParam(String param)
    {
       _uri.setUserParam(param);
    }

    public boolean getLrParam()
    {
        return _uri.getLrParam();
    }

    public void setLrParam(boolean lr)
    {
        _uri.setLrParam(lr);
    }

    public String getHeader(String name)
    {
        return _uri.getHeader(name);
    }

    public void setHeader(String name, String value)
    {
        _uri.setHeader(name, value);
    }

    public Iterator<String> getHeaderNames()
    {
        return _uri.getHeaderNames();
    }

	public void removeHeader(String name)
	{
		_uri.removeHeader(name);
	}
}
