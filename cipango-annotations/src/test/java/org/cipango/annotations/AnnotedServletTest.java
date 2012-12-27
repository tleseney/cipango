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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.*;

import java.util.EventListener;
import java.util.Iterator;
import java.util.List;

import org.cipango.annotations.resources.AnnotedServlet;
import org.cipango.server.servlet.SipServletHandler;
import org.cipango.server.servlet.SipServletHolder;
import org.cipango.server.sipapp.SipAppContext;
import org.eclipse.jetty.annotations.AnnotationParser;
import org.eclipse.jetty.plus.annotation.Injection;
import org.eclipse.jetty.plus.annotation.InjectionCollection;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.util.resource.FileResource;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.Before;
import org.junit.Test;

public class AnnotedServletTest
{
	private SipAppContext _context;
	private WebAppContext _webAppContext;
	
	@Before
	public void setUp() throws Exception
	{
		_context = new SipAppContext();
		_webAppContext = new WebAppContext();
		_context.setWebAppContext(_webAppContext);
		_webAppContext.setSecurityHandler(new ConstraintSecurityHandler());
	}
	
	@Test
	public void testAnnotedServlet() throws Exception
	{		
        AnnotationConfiguration annotConfiguration = new AnnotationConfiguration()
        {
        	@Override
    		public void parseContainerPath(WebAppContext arg0, AnnotationParser arg1) throws Exception
    		{

    		}

    		@Override
    		public void parseWebInfClasses(WebAppContext context, AnnotationParser parser) throws Exception
    		{
    			Resource r = new FileResource(AnnotedServletTest.class.getResource("resources"));
    	        parser.parse(r , new SimpleResolver());
    		}

    		@Override
    		public void parseWebInfLib(WebAppContext arg0, AnnotationParser arg1) throws Exception
    		{
    		}
        };
        annotConfiguration.preConfigure(_webAppContext);
        annotConfiguration.configure(_webAppContext);
        _context.getMetaData().resolve(_context);
        
        assertEquals("org.cipango.kaleo", _context.getName());
        assertEquals("Kaleo", _webAppContext.getDisplayName());
        
        SipServletHandler handler = _context.getServletHandler();
        SipServletHolder[] holders = handler.getServlets();
        assertEquals(1, holders.length);
        assertEquals("AnnotedServlet", holders[0].getName());
        assertEquals(-1, holders[0].getInitOrder());
        
        assertEquals(holders[0], handler.getMainServlet());
        
        AnnotedServlet servlet = (AnnotedServlet) holders[0].getServlet();
        assertNotNull(servlet);
        
        InjectionCollection injectionCollection = (InjectionCollection) _webAppContext.getAttribute(InjectionCollection.INJECTION_COLLECTION);
        List<Injection> injections = injectionCollection.getInjections(AnnotedServlet.class.getName());
		assertEquals(3, injections.size());
		Iterator<Injection> it  = injections.iterator();
		while (it.hasNext())
		{
			Injection injection = it.next();
			String name = injection.getTarget().getName();
			if (name.equals("sipFactory"))
				assertEquals("sip/org.cipango.kaleo/SipFactory", injection.getJndiName());
			else if (name.equals("timerService"))
				assertEquals("sip/org.cipango.kaleo/TimerService", injection.getJndiName());
			else if (name.equals("sessionsUtil"))
				assertEquals("sip/org.cipango.kaleo/SipSessionsUtil", injection.getJndiName());
			else
				fail("Unexpected name: " + name);
		}
        
        EventListener[] listeners = _context.getEventListeners();
        boolean found = false; // There could be 2 listeners as SipAppContext had a listener

        for (EventListener l : listeners)
        {
        	if (l instanceof AnnotedServlet)
        		found = true;
        }
        assertTrue("No listener found", found);
	}

}
