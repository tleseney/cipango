// ========================================================================
// Copyright 2012 NEXCOM Systems
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

import java.util.List;

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
		LOG.info("SipXmlConfiguration::preConfigure context " + context.getContextPath());

		Resource sipXml = findSipXml(context);
		if (sipXml != null) // FIXME case only annotations and no sip.xml
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

			
			SipXmlProcessor processor = context.getBean(SipXmlProcessor.class);
			if (processor == null)
			{
				processor = new SipXmlProcessor(sac);
				context.addBean(processor);
			}
			
			String defaultsDescriptor = sac.getDefaultsDescriptor();
			if (defaultsDescriptor != null && defaultsDescriptor.length() > 0)
			{
				Resource dftSipResource = Resource.newSystemResource(defaultsDescriptor);
				if (dftSipResource == null)
					dftSipResource = context.newResource(defaultsDescriptor);
				processor.parseDefaults(dftSipResource);
				processor.processDefaults();
			}

			if (sipXml != null)
				processor.parseSipXml(sipXml);

			LOG.info("SIP Application: " + context.getWar());
		}
	}

	public void configure(WebAppContext context) throws Exception
	{
		LOG.info("SipXmlConfiguration::configure");

		SipAppContext sac = context.getBean(SipAppContext.class);
		if (sac != null)
		{
			SipXmlProcessor processor = context.getBean(SipXmlProcessor.class);
			if (processor == null)
			{
				processor = new SipXmlProcessor(sac);
				context.addBean(processor);
			}
			
			processor.processSipXml();
			
			List<String> overrideSipDescriptors = sac.getOverrideDescriptors();
			if (overrideSipDescriptors != null)
			{
				for (String descriptor : overrideSipDescriptors)
				{
					Resource overrideSipResource = Resource.newSystemResource(descriptor);
					if (overrideSipResource == null)
						overrideSipResource = context.newResource(descriptor);
					processor.parseSipOverride(overrideSipResource);
					processor.processSipOverride();
				}
			}
		}
	}

	protected Resource findSipXml(WebAppContext context) throws Exception
	{
		Resource webInf = context.getWebInf();
		if (webInf != null && webInf.isDirectory())
		{
			Resource sipXml = webInf.addPath("sip.xml");
			if (sipXml.exists())
				return sipXml;
			LOG.info("No WEB-INF/sip.xml in " + context.getWar());
		}
		return null;
	}
}
