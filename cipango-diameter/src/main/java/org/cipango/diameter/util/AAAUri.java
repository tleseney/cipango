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
package org.cipango.diameter.util;


public class AAAUri
{

    private String _fqdn;
    private int _port;
    
	
	public AAAUri(String fqdn, int port)
	{
		if (fqdn != null && fqdn.contains(":") && !fqdn.contains("["))
			_fqdn = "[" + fqdn + "]";
    	else
    		_fqdn = fqdn;
		_port = port;
	}
	
	public AAAUri(String fqdn, String sPort)
	{
		this(fqdn, Integer.parseInt(sPort));
	}
	
    public AAAUri(String uri)
    {
        if (!uri.startsWith("aaa://"))
            throw new IllegalArgumentException("not AAA URI");
        
        int iport = -1;
		
		if (uri.charAt(6) == '[')
		{
			int i = uri.indexOf(']', 6);
        	if (i < 0)
        		throw new IllegalArgumentException("Invalid IPv6 in " + uri);
        	iport = uri.indexOf(':', i);
		}
		else
			iport = uri.indexOf(':', 6);
        
        int iparams = uri.indexOf(';', 6);
        
        int efqdn = iport;
        
        if (efqdn < 0)
            efqdn = iparams;
        
        if (efqdn < 0)
            _fqdn = uri.substring(6);
        else 
            _fqdn = uri.substring(6, efqdn);
        
        if (iport < 0)
        {
            _port = -1;
        }
        else 
        {
            String sport;
            if (iparams < 0)
                sport = uri.substring(iport+1);
            else 
                sport = uri.substring(iport+1, iparams);
            
            try 
            {
                _port = Integer.parseInt(sport);
            }
            catch (NumberFormatException e)
            {
                throw new IllegalArgumentException("Invalid port: " + sport);
            }
        }
    }
    
    public String getFQDN()
    {
        return _fqdn;
    }
    
    public int getPort()
    {
        return _port;
    }
    
    @Override
    public String toString()
    {
    	StringBuilder sb = new StringBuilder();
    	sb.append("aaa://").append(_fqdn);
    	if (_port != -1)
    		sb.append(':').append(_port);
    	return sb.toString();
    }
    
    @Override
    public boolean equals(Object o)
    {
    	if (!(o instanceof AAAUri))
    		return false;
    	
    	AAAUri uri = (AAAUri) o;
    	return _fqdn.equals(uri.getFQDN()) && _port == uri.getPort();
    }
    
    public static void main(String[] args)
    {
    
        String s= "aaa://host.example.com:1234";
        AAAUri uri = new AAAUri(s);
        System.out.println(uri.getFQDN());
        System.out.println(uri.getPort());
    }
}
