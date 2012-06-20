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

import java.util.EventListener;

import javax.servlet.sip.annotation.SipListener;

import org.cipango.server.sipapp.SipAppContext;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.webapp.DiscoveredAnnotation;
import org.eclipse.jetty.webapp.WebAppContext;

public class SipListenerAnnotation extends DiscoveredAnnotation
{
	private static final Logger LOG = Log.getLogger(SipListenerAnnotation.class);
	
    public SipListenerAnnotation (WebAppContext context, String className)
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
        
        if (!EventListener.class.isAssignableFrom(clazz))
        {
            LOG.warn(clazz.getName()+" is not assignable from java.util.EventListener");
            return;
        }
        
        SipListener annotation = (SipListener) clazz.getAnnotation(SipListener.class);
        
   
        SipAppContext context = _context.getBean(SipAppContext.class);
		
        if (!Util.isEmpty(annotation.applicationName()))
        {
        	
        	if (context.getName() != null && !context.getName().equals(annotation.applicationName()))
    			throw new IllegalStateException("App-name in sip.xml: " + context.getName() 
    					+ " does not match with SipApplication annotation: " + annotation.applicationName());
        	context.getMetaData().setAppName(annotation.applicationName());
        }
  
        context.getMetaData().addListener(_className);
    }
}
