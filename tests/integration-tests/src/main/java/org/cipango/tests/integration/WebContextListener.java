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
package org.cipango.tests.integration;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class WebContextListener implements ServletContextListener
{

	public void contextInitialized(ServletContextEvent sce)
	{
		ServletContext sc = sce.getServletContext();
		if (sc.getAttribute(WebContextListener.class.getName()) != null)
			sc.setAttribute(WebContextListener.class.getName(), "Web context listener initialize twice");
		else
			sc.setAttribute(WebContextListener.class.getName(), "");
	}

	public void contextDestroyed(ServletContextEvent sce)
	{
	}
	
}
