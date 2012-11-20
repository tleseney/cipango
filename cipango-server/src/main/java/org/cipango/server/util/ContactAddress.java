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

package org.cipango.server.util;

import javax.servlet.sip.Address;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.URI;

import org.cipango.sip.SipURIImpl;

public class ContactAddress extends AddressProxy
{
	static final long serialVersionUID = 1L;
	
	public ContactAddress(Address address)
	{
		super(address);
	}

	@Override
	public void setURI(URI uri)
	{
		throw new IllegalStateException("Read-only");
	}

	@Override
	public void setValue(String value)
	{
		throw new IllegalStateException("Read-only");
	}

	@Override
	public URI getURI()
	{
		return new ContactUri((SipURI) super.getURI());
	}
	
	public static boolean isReservedUriParam(String name)
    {
    	return name.equalsIgnoreCase(SipURIImpl.Param.METHOD.asString())
    		|| name.equalsIgnoreCase(SipURIImpl.Param.TTL.asString())
    		|| name.equalsIgnoreCase(SipURIImpl.Param.MADDR.asString())
    		|| name.equalsIgnoreCase(SipURIImpl.Param.LR.asString())
    		|| name.startsWith("org.cipango");
    }

	class ContactUri extends SipURIProxy
	{
		private static final long serialVersionUID = 1L;

		public ContactUri(SipURI uri)
		{
			super(uri);
		}

		@Override
		public void removeParameter(String name)
		{
			if (isReservedUriParam(name))
				throw new IllegalStateException("Read-only");
			super.removeParameter(name);
		}

		@Override
		public void setHost(String host)
		{
			throw new IllegalStateException("Read-only");
		}

		@Override
		public void setLrParam(boolean lr)
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
		public void setParameter(String name, String value)
		{
			if (isReservedUriParam(name))
				throw new IllegalStateException("Read-only");
			
			super.setParameter(name, value);
		}

		@Override
		public void setPort(int port)
		{
			throw new IllegalStateException("Read-only");
		}

		@Override
		public void setTTLParam(int ttl)
		{
			throw new IllegalStateException("Read-only");
		}

		@Override
		public void setSecure(boolean secure)
		{
			throw new IllegalStateException("Read-only");
		}
	}
}

