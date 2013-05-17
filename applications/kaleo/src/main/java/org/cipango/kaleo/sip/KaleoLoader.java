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

package org.cipango.kaleo.sip;

import java.io.File;
import java.util.Enumeration;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.cipango.kaleo.location.LocationService;
import org.cipango.kaleo.location.event.RegEventPackage;
import org.cipango.kaleo.presence.PresenceEventPackage;
import org.cipango.kaleo.presence.policy.PolicyManager;
import org.cipango.kaleo.presence.policy.XcapPolicyManager;
import org.cipango.kaleo.presence.watcherinfo.WatcherInfoEventPackage;
import org.cipango.kaleo.xcap.XcapService;
import org.cipango.kaleo.xcap.dao.FileXcapDao;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.MultiException;
import org.slf4j.Logger;

public class KaleoLoader implements ServletContextListener
{
	private Logger _log = org.slf4j.LoggerFactory.getLogger(KaleoLoader.class);
	private static final String XCAP_BASE_DIR_PROPERTY = "org.cipango.kaleo.xcap.base.dir";
	
	public void contextInitialized(ServletContextEvent event) 
	{
		try
		{
			PresenceEventPackage presence = new PresenceEventPackage();
			LocationService locationService = new LocationService();
			RegEventPackage regEventPackage = new RegEventPackage(locationService);
			WatcherInfoEventPackage watcherInfo = new WatcherInfoEventPackage(presence);
			XcapService xcapService = new XcapService();
			XcapPolicyManager policyManager = new XcapPolicyManager(xcapService);
			
			FileXcapDao xcapDao = new FileXcapDao();
			String baseDir = System.getProperty(XCAP_BASE_DIR_PROPERTY);
			if (baseDir != null)
				xcapDao.setBaseDir(new File(baseDir));
			else
				xcapDao.setBaseDir(new File(System.getProperty("jetty.home", ".") + "/data"));
			xcapService.setDao(xcapDao);
			presence.setPolicyManager(policyManager);
			
			locationService.start();
			presence.start();
			regEventPackage.start();
			watcherInfo.start();
			xcapService.start();
			
			ServletContext sc = event.getServletContext();
			sc.setAttribute(PresenceEventPackage.class.getName(), presence);
			sc.setAttribute(LocationService.class.getName(), locationService);
			sc.setAttribute(RegEventPackage.class.getName(), regEventPackage);
			sc.setAttribute(WatcherInfoEventPackage.class.getName(), watcherInfo);
			sc.setAttribute(XcapService.class.getName(), xcapService);
			sc.setAttribute(PolicyManager.class.getName(), policyManager);
		}
		catch (Exception e)
		{
			_log.error("failed to start Kaleo application", e);
		}
	}
	
	@SuppressWarnings("unchecked")
	public void contextDestroyed(ServletContextEvent event)
	{	
		Enumeration enumeration = event.getServletContext().getAttributeNames();
		MultiException mex = new MultiException();
		
		while (enumeration.hasMoreElements())
		{
			String key = (String) enumeration.nextElement();
			Object o = event.getServletContext().getAttribute(key);
			
			try
			{
				if (o instanceof LifeCycle)
					((LifeCycle) o).stop();
			}
			catch (Exception e)
			{
				mex.add(e);
			}
		}
		try
		{
			mex.ifExceptionThrow();
		}
		catch (Exception e)
		{
			_log.warn("error while stopping kaleo", mex);
		}
	}
}
