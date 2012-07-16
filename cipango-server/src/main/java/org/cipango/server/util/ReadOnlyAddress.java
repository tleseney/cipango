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

import javax.servlet.sip.Address;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.TelURL;
import javax.servlet.sip.URI;

public class ReadOnlyAddress extends AddressProxy
{
    static final long serialVersionUID = -3619796190467302288L;
    
	public ReadOnlyAddress(Address address)
	{
		super(address);
	}
		
	@Override
	public URI getURI() 
	{ 
		URI uri = super.getURI();
		if (uri instanceof SipURI)
			return new ReadOnlySipURI((SipURI) uri);
		else if (uri instanceof TelURL)
			return new ReadOnlyTelURL((TelURL) uri);
		else
			return new ReadOnlyURI(uri);
	} 
	
	@Override
	public void setDisplayName(String displayName) 
	{
		throw new IllegalStateException("Read-only");
	}

	@Override
	public void setExpires(int expires)  
	{
		throw new IllegalStateException("Read-only");
	}

	@Override
	public void setQ(float q)  
	{
		throw new IllegalStateException("Read-only");
	}

	@Override
	public void setURI(URI uri) 
	{
		throw new IllegalStateException("Read-only");
	}

	@Override
	public void removeParameter(String name) 
	{
		throw new IllegalStateException("Read-only");
	}

	@Override
	public void setParameter(String name, String value)  
	{
		throw new IllegalStateException("Read-only");
	}

	@Override
	public void setValue(String value) 
	{
		throw new IllegalStateException("Read-only");
	}	
}
