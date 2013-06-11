// ========================================================================
// Copyright 2010-2012 NEXCOM Systems
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
import java.util.Iterator;
import java.util.List;

import javax.management.MBeanServerConnection;

import org.cipango.console.ApplicationManager;
import org.cipango.console.DiameterManager;
import org.cipango.console.EnvManager;
import org.cipango.console.JettyManager;
import org.cipango.console.SipManager;
import org.cipango.console.SnmpManager;
import org.cipango.console.util.ObjectNameFactory;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class MenuImpl implements Menu
{

	private static final PageImpl PAGES = new PageImpl("");

	public static final PageImpl 
		SERVER = PAGES.add(new PageImpl("Server")),
		ABOUT = SERVER.add(new PageImpl("about.vm", "About")),
		SYSTEM_PROPERTIES = SERVER.add(new PageImpl("system-properties.vm", "System Properties")),
		
		CONFIG = PAGES.add(new PageImpl("Configuration")),
		CONFIG_SIP = CONFIG.add(new PageImpl("configuration/sip.vm", "SIP Configuration", "SIP")
		{
			@Override
			public boolean isEnabled(MBeanServerConnection c) throws IOException
			{
				return c.isRegistered(SipManager.SERVER);
			}
		}),
		CONFIG_HTTP = CONFIG.add(new PageImpl("configuration/http.vm", "HTTP Configuration", "HTTP")),
		CONFIG_DIAMETER = CONFIG.add(new PageImpl("configuration/diameter.vm", "Diameter Configuration", "Diameter")
		{
			@Override
			public boolean isEnabled(MBeanServerConnection c) throws IOException
			{
				return c.isRegistered(DiameterManager.NODE);
			}
		}),
		CONFIG_SNMP = CONFIG.add(new PageImpl("configuration/snmp.vm", "SNMP Configuration", "SNMP")
		{
			@Override
			public boolean isEnabled(MBeanServerConnection c) throws IOException
			{
				return c.isRegistered(SnmpManager.AGENT);
			}
		}),

		APPLICATIONS = PAGES.add(new PageImpl("Applications")
		{
			@Override
			public boolean isEnabled(MBeanServerConnection c) throws IOException
			{
				return !c.isRegistered(ObjectNameFactory.create("org.cipango.console:page-disabled=application"));
			}
		}),
		MAPPINGS = APPLICATIONS.add(new PageImpl("applications.vm", "Applications Mapping")
		{
			@Override
			public boolean isEnabled(MBeanServerConnection c) throws IOException
			{
				return getFather().isEnabled(c);
			}
		}),
		DAR = APPLICATIONS.add(new PageImpl("dar.vm", "Default Application Router", "DAR")
		{
			@Override
			public boolean isEnabled(MBeanServerConnection c) throws IOException
			{
				return getFather().isEnabled(c) && c.isRegistered(ApplicationManager.DAR);
			}
		}),
		
		STATISTICS = PAGES.add(new PageImpl("Statistics")),
		STATISTICS_SIP = STATISTICS.add(new PageImpl("statistics/sip.vm", "SIP Statistics", "SIP")),
		STATISTICS_HTTP = STATISTICS.add(new PageImpl("statistics/http.vm", "HTTP Statistics", "HTTP")
		{
			@Override
			public boolean isEnabled(MBeanServerConnection c) throws IOException
			{
				// In current version of Jetty 9, no stats a available.
				return false;
			}
		}),
		STATISTICS_DIAMETER = STATISTICS.add(new PageImpl("statistics/diameter.vm", "Diameter Statistics", "Diameter")
		{
			@Override
			public boolean isEnabled(MBeanServerConnection c) throws IOException
			{
				return c.isRegistered(DiameterManager.NODE);
			}
		}),
		STATISTICS_GRAPH = STATISTICS.add(new PageImpl("statistics/graph.vm", "Statistics graphs", "Graphs")),
		
		LOGS = PAGES.add(new PageImpl("Logs")),
		SIP_LOGS = LOGS.add(new PageImpl("logs/sip.vm", "SIP Logs", "SIP"){
			@Override
			public boolean isEnabled(MBeanServerConnection c) throws IOException
			{
				return c.isRegistered(SipManager.CONSOLE_LOGGER)
							|| c.isRegistered(SipManager.FILE_MESSAGE_LOG);
			}
		}),
		HTTP_LOGS = LOGS.add(new PageImpl("logs/http.vm", "HTTP Logs", "HTTP")
		{
			@Override
			public boolean isEnabled(MBeanServerConnection c) throws IOException
			{
				return c.isRegistered(JettyManager.HTTP_LOG);
			}
		}),
		DIAMETER_LOGS = LOGS.add(new PageImpl("logs/diameter.vm", "Diameter Logs", "Diameter")
		{
			@Override
			public boolean isEnabled(MBeanServerConnection c) throws IOException
			{
				return c.isRegistered(DiameterManager.NODE);
			}
		}),
		CALLS = LOGS.add(new PageImpl("logs/sessions.vm", "Sessions")),
		SYSTEM_LOGS = LOGS.add(new PageImpl("logs/systems.vm", "System logs")
		{
			@Override
			public boolean isEnabled(MBeanServerConnection c) throws IOException
			{
				return c.isRegistered(EnvManager.LOGBACK) ||  c.isRegistered(EnvManager.JETTY_LOGGER);
			}
		});
	
	
	protected MBeanServerConnection _connection;
	protected PageImpl _currentPage;
	protected List<PageImpl> _pages;
	private static Logger _logger = Log.getLogger(MenuImpl.class);

	public MenuImpl(MBeanServerConnection c, String command)
	{
		_connection = c;
		_pages = getPages();
		Iterator<PageImpl> it = _pages.iterator();
		while (it.hasNext())
		{
			PageImpl subPage = getPage(command, it.next());
			
			if (subPage != null)
			{
				_currentPage = subPage;
				break;
			}
		}
	}
	
	private PageImpl getPage(String command, PageImpl page)
	{
		Iterator<PageImpl> it = page.getPages().iterator();
		while (it.hasNext())
		{
			PageImpl subPage = getPage(command, it.next());
			if (subPage != null)
				return subPage;
		}

		if (command != null && command.equals(page.getName()))
			return page;
		
		return null;
	}
	
	
	public Page getCurrentPage()
	{
		return _currentPage;
	}
	
	public String getTitle()
	{
		return _currentPage == null ? "Cipango console" : _currentPage.getTitle();
	}
		
	public String getHtmlTitle()
	{
		if (_currentPage.getFather() == null)
			return "<h1>" + _currentPage.getTitle() + "</h1>\n";
		else
			return "<h1>" + _currentPage.getFather().getTitle() + 
					"<span> > " + _currentPage.getMenuTitle() + "</span></h1>\n";
	}
	
	public boolean isPageEnabled(PageImpl page) throws IOException
	{
		return page.isEnabled(_connection);
	}
	
	public boolean isCurrentPage(PageImpl page)
	{
		return _currentPage != null && (page == _currentPage || page == _currentPage.getFather());
	}
	
	public List<PageImpl> getPages()
	{
		return PAGES.getPages();
	}	
}
