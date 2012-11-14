// ========================================================================
// Copyright 2008-2012 NEXCOM Systems
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

package org.cipango.server.ar;

import java.util.Iterator;
import java.util.ServiceLoader;

import javax.servlet.sip.ar.SipApplicationRouter;
import javax.servlet.sip.ar.spi.SipApplicationRouterProvider;

import org.cipango.server.SipServer;
import org.eclipse.jetty.util.Loader;

/**
 * Locates and loads a {@link SipApplicationRouter} using javax.servlet.sip.ar.spi.SipApplicationRouterProvider 
 * system property or Java SPI if no property is defined (as defined in JSR289)
 */
public class ApplicationRouterLoader
{
	@SuppressWarnings("rawtypes")
	public static SipApplicationRouter loadApplicationRouter()
	{
		SipApplicationRouterProvider provider = null;
		
		String providerClass = System.getProperty(SipApplicationRouterProvider.class.getName());
		try
		{
			if (providerClass != null)
			{
				Class clazz = Loader.loadClass(SipServer.class, providerClass);
				provider = (SipApplicationRouterProvider) clazz.newInstance();
			}
		}
		catch (Exception e)
		{
			throw new IllegalStateException("Could not load SipApplicationRouterProvider class: " + providerClass, e);
		}
		
		if (provider == null)
		{
			 Iterator<SipApplicationRouterProvider> it = ServiceLoader.load(SipApplicationRouterProvider.class).iterator();
			 if (it.hasNext())
				 provider = it.next();
		}
		
		if (provider != null)
			return provider.getSipApplicationRouter();
		else
			throw new IllegalStateException("Could not find any application router provider");
	}
}
