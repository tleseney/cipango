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
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.xml.XmlParser;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.xml.sax.SAXParseException;

public class SipXmlProcessorTest
{
	private SipAppContext _context;
	private WebAppContext _webAppContext;
	
	@Before
	public void setUp()
	{
		_context = new SipAppContext();
		_webAppContext = new WebAppContext();
		_context.setWebAppContext(_webAppContext);
		_webAppContext.addBean(_context);
	}
	
	private XmlParser getParser(boolean validating) throws ClassNotFoundException
	{
		String value = (validating ? "true" : "false");
		System.setProperty("org.eclipse.jetty.xml.XmlParser.Validating", value);
		
		return new SipDescriptor(null).newParser(); 
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
		SipDescriptor descriptor = new SipDescriptor(getResource("sip-xsd.xml"));
		descriptor.parse();
		StandardDescriptorProcessor processor = new StandardDescriptorProcessor();
		processor.process(_webAppContext, descriptor);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testSipXml10() throws Exception
	{
		//System.setProperty("org.eclipse.jetty.xml.XmlParser.Validating", "false");
		
		MetaData metaData = _context.getMetaData();
		metaData.setSipXml(getResource("sip-sample-1.0.xml"));
		metaData.addDescriptorProcessor(new StandardDescriptorProcessor());
		metaData.resolve(_context);

		SipServletHandler servletHandler = (SipServletHandler) _context.getServletHandler();
		
		assertEquals(SipAppContext.VERSION_10, _context.getSpecVersion());
		assertEquals("SIP Servlet based Registrar", _webAppContext.getDisplayName());
		
		Enumeration<String> e = _webAppContext.getInitParameterNames();
		String name = (String) e.nextElement();
		assertEquals("contextConfigLocation", name);
		assertEquals("/WEB-INF/kaleo.xml", _webAppContext.getInitParameter(name));
		assertFalse(e.hasMoreElements());	
		
		assertEquals(TestListener.class, _context.getTimerListeners().get(0).getClass());
	
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
		assertEquals(60, _context.getSessionHandler().getSessionManager().getSessionTimeout());
		
		assertNull(servletHandler.getMainServlet());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testSipXml11() throws Exception
	{
		System.setProperty("org.eclipse.jetty.xml.XmlParser.Validating", "false");
		
		MetaData metaData = _context.getMetaData();
		metaData.setSipXml(getResource("sip-sample-1.1.xml"));
		metaData.addDescriptorProcessor(new StandardDescriptorProcessor());
		metaData.resolve(_context);		
		
		assertEquals(SipAppContext.VERSION_11, _context.getSpecVersion());
		assertEquals("SIP Servlet based Registrar", _webAppContext.getDisplayName());
		
		Enumeration<String> e = _webAppContext.getInitParameterNames();
		String name = (String) e.nextElement();
		assertEquals("contextConfigLocation", name);
		assertEquals("/WEB-INF/kaleo.xml", _webAppContext.getInitParameter(name));
		assertFalse(e.hasMoreElements());	
		
		assertEquals(TestListener.class, _context.getTimerListeners().get(0).getClass());
	
		// servlets
		SipServletHandler servletHandler = (SipServletHandler) _context.getServletHandler();
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
		assertNull(mappings);
		assertEquals(60, _context.getSessionHandler().getSessionManager().getSessionTimeout());
		
		assertNotNull(servletHandler.getMainServlet());
		assertEquals("main", servletHandler.getMainServlet().getName());
		assertEquals("org.cipango.kaleo", _context.getName());
	}

	@Test
	public void testMappings11() throws Exception 
	{
		System.setProperty("org.eclipse.jetty.xml.XmlParser.Validating", "false");
		MetaData metaData = _context.getMetaData();
		metaData.setSipXml(getResource("sip-mappings-1.1.xml"));
		metaData.addDescriptorProcessor(new StandardDescriptorProcessor());
		metaData.resolve(_context);	
		
		assertEquals(SipAppContext.VERSION_11, _context.getSpecVersion());
		
		SipServletHandler servletHandler = (SipServletHandler) _context.getServletHandler();
		SipServletMapping[] mappings = servletHandler.getServletMappings();
		assertEquals(2, mappings.length);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testNamespace() throws Exception
	{
		System.setProperty("org.eclipse.jetty.xml.XmlParser.Validating", "false");
		MetaData metaData = _context.getMetaData();
		metaData.setSipXml(getResource("sip-namespace.xml"));
		metaData.addDescriptorProcessor(new StandardDescriptorProcessor());
		metaData.resolve(_context);	
				
		assertEquals(SipAppContext.VERSION_11, _context.getSpecVersion());
		
		Enumeration<String> e = _webAppContext.getInitParameterNames();
		String name = (String) e.nextElement();
		assertNotNull(name);
		assertEquals("contextConfigLocation", name);
		assertEquals("/WEB-INF/kaleo.xml", _webAppContext.getInitParameter(name));
		assertFalse(e.hasMoreElements());	
		
		// servlets
		SipServletHandler servletHandler = (SipServletHandler) _context.getServletHandler();
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
		assertNull(mappings);
		
		assertEquals("main", servletHandler.getMainServlet().getName());
		
		assertEquals("org.cipango.kaleo", _context.getName());	
	}


	@Ignore // Ignore this test as it could impact other tests in specific configuration
	public void testValidateSip() throws Exception
	{
		System.setProperty("org.eclipse.jetty.xml.XmlParser.Validating", "true");
		
		MetaData metaData = _context.getMetaData();
		metaData.setSipXml(getResource("sip-validated-1.1.xml"));
		metaData.addDescriptorProcessor(new StandardDescriptorProcessor());
		metaData.resolve(_context);	
	}

	@Test
	public void testXmlXsd() throws Exception
	{
		//System.out.println(WebAppContext.class.getResource("/javax/servlet/resources/javaee_5.xsd"));
		XmlParser parser = getParser(true);
		parser.parse(getClass().getResource("sip-xsd.xml").toString());
	}

	@Ignore
	public void testWeb() throws Exception 
	{
		XmlParser parser = getParser(true);
		parser.parse(getClass().getResource("web.xml").toString());
	}
	/**/
	
}
