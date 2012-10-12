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
import java.util.Set;
import java.util.Map.Entry;

import javax.servlet.sip.Parameterable;

public class ParameterableProxy implements Parameterable, Serializable
{
    static final long serialVersionUID = -986063363952358747L;
    
	private Parameterable _parameterable;
	
	public ParameterableProxy(Parameterable parameterable)
	{
		_parameterable = parameterable;
	}

	public String getParameter(String name)
	{
		return _parameterable.getParameter(name);
	}

	public Iterator<String> getParameterNames()
	{
		return _parameterable.getParameterNames();
	}

	public Set<Entry<String, String>> getParameters()
	{
		return _parameterable.getParameters();
	}

	public String getValue()
	{
		return _parameterable.getValue();
	}

	public void removeParameter(String name)
	{
		_parameterable.removeParameter(name);
	}

	public void setParameter(String name, String value)
	{
		_parameterable.setParameter(name, value);
	}

	public void setValue(String value)
	{
		_parameterable.setValue(value);
	}

    public Object clone()
    {
        return _parameterable.clone();
    }
    
    public String toString()
    {
    	return _parameterable.toString();
    }
    
    public boolean equals(Object o)
    {
    	return _parameterable.equals(o);
    }
}
