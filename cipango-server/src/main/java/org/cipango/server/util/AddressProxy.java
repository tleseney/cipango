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
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.sip.Address;
import javax.servlet.sip.URI;

public class AddressProxy implements Address, Serializable
{
    static final long serialVersionUID = 3124500907098139606L;
	
	private Address _address;
	
	public AddressProxy(Address address)
	{
		_address = address;
	}
	
	public String getDisplayName()
	{
		return _address.getDisplayName();
	}

	public int getExpires()
	{
		return _address.getExpires();
	}

	public float getQ()
	{
		return _address.getQ();
	}

	public URI getURI()
	{
		return _address.getURI();
	}

	public boolean isWildcard()
	{
		return _address.isWildcard();
	}

	public void setDisplayName(String displayName)
	{
		_address.setDisplayName(displayName);
	}

	public void setExpires(int expires)
	{
		_address.setExpires(expires);
	}

	public void setQ(float q)
	{
		_address.setQ(q);
	}

	public void setURI(URI uri)
	{
		_address.setURI(uri);
	}

	public String getParameter(String name)
	{
		return _address.getParameter(name);
	}

	public Iterator<String> getParameterNames()
	{
		return _address.getParameterNames();
	}

	public Set<Entry<String, String>> getParameters()
	{
		return _address.getParameters();
	}

	public String getValue()
	{
		return _address.getValue();
	}

	public void removeParameter(String name)
	{
		_address.removeParameter(name);
	}

	public void setParameter(String name, String value)
	{
		_address.setParameter(name, value);
	}

	public void setValue(String value)
	{
		_address.setValue(value);
	}
	
	public Address clone()
	{
		return (Address) _address.clone();
	}
	
	public boolean equals(Object o)
	{
		return _address.equals(o);
	}
	
	public String toString()
	{
		return _address.toString();
	}
}
