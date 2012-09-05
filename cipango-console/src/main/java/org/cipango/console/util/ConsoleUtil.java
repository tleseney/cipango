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

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

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
	
	public static Map<String, String> getFilters(ResourceBundle bundle)
	{
		Map<String, String> filters = new HashMap<String, String>();
		Enumeration<String> keys = bundle.getKeys();
		while (keys.hasMoreElements())
		{
			String key = keys.nextElement();
			if (key.endsWith(".title"))
			{
				String title = bundle.getString(key);
				String prefix = key.substring(0, key.length()
						- ".title".length());
				String filter = bundle.getString(prefix + ".filter").trim();
				filters.put(filter, title);
			}
		}
		return filters;
	}
}
