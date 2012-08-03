// ========================================================================
// Copyright 2011 NEXCOM Systems
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
package org.cipango.console.util;

import javax.servlet.http.HttpServletRequest;

public class ConsoleUtil
{

	public static String getParamValue(String name, HttpServletRequest request)
	{
		String val = request.getParameter(name);
		if (val == null)
			return (String) request.getSession().getAttribute(name);
		
		request.getSession().setAttribute(name, val);
		return  val;
	}
	
	public static int getParamValueAsInt(String name, HttpServletRequest request, int defaultVal)
	{
		String val = request.getParameter(name);
		if (val == null)
		{
			Integer intVal =  (Integer) request.getSession().getAttribute(name);
			return (intVal == null) ? defaultVal : intVal;
		}
		
		try
		{
			int intVal = Integer.parseInt(val);
			request.getSession().setAttribute(name, intVal);
			return intVal;
		}
		catch (Exception e) 
		{
			return defaultVal;
		}
	}
}
