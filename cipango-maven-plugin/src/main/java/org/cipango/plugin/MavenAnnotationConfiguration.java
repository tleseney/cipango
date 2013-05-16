//========================================================================
//$Id: Jetty6MavenConfiguration.java 3766 2008-10-08 07:59:53Z janb $
//Copyright 2000-2005 Mort Bay Consulting Pty. Ltd.
//------------------------------------------------------------------------
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at 
//http://www.apache.org/licenses/LICENSE-2.0
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
//========================================================================
package org.cipango.plugin;

import java.io.File;

import org.cipango.annotations.AnnotationConfiguration;
import org.eclipse.jetty.annotations.AnnotationParser;
import org.eclipse.jetty.annotations.ClassNameResolver;
import org.eclipse.jetty.maven.plugin.JettyWebAppContext;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.WebAppContext;

public class MavenAnnotationConfiguration extends AnnotationConfiguration 
{
	private static final Logger LOG = Log.getLogger(MavenAnnotationConfiguration.class);
	
	   /* ------------------------------------------------------------ */
    @Override
    public void parseWebInfClasses(final WebAppContext context, final AnnotationParser parser) throws Exception
    {

        JettyWebAppContext jwac = (JettyWebAppContext) context;
        if (jwac.getClassPathFiles() == null)
            super.parseWebInfClasses (context, parser);
        else
        {
            LOG.debug("Scanning classes ");
            //Look for directories on the classpath and process each one of those
            for (File f:jwac.getClassPathFiles())
            {
                if (f.isDirectory() && f.exists())
                {
                    parser.parseDir(Resource.newResource(f.toURL()), 
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
    }
}
