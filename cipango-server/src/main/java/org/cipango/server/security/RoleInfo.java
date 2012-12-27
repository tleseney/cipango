// ========================================================================
// Copyright 2011-2012 NEXCOM Systems
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
package org.cipango.server.security;


public class RoleInfo extends org.eclipse.jetty.security.RoleInfo
{
	private boolean _proxyMode;
	

	public void combine(RoleInfo other)
    {
        super.combine(other);
        if (other.isProxyMode())
        	_proxyMode = true;
    }

	public boolean isProxyMode()
	{
		return _proxyMode;
	}

	public void setProxyMode(boolean proxyMode)
	{
		_proxyMode = proxyMode;
	}
	
    @Override
    public String toString()
    {
    	if (_proxyMode)
    	{
    		String s = super.toString();
    		return s.substring(0, s.length() - 1) + ",P}";
    	}
    	return super.toString();
    }
}