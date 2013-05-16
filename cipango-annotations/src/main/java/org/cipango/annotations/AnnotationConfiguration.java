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
package org.cipango.annotations;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.cipango.server.sipapp.SipAppContext;
import org.eclipse.jetty.annotations.AbstractDiscoverableAnnotationHandler;
import org.eclipse.jetty.annotations.AnnotationParser;
import org.eclipse.jetty.annotations.AnnotationParser.DiscoverableAnnotationHandler;
import org.eclipse.jetty.annotations.ClassNameResolver;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.AbstractConfiguration;
import org.eclipse.jetty.webapp.DiscoveredAnnotation;
import org.eclipse.jetty.webapp.WebAppContext;

/**
 * Configuration
 */
public class AnnotationConfiguration extends AbstractConfiguration
{
	private static final Logger LOG = Log.getLogger(AnnotationConfiguration.class);
    
    @Override
    public void configure(WebAppContext context) throws Exception
    { 
    	 
        AnnotationParser parser = new AnnotationParser();

        SipAppContext sac = context.getBean(SipAppContext.class);
        if (sac.getSpecVersion() == SipAppContext.VERSION_10)
        	return;
        

    	// FIXME is decorator needed as all classes are parsed
    	sac.addDecorator(new AnnotationDecorator(context)); 
        
        if (LOG.isDebugEnabled()) 
        	LOG.debug("parsing annotations");
        
        SipApplicationAnnotationHandler sipApplicationAnnotationHandler = new SipApplicationAnnotationHandler(context);
        parser.registerHandler(sipApplicationAnnotationHandler);
        parser.registerHandler(new SipApplicationKeyAnnotationHandler(context));
        parser.registerHandler(new SipListenerAnnotationHandler(context));
        parser.registerHandler(new SipServletAnnotationHandler(context));
                

        clearAnnotationList(parser.getAnnotationHandlers());
        parseContainerPath(context, parser);
        parseWebInfLib (context, parser);
        parseWebInfClasses(context, parser);
        
        //gather together all annotations discovered
        List<DiscoveredAnnotation> annotations = new ArrayList<DiscoveredAnnotation>();
        gatherAnnotations(annotations, parser.getAnnotationHandlers());
        sac.getMetaData().addDiscoveredAnnotations (annotations);
        
     }
    
    @Override
    public void cloneConfigure(WebAppContext template, WebAppContext context) throws Exception
    {
        context.addDecorator(new AnnotationDecorator(context));   
    }

    
    public void parseContainerPath (final WebAppContext context, final AnnotationParser parser)
    throws Exception
    {
        //if no pattern for the container path is defined, then by default scan NOTHING
        LOG.debug("Scanning container jars");
       
        //Convert from Resource to URI
        ArrayList<URI> containerUris = new ArrayList<URI>();
        for (Resource r : context.getMetaData().getContainerResources())
        {
            URI uri = r.getURI();
                containerUris.add(uri);          
        }
        
        parser.parse (containerUris.toArray(new URI[containerUris.size()]),
                new ClassNameResolver ()
                {
                    public boolean isExcluded (String name)
                    {
                        if (context.isSystemClass(name)) return false;
                        if (context.isServerClass(name)) return true;
                        return false;
                    }

                    public boolean shouldOverride (String name)
                    { 
                        //looking at system classpath
                        if (context.isParentLoaderPriority())
                            return true;
                        return false;
                    }
                });   
    }
    
    
    public void parseWebInfLib (final WebAppContext context, final AnnotationParser parser)
    throws Exception
    {          
        List<Resource> jars = context.getMetaData().getOrderedWebInfJars();
        
        //No ordering just use the jars in any order
        if (jars == null || jars.isEmpty())
            jars = context.getMetaData().getWebInfJars();
   
        for (Resource r : jars)
        {             
            URI uri  = r.getURI();
        
            parser.parse(uri, 
                         new ClassNameResolver()
                         {
                             public boolean isExcluded (String name)
                             {    
                                 if (context.isSystemClass(name)) return true;
                                 if (context.isServerClass(name)) return false;
                                 return false;
                             }

                             public boolean shouldOverride (String name)
                             {
                                //looking at webapp classpath, found already-parsed class of same name - did it come from system or duplicate in webapp?
                                if (context.isParentLoaderPriority())
                                    return false;
                                return true;
                             }
                         });  
        }
    }
     
    public void parseWebInfClasses (final WebAppContext context, final AnnotationParser parser)
    throws Exception
    {
        LOG.debug("Scanning classes in WEB-INF/classes");
        if (context.getWebInf() != null)
        {
        	Resource classesDir = context.getWebInf().addPath("classes/");
            if (classesDir.exists())
            {
                parser.parseDir(classesDir, 
                             new ClassNameResolver()
                {
                    public boolean isExcluded (String name)
                    {
                        if (context.isSystemClass(name)) return true;
                        if (context.isServerClass(name)) return false;
                        return false;
                    }

                    public boolean shouldOverride (String name)
                    {
                        //looking at webapp classpath, found already-parsed class of same name - did it come from system or duplicate in webapp?
                        if (context.isParentLoaderPriority())
                            return false;
                        return true;
                    }
                });
            }
        }
    }
    
    protected void clearAnnotationList (List<DiscoverableAnnotationHandler> handlers)
    {
        if (handlers == null)
            return;
        
        for (DiscoverableAnnotationHandler h:handlers)
        {
            if (h instanceof AbstractDiscoverableAnnotationHandler)
                ((AbstractDiscoverableAnnotationHandler)h).resetList();
        }
    }

    protected void gatherAnnotations (List<DiscoveredAnnotation> annotations, List<DiscoverableAnnotationHandler> handlers)
    {
        for (DiscoverableAnnotationHandler h:handlers)
        {
            if (h instanceof AbstractDiscoverableAnnotationHandler)
                annotations.addAll(((AbstractDiscoverableAnnotationHandler)h).getAnnotationList());
        }
    }
}
