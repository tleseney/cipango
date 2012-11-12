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
package org.cipango.annotations;

import javax.servlet.sip.annotation.SipApplication;

import org.cipango.server.sipapp.SipAppContext;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.webapp.DiscoveredAnnotation;
import org.eclipse.jetty.webapp.WebAppContext;

public class SipApplicationAnnotation extends DiscoveredAnnotation
{
	private static final Logger LOG = Log.getLogger(SipApplicationAnnotation.class);
	
    public SipApplicationAnnotation (WebAppContext context, String className)
    {
        super(context, className);
    }

    /** 
     * @see org.eclipse.jetty.annotations.ClassAnnotation#apply()
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
	public void apply()
    {
        Class clazz = getTargetClass();
        
        if (clazz == null)
        {
            LOG.warn(_className+" cannot be loaded");
            return;
        }
        
     
        SipApplication annotation = (SipApplication) clazz.getAnnotation(SipApplication.class);
        
   
        SipAppContext context = _context.getBean(SipAppContext.class);
    
		if (context.getName() != null && !context.getName().equals(annotation.name()))
			throw new IllegalStateException("App-name in sip.xml: " + context.getName() 
					+ " does not match with SipApplication annotation: " + annotation.name());
		context.getMetaData().setAppName(annotation.name());
		
		_context.setDistributable(annotation.distributable());
		_context.setDisplayName(annotation.displayName());
		context.setProxyTimeout(annotation.proxyTimeout());
		context.getSessionHandler().getSessionManager().setSessionTimeout(annotation.sessionTimeout());
        context.getMetaData().setMainServletName(annotation.mainServlet());
    }
}
