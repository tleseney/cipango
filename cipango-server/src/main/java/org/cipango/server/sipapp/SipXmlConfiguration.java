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

package org.cipango.server.sipapp;

import java.io.IOException;
import java.net.MalformedURLException;

import org.cipango.server.servlet.SipServletHandler;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.AbstractConfiguration;
import org.eclipse.jetty.webapp.WebAppContext;

public class SipXmlConfiguration extends AbstractConfiguration
{	
	private static final Logger LOG = Log.getLogger(SipXmlConfiguration.class);
	
	public void preConfigure(WebAppContext context) throws Exception
	{
		SipAppContext sac = context.getBean(SipAppContext.class);
		if (sac == null)
		{
			sac = new SipAppContext();
			sac.setWebAppContext(context);
			context.addBean(sac);
		}
		else
			sac.setWebAppContext(context);
		
				
		String defaultsSipDescriptor = sac.getDefaultsDescriptor();
		if (defaultsSipDescriptor != null && defaultsSipDescriptor.length() > 0)
		{
			Resource dftSipResource = Resource.newSystemResource(defaultsSipDescriptor);
			if (dftSipResource == null)
				dftSipResource = context.newResource(defaultsSipDescriptor);
			sac.getMetaData().setDefaults(dftSipResource);
		}
		Resource sipXml = findSipXml(context);
		if (sipXml != null)
			sac.getMetaData().setSipXml(sipXml);
		
		//parse but don't process override-sip.xml
        for (String overrideDescriptor : sac.getOverrideDescriptors())
        {
            if (overrideDescriptor != null && overrideDescriptor.length() > 0)
            {
                Resource orideResource = Resource.newSystemResource(overrideDescriptor);
                if (orideResource == null) 
                    orideResource = context.newResource(overrideDescriptor);
                sac.getMetaData().addOverride(orideResource);
            }
        }
	}
	
	public void configure(WebAppContext context) throws Exception
	{
		SipAppContext sac = context.getBean(SipAppContext.class);

		sac.getMetaData().addDescriptorProcessor(new StandardDescriptorProcessor());
	}
	
	public void deconfigure(WebAppContext context) throws Exception 
	{
		// TODO preserve any configuration that pre-existed.
		SipAppContext sac = context.getBean(SipAppContext.class);
        SipServletHandler servletHandler = sac.getServletHandler();
       
        servletHandler.setServlets(null);
        servletHandler.setServletMappings(null);

        context.setEventListeners(null);

        // TODO remove classpaths from classloader
	}

	
	protected Resource findSipXml(WebAppContext context) throws IOException, MalformedURLException 
	{
		// TODO sip descriptor
		Resource webInf = context.getWebInf();
		if (webInf != null && webInf.isDirectory()) 
		{
			Resource sip = webInf.addPath("sip.xml");
			if (sip.exists()) 
				return sip;
			LOG.debug("No WEB-INF/sip.xml in " + context.getWar());
		}
		return null;
	}
}
