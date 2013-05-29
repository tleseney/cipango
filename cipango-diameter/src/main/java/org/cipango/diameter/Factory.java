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

public class Factory 
{
	public static <T> Type<T> newType(String name, int vendorId, int code, DataFormat<T> dataFormat)
	{
		return new Type<T>(vendorId, code, name, dataFormat);
	}
	
	public static <T> Type<T> newType(String name, int code, DataFormat<T> dataFormat)
	{
		return newType(name, Common.IETF_VENDOR_ID, code, dataFormat);
	}
	
	public static DiameterCommand newCommand(boolean request, int code, String name, boolean proxiable)
	{
		return new DiameterCommand(request, code, name, proxiable);
	}
	
	public static DiameterCommand newRequest(int code, String name)
	{
		return new DiameterCommand(true, code, name);
	}
	
	public static DiameterCommand newAnswer(int code, String name)
	{
		return new DiameterCommand(false, code, name);
	}
	
	public static ResultCode newResultCode(int code, String name)
	{
		return new ResultCode(Common.IETF_VENDOR_ID, code, name);
	}
	
	public static ResultCode newResultCode(int vendorId, int code, String name)
	{
		return new ResultCode(vendorId, code, name);
	}
}
