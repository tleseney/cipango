// ========================================================================
// Copyright 2010-2015 NEXCOM Systems
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

import org.cipango.server.sipapp.SipAppContext;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.webapp.WebAppContext;

/**
 * Configuration
 */
public class AnnotationConfiguration extends org.eclipse.jetty.annotations.AnnotationConfiguration
{
	private static final Logger LOG = Log.getLogger(AnnotationConfiguration.class);
    
    @Override
    public void configure(WebAppContext context) throws Exception
    { 
        SipAppContext sac = context.getBean(SipAppContext.class);	
        if (sac.getSpecVersion() != SipAppContext.VERSION_10)
        {            
            if (LOG.isDebugEnabled()) 
            	LOG.debug("parsing SIP annotations");
            
            SipApplicationAnnotationHandler sipApplicationAnnotationHandler = new SipApplicationAnnotationHandler(context);
            _discoverableAnnotationHandlers.add(sipApplicationAnnotationHandler);
            _discoverableAnnotationHandlers.add(new SipApplicationKeyAnnotationHandler(context));
            _discoverableAnnotationHandlers.add(new SipListenerAnnotationHandler(context));
            _discoverableAnnotationHandlers.add(new SipServletAnnotationHandler(context));
        }
        super.configure(context);

    	sac.addDecorator(new AnnotationDecorator(context)); 
     }
}
