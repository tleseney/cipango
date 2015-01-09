// ========================================================================
// Copyright (c) 2006-2009 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// All rights reserved. This program and the accompanying materials
// are made available under the terms of the Eclipse Public License v1.0
// and Apache License v2.0 which accompanies this distribution.
// The Eclipse Public License is available at 
// http://www.eclipse.org/legal/epl-v10.html
// The Apache License v2.0 is available at
// http://www.opensource.org/licenses/apache2.0.php
// You may elect to redistribute this code under either of these licenses. 
// ========================================================================

package org.cipango.annotations;

import org.eclipse.jetty.annotations.AnnotationIntrospector;
import org.eclipse.jetty.servlet.ServletContextHandler.Decorator;
import org.eclipse.jetty.webapp.WebAppContext;


public class AnnotationDecorator implements Decorator
{
    AnnotationIntrospector _introspector = new AnnotationIntrospector();
    
    /**
     * @param context
     */
    public AnnotationDecorator(WebAppContext context)
    {
        _introspector.registerHandler(new ResourceAnnotationHandler(context));
    }

    /**
     * Look only for annotation @Resource as other annotations will be processed by
     * org.eclipse.jetty.annotations.AnnotationDecorator
     */
    protected void introspect (Object o)
    {
        _introspector.introspect(o.getClass());
    }


	@Override
	public <T> T decorate(T o) 
	{
		introspect(o);
        return o;
	}

	@Override
	public void destroy(Object o) 
	{
		
	}
}
