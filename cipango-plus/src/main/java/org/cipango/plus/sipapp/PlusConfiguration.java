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
package org.cipango.plus.sipapp;

import org.cipango.server.sipapp.SipAppContext;
import org.eclipse.jetty.plus.webapp.PlusDescriptorProcessor;
import org.eclipse.jetty.webapp.WebAppContext;

/**
 * Configuration
 * 
 * 
 */
public class PlusConfiguration extends org.eclipse.jetty.plus.webapp.PlusConfiguration
{

	@Override
	public void preConfigure(WebAppContext webAppContext) throws Exception
	{
		SipAppContext context = webAppContext.getBean(SipAppContext.class);
		SipResourceDecorator decorator = new SipResourceDecorator(context);
		webAppContext.addDecorator(decorator);
		context.addDecorator(decorator);
		super.preConfigure(webAppContext);
	}

	@Override
	public void configure(WebAppContext webAppContext) throws Exception
	{
		super.configure(webAppContext);
		SipAppContext context = webAppContext.getBean(SipAppContext.class);
		context.getMetaData().addDescriptorProcessor(new PlusDescriptorProcessor());
	}
}
