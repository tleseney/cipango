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
package org.cipango.console.data;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.cipango.console.util.ObjectNameFactory;
import org.cipango.console.util.PrinterUtil;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class PropertyList extends AbstractList<Property>
{
	private static Logger __logger = Log.getLogger(PropertyList.class);
	private List<Property> _properties = new ArrayList<Property>();
	public String _title;

	
	public PropertyList()
	{
	}

	public PropertyList(MBeanServerConnection connection, ObjectName objectName, String propertyName) throws Exception
	{
		setTitle(PrinterUtil.getTitle(propertyName));
		addProperties(connection, objectName, propertyName);
	}
	

			
	public PropertyList(MBeanServerConnection connection, String prefix) throws Exception
	{
		setTitle(PrinterUtil.getTitle(prefix));
		
		HashMap<String, ObjectName> objectNames = getObjectNames(prefix);
		Iterator<Entry<String, ObjectName>> it = objectNames.entrySet().iterator();
		while (it.hasNext())
		{
			Entry<String, ObjectName> entry = it.next();
			addProperties(connection, entry.getValue(), entry.getKey());		
		}
	}
	
	protected HashMap<String, ObjectName> getObjectNames(String prefix)
	{
		HashMap<String, ObjectName> objectNames = new HashMap<String, ObjectName>();
		Enumeration<String> enumeration = PrinterUtil.PARAMETERS.getKeys();
		while (enumeration.hasMoreElements())
		{
			String key = (String) enumeration.nextElement();
			if (key.startsWith(prefix)
					&& key.endsWith(PrinterUtil.PARAMS_POSTFIX))
			{
				String param = key.substring(0, key.lastIndexOf('.'));
				String className = param.substring(prefix.length() + 1).toLowerCase();
				Hashtable<String, String> table = new Hashtable<String, String>();
				table.put("type", className.substring(className.lastIndexOf('.') + 1));
				table.put("id", "0");
				ObjectName objectName = ObjectNameFactory.create(
						className.substring(0, className.lastIndexOf('.')), table);
				objectNames.put(param, objectName);
			}
		}
		return objectNames;
	}
	
	public void addProperties(MBeanServerConnection connection, ObjectName objectName, String propertyName) throws Exception
	{
		String[] params = PrinterUtil.getParams(propertyName);
		if (!connection.isRegistered(objectName))
		{
			StringBuilder sb = new StringBuilder();
			sb.append("Could not get values for parameters: ");
			for (int i = 0; i < params.length; i++)
			{
				sb.append(params[i]);
				if (i + 1 < params.length)
					sb.append(", ");
			}
			sb.append(" as there are not registered in JMX");
			__logger.warn(sb.toString());
		}
		else
		{
			MBeanInfo info = connection.getMBeanInfo(objectName);
			
			MBeanAttributeInfo[] attrInfo = info.getAttributes();
			for (int j = 0; j < params.length; j++)
			{
				if (params[j] == null ||  "".equals(params[j].trim()))
					continue;
				
				int k;
				for (k = 0; k < attrInfo.length; k++)
				{
					if (attrInfo[k].getName().equals(params[j]))
						break;
				}
				if (k >= attrInfo.length)
					__logger.warn("Could not found attribute: {} in {}", params[j], objectName);
				else
				{
					String note = PrinterUtil.getNote(propertyName, params[j]);
					Property property = new Property(attrInfo[k], connection.getAttribute(objectName, params[j]), note);
					add(property);
				}
			}
		}
	}
	
	public String getTitle()
	{
		return _title;
	}
	public void setTitle(String title)
	{
		_title = title;
	}
	public boolean hasNotes()
	{
		Iterator<Property> it = iterator();
		while (it.hasNext())
		{
			if (it.next().hasNote())
				return true;
		}
		return false;
	}

	public List<Property> getProperties()
	{
		return _properties;
	}
	
	@Override
	public void add(int index, Property property)
	{
		_properties.add(index, property);
	}
	
	@Override
	public Property get(int index)
	{
		return _properties.get(index);
	}
	
	@Override
	public int size()
	{
		return _properties.size();
	}
	
}
