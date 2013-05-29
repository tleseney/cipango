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

import org.cipango.diameter.base.Common;

/**
 * AVP type. It contains the AVP name, vendorId, code and AVP data format.
 *
 * @param <T> AVP data format 
 */
public class Type<T> 
{
	private int _vendorId;
	private int _code;
	private String _name;
	private DataFormat<T> _format;
	private boolean _mandatory;
	
	public Type(int vendorId, int code, String name, DataFormat<T> format)
	{
		_vendorId = vendorId;
		_code = code;
		_name = name;
		_format = format;
		_mandatory = true;
	}
	
	public int getVendorId()
	{
		return _vendorId;
	}
	
	public boolean isVendorSpecific()
	{
		return _vendorId != Common.IETF_VENDOR_ID;
	}
	
	public int getCode()
	{
		return _code;
	}
	
	public DataFormat<T> getDataFormat()
	{
		return _format;
	}
	
	@Override
	public int hashCode()
	{
		return _vendorId ^_code;
	}
	
	public String toString()
	{
		return _name + " (" + _vendorId + "/" + _code + ")";
	}

	public boolean isMandatory()
	{
		return _mandatory;
	}

	public Type<T> setMandatory(boolean mandatory)
	{
		_mandatory = mandatory;
		return this;
	}
}
