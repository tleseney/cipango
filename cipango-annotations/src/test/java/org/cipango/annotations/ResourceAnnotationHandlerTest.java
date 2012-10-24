package org.cipango.annotations;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

import java.util.List;

import javax.annotation.Resource;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServletContextEvent;
import javax.servlet.sip.SipServletListener;
import javax.servlet.sip.annotation.SipListener;

import org.cipango.server.sipapp.SipAppContext;
import org.eclipse.jetty.plus.annotation.Injection;
import org.eclipse.jetty.plus.annotation.InjectionCollection;
import org.eclipse.jetty.servlet.ServletContextHandler.Decorator;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.Before;
import org.junit.Test;

public class ResourceAnnotationHandlerTest
{
	private SipAppContext _context;
	private InjectionCollection _injections;
	private Decorator _decorator; 

	@Before
	public void setUp() throws Exception
	{
		_context = new SipAppContext();
		WebAppContext webAppContext = new WebAppContext();
		_context.setWebAppContext(webAppContext);
		_injections = new InjectionCollection();
		 webAppContext.setAttribute(InjectionCollection.INJECTION_COLLECTION, _injections);
		_decorator = new AnnotationDecorator(webAppContext);
	}

	@Test		
	public void testBadResource() throws Exception
	{
		_decorator.decorateServletInstance(new BadRessource());
		
		assertNull(_injections.getInjections(BadRessource.class.getName()));
		
		_decorator.decorateServletInstance(new BadRessource2());
		assertNull(_injections.getInjections(BadRessource2.class.getName()));
	}

	@Test
	public void testSipFactory() throws Exception
	{
		_context.setName("org.cipango.kaleo");
		_decorator.decorateListenerInstance(new ListenerRessource());
	
		List<Injection> injections = _injections.getInjections(ListenerRessource.class.getName());
		assertEquals(1, injections.size());
		assertEquals("sip/org.cipango.kaleo/SipFactory", injections.get(0).getJndiName());
	}
	
}


@SuppressWarnings("serial")
class BadRessource extends javax.servlet.sip.SipServlet
{
	@Resource
	protected static SipFactory sipFactory;
}

@SuppressWarnings("serial")
class BadRessource2 extends javax.servlet.sip.SipServlet
{
	@Resource
	protected final SipFactory sipFactory = null;
}

@SipListener (applicationName="org.cipango.kaleo")
class ListenerRessource implements SipServletListener
{
	@Resource
	protected SipFactory sipFactory ;

	public void servletInitialized(SipServletContextEvent arg0)
	{
	}
}
