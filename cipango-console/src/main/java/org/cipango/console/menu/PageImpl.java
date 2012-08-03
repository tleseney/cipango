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
package org.cipango.console.menu;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;


public class PageImpl implements Page
{
	private List<PageImpl> _pages = new ArrayList<PageImpl>();
	private PageImpl _father;
	private String _name;
	private String _title;
	private String _menuTitle;
	private ObjectName _objectName;
	
	public PageImpl(String title)
	{
		_title = title;
	}
	
	public PageImpl(String name, String title)
	{
		_name = name;
		_title = title;
	}
	
	public PageImpl(String name, String title, String menuTitle)
	{
		_name = name;
		_title = title;
		_menuTitle = menuTitle;
	}
	
	public List<PageImpl> getPages()
	{
		return _pages;
	}

	public String getName()
	{
		return _name;
	}
	
	public String getTitle()
	{
		if (_title != null)
			return _title;
		return _name.substring(0, 1).toUpperCase() + _name.substring(1);
	}
	
	public String getMenuTitle()
	{
		if (_menuTitle != null)
			return _menuTitle;
		return getTitle();
	}
	
	public PageImpl add(PageImpl page)
	{
		_pages.add(page);
		page.setFather(this);
		return page;
	}

	public PageImpl getFather()
	{
		return _father;
	}

	private void setFather(PageImpl father)
	{
		_father = father;
	}

	public boolean isDynamic()
	{
		return _objectName != null;
	}

	public void setObjectName(ObjectName objectName)
	{
		_objectName = objectName;
	}
	
	public ObjectName getObjectName()
	{
		return _objectName;
	}
	
	public boolean isEnabled(MBeanServerConnection c) throws IOException
	{
		return true;
	}
}
