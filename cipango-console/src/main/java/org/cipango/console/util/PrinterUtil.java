// ========================================================================
// Copyright 2010 NEXCOM Systems
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

import java.util.MissingResourceException;
import java.util.ResourceBundle;


public class PrinterUtil
{
	public static final String PARAMS_POSTFIX = ".params";
	public static final ResourceBundle PARAMETERS = ResourceBundle
			.getBundle("org.cipango.console.methods");
	public static final ResourceBundle DESCRIPTION = ResourceBundle
			.getBundle("org.cipango.console.description");
	
	
	public static String[] getParams(String name)
	{
		return getValueSplit(name + PARAMS_POSTFIX);
	}

	private static String[] getValueSplit(String name)
	{
		try
		{
			return PARAMETERS.getString(name).split("\\p{Space}*,\\p{Space}*");
		}
		catch (MissingResourceException e)
		{
			return null;
		}
	}

	public static String getTitle(String name)
	{
		try
		{
			return PARAMETERS.getString(name + ".title").trim();
		}
		catch (MissingResourceException e)
		{
			return null;
		}
	}

	public static String getNote(String propertyName, String param)
	{
		try
		{
			return DESCRIPTION.getString(propertyName + ".params." + param).trim();
		}
		catch (MissingResourceException e)
		{
			return null;
		}
	}
	
	public static String getDuration(long millis)
	{
		long seconds = millis/1000;
		long minutes = seconds /60;
		long hours = minutes /60;
		long days = hours/24;
		StringBuilder sb = new StringBuilder();
		if (days >= 1)
		{
			sb.append(days).append(" day");
			if (days >= 1)
				sb.append('s');
			sb.append(", ");
		}
		if (hours  >= 1)
		{
			sb.append(hours % 24).append(" hour");
			if ((hours % 24) > 1)
				sb.append('s');
			sb.append(", ");
		}
		if (minutes >= 1)
		{
			sb.append(minutes % 60).append(" minute");
			if ((minutes % 60) > 1)
				sb.append('s');
			sb.append(", ");
		}
		sb.append(seconds % 60).append(" second");
		if ((seconds % 60) > 1)
			sb.append('s');
		return sb.toString();
	}
}
