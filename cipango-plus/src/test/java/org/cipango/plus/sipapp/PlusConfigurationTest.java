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
package org.cipango.plus.sipapp;

import javax.naming.InitialContext;

import org.cipango.server.SipServer;
import org.cipango.server.sipapp.SipAppContext;
import org.cipango.server.sipapp.SipXmlConfiguration;
import org.eclipse.jetty.plus.webapp.EnvConfiguration;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppClassLoader;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.Assert;
import org.junit.Test;

public class PlusConfigurationTest
{
	
	@Test
	public void testConfig() throws Exception
	{
		 ClassLoader old_loader = Thread.currentThread().getContextClassLoader();

        try
        {
            InitialContext ic = new InitialContext();

            SipServer server = new SipServer();

            SipAppContext context = new SipAppContext();
            WebAppContext webAppContext = new WebAppContext();
            context.setWebAppContext(webAppContext);
            
            context.setServer(server);
            context.getMetaData().setAppName("myApp");
            webAppContext.setClassLoader(new WebAppClassLoader(Thread.currentThread().getContextClassLoader(), webAppContext));
            context.getOverrideDescriptors().add(getClass().getResource("/sip.xml").toString());
           
            
            Thread.currentThread().setContextClassLoader(webAppContext.getClassLoader());
            
			PlusConfiguration plusConfiguration = new PlusConfiguration();
			EnvConfiguration envConfiguration = new EnvConfiguration();
			envConfiguration.setJettyEnvXml(getClass().getResource("/jetty-env.xml"));

			// Need to add a listener or a servlet to ensure that SipFactory is set in JNDI
			context.getMetaData().addListener("org.cipango.plus.sipapp.Listener");
			
			webAppContext.setConfigurations(new Configuration[] {envConfiguration, plusConfiguration, new SipXmlConfiguration()});
			webAppContext.preConfigure();
			webAppContext.configure();
			webAppContext.postConfigure();		
			context.getMetaData().resolve(context);
			

			 Object lookup = ic.lookup("java:comp/env/resource/sample");
			 Assert.assertNotNull(lookup);
			 Assert.assertEquals("1234", lookup);
			 
			 Assert.assertEquals(context.getSipFactory(), ic.lookup("java:comp/env/sip/myApp/SipFactory"));
			 Assert.assertEquals(context.getTimerService(), ic.lookup("java:comp/env/sip/myApp/TimerService"));
			 Assert.assertEquals(context.getSipSessionsUtil(), ic.lookup("java:comp/env/sip/myApp/SipSessionsUtil"));
        }
        finally
        {
            Thread.currentThread().setContextClassLoader(old_loader);
        }
	}
}
