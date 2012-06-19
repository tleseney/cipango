// ========================================================================
// Copyright 2007-2012 NEXCOM Systems
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

package org.cipango.server.sipapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.Enumeration;

import org.cipango.server.servlet.SipServletHandler;
import org.cipango.server.servlet.SipServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.StandardDescriptorProcessor;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.xml.XmlParser;
import org.junit.Test;
import org.xml.sax.SAXParseException;

public class SipXmlProcessorTest
{
	XmlParser getParser(boolean validating) throws ClassNotFoundException
	{
		String value = (validating ? "true" : "false");
		System.setProperty("org.eclipse.jetty.xml.XmlParser.Validating", value);
		
		return SipXmlProcessor.sipXmlParser(); 
	}
	
	@Test
	public void testXml() throws Exception
	{
		XmlParser parser = getParser(true);
		try
		{
			parser.parse(getClass().getResource("sip.xml").toString());
			fail("expected SAXParseException");
		}
		catch (SAXParseException e)
		{
		}
		
		parser = getParser(false);
		parser.parse(getClass().getResource("sip.xml").toString());
	}

	@Test
	public void testXmlDtd() throws Exception
	{
		XmlParser parser = getParser(true);
		parser.parse(getClass().getResource("sip-dtd.xml").toString());
	}
	
	protected Resource getResource(String name)
	{
		name = getClass().getPackage().getName().replaceAll("\\.", "/") + "/" + name;
		return Resource.newClassPathResource(name);
	}

	@Test
	public void testSipXml() throws Exception
	{
		System.setProperty("org.eclipse.jetty.xml.XmlParser.Validating", "false");
		
		SipXmlProcessor processor = new SipXmlProcessor(new SipAppContext());
		processor.parseSipXml(getResource("sip-xsd.xml"));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testSipXml10() throws Exception
	{
		//System.setProperty("org.eclipse.jetty.xml.XmlParser.Validating", "false");
		
		SipAppContext context = new SipAppContext();
		WebAppContext webAppContext = new WebAppContext();
		context.setWebAppContext(webAppContext);
		
		SipXmlProcessor processor = new SipXmlProcessor(context);
		processor.parseSipXml(getResource("sip-sample-1.0.xml"));
		processor.processSipXml();
		SipServletHandler servletHandler = (SipServletHandler) context.getServletHandler();
		
		assertEquals(SipAppContext.VERSION_10, context.getSpecVersion());
		assertEquals("SIP Servlet based Registrar", webAppContext.getDisplayName());
		
		Enumeration<String> e = webAppContext.getInitParameterNames();
		String name = (String) e.nextElement();
		assertEquals("contextConfigLocation", name);
		assertEquals("/WEB-INF/kaleo.xml", webAppContext.getInitParameter(name));
		assertFalse(e.hasMoreElements());	
		
		assertEquals(TestListener.class, context.getTimerListeners()[0].getClass());
	
		// servlets
		SipServletHolder[] holders = servletHandler.getServlets();
		assertEquals(2, holders.length);
		
		assertEquals("main", holders[0].getName());
		// TODO assertEquals("PBX Servlet", holders[0].getDisplayName());
		assertEquals("org.cipango.kaleo.PbxServlet", holders[0].getClassName());
		e = holders[0].getInitParameterNames();
		name = (String) e.nextElement();
		assertEquals("value", holders[0].getInitParameter(name));
		assertFalse(e.hasMoreElements());	
		assertEquals(10, holders[0].getInitOrder());
		
		assertEquals("presence", holders[1].getName());
		assertEquals("org.cipango.kaleo.presence.PresenceServlet", holders[1].getClassName());
		assertFalse(holders[1].getInitParameterNames().hasMoreElements());	
		assertEquals(0, holders[1].getInitOrder());
		
		// servlet-mapping
		SipServletMapping[] mappings = servletHandler.getServletMappings();
		assertEquals(1, mappings.length);
		assertEquals("main", mappings[0].getServletName());
		assertEquals("((request.method == REGISTER) or (request.method == PUBLISH) or (request.method == SUBSCRIBE) or (request.method == INVITE))", 
				mappings[0].getMatchingRuleExpression());
		assertEquals(60, context.getSessionTimeout());
		
		assertNull(servletHandler.getMainServlet());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testSipXml11() throws Exception
	{
		System.setProperty("org.eclipse.jetty.xml.XmlParser.Validating", "false");
		SipAppContext context = new SipAppContext();
		WebAppContext webAppContext = new WebAppContext();
		context.setWebAppContext(webAppContext);
		
		SipXmlProcessor processor = new SipXmlProcessor(context);
		processor.parseSipXml(getResource("sip-sample-1.1.xml"));
		processor.processSipXml();
		
		assertEquals(SipAppContext.VERSION_11, context.getSpecVersion());
		assertEquals("SIP Servlet based Registrar", webAppContext.getDisplayName());
		
		Enumeration<String> e = webAppContext.getInitParameterNames();
		String name = (String) e.nextElement();
		assertEquals("contextConfigLocation", name);
		assertEquals("/WEB-INF/kaleo.xml", webAppContext.getInitParameter(name));
		assertFalse(e.hasMoreElements());	
		
		assertEquals(TestListener.class, context.getTimerListeners()[0].getClass());
	
		// servlets
		SipServletHandler servletHandler = (SipServletHandler) context.getServletHandler();
		SipServletHolder[] holders = servletHandler.getServlets();
		assertEquals(2, holders.length);
		
		assertEquals("main", holders[0].getName());
		// TODO assertEquals("PBX Servlet", holders[0].getDisplayName());
		assertEquals("org.cipango.kaleo.PbxServlet", holders[0].getClassName());
		e = holders[0].getInitParameterNames();
		name = (String) e.nextElement();
		assertEquals("value", holders[0].getInitParameter(name));
		assertFalse(e.hasMoreElements());	
		assertEquals(10, holders[0].getInitOrder());
		
		assertEquals("presence", holders[1].getName());
		assertEquals("org.cipango.kaleo.presence.PresenceServlet", holders[1].getClassName());
		assertFalse(holders[1].getInitParameterNames().hasMoreElements());	
		assertEquals(0, holders[1].getInitOrder());
		
		// servlet-mapping
		SipServletMapping[] mappings = servletHandler.getServletMappings();
		assertEquals(0, mappings.length);
		assertEquals(60, context.getSessionTimeout());
		
		assertNotNull(servletHandler.getMainServlet());
		assertEquals("main", servletHandler.getMainServlet().getName());
		assertEquals("org.cipango.kaleo", context.getName());
	}

	@Test
	public void testMappings11() throws Exception 
	{
		System.setProperty("org.eclipse.jetty.xml.XmlParser.Validating", "false");
		SipAppContext context = new SipAppContext();
		WebAppContext webAppContext = new WebAppContext();
		context.setWebAppContext(webAppContext);
		
		SipXmlProcessor processor = new SipXmlProcessor(context);
		processor.parseSipXml(getResource("sip-mappings-1.1.xml"));
		processor.processSipXml();
		
		assertEquals(SipAppContext.VERSION_11, context.getSpecVersion());
		
		SipServletHandler servletHandler = (SipServletHandler) context.getServletHandler();
		SipServletMapping[] mappings = servletHandler.getServletMappings();
		assertEquals(2, mappings.length);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testNamespace() throws Exception
	{
		System.setProperty("org.eclipse.jetty.xml.XmlParser.Validating", "false");
		SipAppContext context = new SipAppContext();
		WebAppContext webAppContext = new WebAppContext();
		context.setWebAppContext(webAppContext);
		
		SipXmlProcessor processor = new SipXmlProcessor(context);
		processor.parseSipXml(getResource("sip-namespace.xml"));
		processor.processSipXml();

		
		assertEquals(SipAppContext.VERSION_11, context.getSpecVersion());
		
		Enumeration<String> e = webAppContext.getInitParameterNames();
		String name = (String) e.nextElement();
		assertNotNull(name);
		assertEquals("contextConfigLocation", name);
		assertEquals("/WEB-INF/kaleo.xml", webAppContext.getInitParameter(name));
		assertFalse(e.hasMoreElements());	
		
		// servlets
		SipServletHandler servletHandler = (SipServletHandler) context.getServletHandler();
		SipServletHolder[] holders = servletHandler.getServlets();
		assertEquals(1, holders.length);
		
		assertEquals("main", holders[0].getName());
		// TODO assertEquals("PBX Servlet", holders[0].getDisplayName());
		assertEquals("org.cipango.kaleo.PbxServlet", holders[0].getClassName());
		e = holders[0].getInitParameterNames();
		name = (String) e.nextElement();
		assertEquals("value", holders[0].getInitParameter(name));
		assertFalse(e.hasMoreElements());			
		
		// servlet-mapping
		SipServletMapping[] mappings = servletHandler.getServletMappings();
		assertEquals(0, mappings.length);
		
		assertEquals("main", servletHandler.getMainServlet().getName());
		
		assertEquals("org.cipango.kaleo", context.getName());	
	}

	@Test
	public void testValidateSip() throws Exception
	{
		System.setProperty("org.eclipse.jetty.xml.XmlParser.Validating", "true");
		
		SipAppContext context = new SipAppContext();
		WebAppContext webAppContext = new WebAppContext();
		context.setWebAppContext(webAppContext);
		SipXmlProcessor processor = new SipXmlProcessor(context);
		
		processor.parseSipXml(getResource("sip-validated-1.1.xml"));
		processor.processSipXml();
//		StandardDescriptorProcessor processor = new StandardDescriptorProcessor();
//		
//		SipDescriptor descriptor = new SipDescriptor(getResource("sip-validated-1.1.xml"));
//		descriptor.setValidating(true);
//		descriptor.parse();
//			
//		processor.process(new SipAppContext(), descriptor);
	}

	@Test
	public void testXmlXsd() throws Exception
	{
		//System.out.println(WebAppContext.class.getResource("/javax/servlet/resources/javaee_5.xsd"));
		XmlParser parser = getParser(true);
		parser.parse(getClass().getResource("sip-xsd.xml").toString());
	}

	@Test
	public void testWeb() throws Exception 
	{
		XmlParser parser = getParser(true);
		parser.parse(getClass().getResource("web.xml").toString());
	}
	/**/
	
}
