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
package org.cipango.diameter;

/**
 * Definition of command code.
 * <br/>
 * Each command Request/Answer pair is assigned a command code, and the sub-type (i.e., request or
 * answer) is identified via the 'R' bit in the Command Flags field of the Diameter header.
 * <p>
 * Every Diameter message MUST contain a command code in its header's Command-Code field, which is
 * used to determine the action that is to be taken for a particular message.
 * <p>
 * IETF command codes are defined in {@link org.cipango.diameter.base.Common}.
 * <br/>
 * 3GPP command codes are defined in {@link org.cipango.diameter.ims.Cx},
 * {@link org.cipango.diameter.ims.Sh}, {@link org.cipango.diameter.ims.Zh}.
 */
public class DiameterCommand
{
	private boolean _request;
	private int _code;
	private String _name;

	private boolean _proxiable;

	public DiameterCommand(boolean request, int code, String name, boolean proxiable)
	{
		_request = request;
		_code = code;
		_name = name;
		_proxiable = proxiable;
	}

	public DiameterCommand(boolean request, int code, String name)
	{
		this(request, code, name, true);
	}

	/**
	 * Returns <code>true</code> if this code applies for a request. Use
	 * 
	 * Used for the 'R' bit in the Command Flags.
	 * @return <code>true</code> if this code applies for a request.
	 */
	public boolean isRequest()
	{
		return _request;
	}

	/**
	 * Returns the command code.
	 * 
	 * @return the command code.
	 */
	public Integer getCode()
	{
		return _code;
	}

	/**
	 * Returns <code>true</code> if the message is proxiable.
	 * 
	 * Used for the 'P' bit in the Command Flags.
	 * 
	 * @return <code>true</code> if the message is proxiable.
	 */
	public boolean isProxiable()
	{
		return _proxiable;
	}
	
	

	public String toString()
	{
		return _name + "(" + _code + ")";
	}

	@Override
	public int hashCode()
	{
		if (isRequest())
			return _code;
		return -_code;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == this)
			return true;
		
		if (obj instanceof DiameterCommand)
			return hashCode() == obj.hashCode();
		return false;
	}
}
