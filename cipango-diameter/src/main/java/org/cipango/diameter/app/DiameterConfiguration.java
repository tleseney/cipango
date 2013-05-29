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
package org.cipango.diameter.app;

import org.cipango.diameter.api.DiameterFactory;
import org.cipango.diameter.node.DiameterFactoryImpl;
import org.cipango.diameter.node.Node;
import org.cipango.server.sipapp.SipAppContext;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.webapp.AbstractConfiguration;

public class DiameterConfiguration extends AbstractConfiguration
{
	private static final Logger LOG = Log.getLogger(DiameterConfiguration.class);

	@Override
	public void preConfigure(org.eclipse.jetty.webapp.WebAppContext context) throws Exception
	{
		if (context.isStarted())
        {
           	LOG.debug("Cannot configure webapp after it is started");
            return;
        } 
				
		DiameterFactoryImpl factory = new DiameterFactoryImpl();
		Node node = (Node) context.getServer().getAttribute(Node.class.getName());
		factory.setNode(node);
		factory.setAppContext(context.getBean(SipAppContext.class));
		
		context.setAttribute(DiameterFactory.class.getName(), factory);
		
		context.addDecorator(new DiameterDecorator((DiameterContext) node.getHandler(), context));
		
	}
	
	@Override
	public void deconfigure(org.eclipse.jetty.webapp.WebAppContext context) throws Exception
	{
		Node node = (Node) context.getServer().getAttribute(Node.class.getName());
		((DiameterContext) node.getHandler()).removeListeners(context);
	}


	
}
