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
package org.cipango.tests.integration;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.servlet.sip.ServletTimer;
import javax.servlet.sip.SipServletContextEvent;
import javax.servlet.sip.SipServletListener;
import javax.servlet.sip.TimerListener;
import javax.servlet.sip.annotation.SipListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SipListener
public class Listener implements SipServletListener, TimerListener
{
	private static final Logger __logger = LoggerFactory.getLogger(Listener.class);

	private static final List<String> __servlets = new ArrayList<String>();

	private static final int SERVLETS_COUNT = 10;

	private static Timer __timer = null;

	public static void checkLoadedServlets()
	{
		synchronized (__servlets)
		{
			if (__servlets.size() < SERVLETS_COUNT)
			{
				__logger.warn("Loaded only {} servlets out of {}. Did you activate annotations?",
						__servlets.size(), SERVLETS_COUNT);
				if (__logger.isDebugEnabled())
				{
					StringBuilder builder = new StringBuilder("Loaded:");
					for (String name: __servlets)
						builder.append(" ").append(name);
					__logger.debug(builder.toString());
				}
			}
		}
	}

	public void servletInitialized(SipServletContextEvent event)
	{
		synchronized (__servlets)
		{
			__servlets.add(event.getSipServlet().getServletName());
			if (__timer == null)
			{
				__timer = new Timer(); 
				__timer.schedule(new TimerTask() {
					@Override
					public void run()
					{
						checkLoadedServlets();
					}
				}, 2000);
			}
		}
	}
	
	public void timeout(ServletTimer timer)
	{
		try
		{
			Runnable r = (Runnable) timer.getInfo();
			r.run();
		}
		catch (Throwable e)
		{
			__logger.warn("Failed to handle timer " + timer.getInfo(), e);
		}
	}
}
