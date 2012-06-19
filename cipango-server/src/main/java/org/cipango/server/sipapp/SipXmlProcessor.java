// ========================================================================
// Copyright 2012 NEXCOM Systems
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

import java.net.URL;
import java.util.EventListener;
import java.util.Iterator;

import javax.servlet.Servlet;
import javax.servlet.UnavailableException;
import javax.servlet.sip.SipServlet;

import org.cipango.server.servlet.SipServletHandler;
import org.cipango.server.servlet.SipServletHolder;
import org.cipango.server.sipapp.rules.AndRule;
import org.cipango.server.sipapp.rules.ContainsRule;
import org.cipango.server.sipapp.rules.EqualsRule;
import org.cipango.server.sipapp.rules.ExistsRule;
import org.cipango.server.sipapp.rules.MatchingRule;
import org.cipango.server.sipapp.rules.NotRule;
import org.cipango.server.sipapp.rules.OrRule;
import org.cipango.server.sipapp.rules.SubdomainRule;
import org.eclipse.jetty.util.LazyList;
import org.eclipse.jetty.util.Loader;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebDescriptor;
import org.eclipse.jetty.xml.XmlParser;

public class SipXmlProcessor 
{
	private static final Logger LOG = Log.getLogger(SipXmlProcessor.class);
	
	protected SipAppContext _context;
	protected XmlParser _xmlParser;
	protected Descriptor _sipDefaultsRoot;
	protected Descriptor _sipXmlRoot;
	protected Descriptor _sipOverrideRoot;
	
	protected String _appName;
	
	protected Object _listeners;
	protected Object _servlets;
	protected Object _servletMappings;
	protected String _mainServlet;
	
	protected Object _listenerClasses;
	
	public class Descriptor
	{
		protected Resource _xml;
		protected XmlParser.Node _root;
		protected int _version = SipAppContext.VERSION_11;
		
		public Descriptor(Resource xml)
		{
			_xml = xml;
		}
		
		public void parse() throws Exception
		{
			if (_root == null)
			{
				_root = _xmlParser.parse(_xml.getURL().toString());
				processVersion();
				processOrdering();
			}
		}
		
		private void processVersion()
		{
			String version = _root.getAttribute("version", "DTD");
			if ("1.0".equals(version))
				_version = SipAppContext.VERSION_10;
			else if ("1.1".equals(version))
				_version = SipAppContext.VERSION_11;
			else if ("DTD".equals(version))
			{
				String schemaLocation = _root.getAttribute("schemaLocation");
				if (schemaLocation != null && schemaLocation.indexOf("sip-app_1_1.xsd") > 0)
					_version = SipAppContext.VERSION_11;
				else
					_version = SipAppContext.VERSION_10;
			}
		}
		
		private void processOrdering()
		{
		}
		
		public void process() throws Exception
		{
			SipXmlProcessor.this.process(_root);
		}
		
	}
	
	public static XmlParser sipXmlParser() throws ClassNotFoundException
	{
		XmlParser xmlParser = new WebDescriptor(null).newParser();
		
		URL jsp21xsd = Loader.getResource(Servlet.class, "org/cipango/server/sipapp/jsp_2_1.xsd", true);
        redirect(xmlParser,"jsp_2_1.xsd",jsp21xsd);
		
		URL dtd10 = SipAppContext.class.getResource("/javax/servlet/sip/resources/sip-app_1_0.dtd");
		URL sipapp11xsd = SipAppContext.class.getResource("/javax/servlet/sip/resources/sip-app_1_1.xsd");
        URL javaee5xsd = WebAppContext.class.getResource("/javax/servlet/resources/javaee_5.xsd");
		
		redirect(xmlParser, "-//Java Community Process//DTD SIP Application 1.0//EN", dtd10);
		redirect(xmlParser, "javaee_5.xsd", javaee5xsd);
		redirect(xmlParser, "sip-app_1_1.xsd", sipapp11xsd);
		redirect(xmlParser, "http://www.jcp.org/xml/ns/sipservlet/sip-app_1_1.xsd", sipapp11xsd);
		
		return xmlParser;
	}
	
    protected static void redirect(XmlParser parser, String resource, URL source)
    {
        if (source != null) parser.redirectEntity(resource, source);
    }
	
	public SipXmlProcessor(SipAppContext context) throws ClassNotFoundException
	{
		_context = context;
		_xmlParser = sipXmlParser();
	}

	public void parseDefaults(Resource sipDefaults) throws Exception
	{
		_sipDefaultsRoot = new Descriptor(sipDefaults);
		_sipDefaultsRoot.parse();
	}
	
	public void parseSipXml(Resource sipXml) throws Exception
	{
		_sipXmlRoot = new Descriptor(sipXml);
		_sipXmlRoot.parse();
	
		// TODO class names ?
	}
	
	public void parseSipOverride(Resource override) throws Exception
	{
		_xmlParser.setValidating(false);
		_sipOverrideRoot = new Descriptor(override);
		_sipOverrideRoot.parse();
	}
	
	public void processDefaults() throws Exception
	{
		_sipDefaultsRoot.process();
	}
	
	public void processSipXml() throws Exception
	{
		if (_sipXmlRoot != null)
		{
			_sipXmlRoot.process();
			_context.setSpecVersion(_sipXmlRoot._version);
		}
	}
	
	public void processSipOverride() throws Exception
	{
		_sipOverrideRoot.process();
	}
	
	@SuppressWarnings("unchecked")
	public void process(XmlParser.Node config) throws Exception
	{
		SipServletHandler servletHandler = _context.getServletHandler();
		
		_servlets = LazyList.array2List(servletHandler.getServlets());
		_listeners = LazyList.array2List(_context.getEventListeners());
		
		Iterator<Object> it = config.iterator();
		XmlParser.Node node = null;
		
		while (it.hasNext()) 
		{
			try 
			{
				Object o = it.next();
				if (!(o instanceof XmlParser.Node)) 
					continue;
	
				node = (XmlParser.Node) o;
				
				initSipXmlElement(node);
			} 
			catch (ClassNotFoundException e) 
			{
				throw e;
			} 
			catch (UnavailableException e) 
			{
				throw e;
			}
			catch (Exception e) 
			{
				LOG.warn("Configuration error at " + node, e);
				throw new UnavailableException("Configuration problem: " + e.getMessage() + " at " + node);
			}
		}
		
		initListeners();
		
		servletHandler.setServlets((SipServletHolder[]) LazyList.toArray(_servlets, SipServletHolder.class));
		servletHandler.setServletMappings((SipServletMapping[]) LazyList.toArray(_servletMappings, SipServletMapping.class));
		
		if (_mainServlet != null)
			servletHandler.setMainServletName(_mainServlet);
		
		_context.setName(_appName);
		_context.setEventListeners((EventListener[]) LazyList.toArray(_listeners, EventListener.class));
	}
	
	protected void initSipXmlElement(XmlParser.Node node) throws Exception 
	{
		String name = node.getTag();
	
		if ("app-name".equals(name))
			initAppName(node);
		else if ("display-name".equals(name)) 	
			initDisplayName(node);
		else if ("context-param".equals(name)) 
			initContextParam(node);
		else if ("servlet".equals(name))
			initServlet(node);
		else if ("servlet-mapping".equals(name))
			initServletMapping(node);
		else if ("listener".equals(name))
			initListener(node);
        else if ("proxy-config".equals(name))
            initProxyConfig(node);
        else if("session-config".equals(name))
            initSessionConfig(node);
        else if ("servlet-selection".equals(name))
        	initServletSelection(node);
	}
	
    protected void initSessionConfig(XmlParser.Node node)
    {
        XmlParser.Node tNode=node.get("session-timeout");
        if(tNode!=null)
        {
            int timeout=Integer.parseInt(tNode.toString(false,true));
            _context.setSessionTimeout(timeout);
        }
    }
	
	protected void initDisplayName(XmlParser.Node node) 
	{
		_context.getWebAppContext().setDisplayName(node.toString(false, true));
	}
	
	protected void initContextParam(XmlParser.Node node) 
	{
		String name = node.getString("param-name", false, true);
		String value = node.getString("param-value", false, true);
		_context.getWebAppContext().getInitParams().put(name, value);
	}
	
	protected void initServlet(XmlParser.Node node) throws Exception
	{
		String servletName = node.getString("servlet-name", false, true);
		String servletClass = node.getString("servlet-class", false, true);
		// FIXME allow deploy with prefix: javaee:servlet-name
		SipServletHolder holder = new SipServletHolder();
		holder.setName(servletName);
		holder.setClassName(servletClass);
		
		Iterator params = node.iterator("init-param");
		
		while (params.hasNext()) 
		{
			XmlParser.Node param = (XmlParser.Node) params.next();
			String pName = param.getString("param-name", false, true);
			String pValue = param.getString("param-value", false, true);
			holder.setInitParameter(pName, pValue);
		}
		
		XmlParser.Node startup = node.get("load-on-startup");
		if (startup != null) 
		{
			String s = startup.toString(false, true);
			int order = 0; 
			if (s != null && s.trim().length() > 0) 
			{
				try 
				{
					order = Integer.parseInt(s);
				} 
				catch (NumberFormatException e) 
				{
					LOG.warn("Cannot parse load-on-startup " + s);
				}
			}
			holder.setInitOrder(order);
		}
		_servlets = LazyList.add(_servlets, holder);
	}
	
	protected void initServletMapping(XmlParser.Node node) 
	{
		String servletName = node.getString("servlet-name", false, true);
		SipServletMapping mapping = new SipServletMapping();
		
		XmlParser.Node pattern = node.get("pattern");
		XmlParser.Node start = null;
		Iterator it = pattern.iterator();
		
		while (it.hasNext() && start == null) 
		{
			Object o = it.next();
			if (!(o instanceof XmlParser.Node)) 
				continue;

			start = (XmlParser.Node) o;
		}
		MatchingRule rule = initRule(start);
		mapping.setServletName(servletName);
		mapping.setMatchingRule(rule);
		
		_servletMappings = LazyList.add(_servletMappings, mapping);
	}
	
	private MatchingRule initRule(XmlParser.Node node) 
	{
		String name = node.getTag();
		if ("and".equals(name)) 
		{
			AndRule and = new AndRule();
			Iterator it = node.iterator();
			while (it.hasNext()) 
			{
				Object o = it.next();
				if (!(o instanceof XmlParser.Node)) 
					continue;
				
				and.addCriterion(initRule((XmlParser.Node) o));
			}
			return and;
		} 
		else if ("equal".equals(name)) 
		{
			String var = node.getString("var", false, true);
			String value = node.getString("value", false, true);
			boolean ignoreCase = "true".equalsIgnoreCase(node.getAttribute("ignore-case"));
			return new EqualsRule(var, value, ignoreCase);
		} 
		else if ("subdomain-of".equals(name)) 
		{
			String var = node.getString("var", false, true);
			String value = node.getString("value", false, true);
			return new SubdomainRule(var, value);
		} 
		else if ("or".equals(name)) 
		{
			OrRule or = new OrRule();
			Iterator it = node.iterator();
			while (it.hasNext()) 
			{
				Object o = it.next();
				if (!(o instanceof XmlParser.Node)) 
					continue;

				or.addCriterion(initRule((XmlParser.Node) o));
			}
			return or;
		} 
		else if ("not".equals(name)) 
		{
			NotRule not = new NotRule();
			Iterator it = node.iterator();
			while (it.hasNext()) 
			{
				Object o = it.next();
				if (!(o instanceof XmlParser.Node)) 
					continue;
				
				not.setCriterion(initRule((XmlParser.Node) o));
			}
			return not;
		} 
		else if ("contains".equals(name)) 
		{
			String var = node.getString("var", false, true);
			String value = node.getString("value", false, true);
			boolean ignoreCase = "true".equalsIgnoreCase(node.getAttribute("ignore-case"));
			return new ContainsRule(var, value, ignoreCase);
		} 
		else if ("exists".equals(name)) 
		{
			return new ExistsRule(node.getString("var", false, true));
		} 
		else 
		{
			throw new IllegalArgumentException("Unknown rule: " + name);
		}
	}
	
    public void initProxyConfig(XmlParser.Node node)
    {
        String s = node.getString("proxy-timeout", false, true);
        
        if (s == null)
        	s = node.getString("sequential-search-timeout", false, true);
        
        if (s != null)
        {
            try 
            {
                int timeout = Integer.parseInt(s);
                _context.setProxyTimeout(timeout);
            }
            catch (NumberFormatException e)
            {
                LOG.warn("Invalid sequential-search-timeout value: " + s);
            }
        }
    }
    
	public void initListener(XmlParser.Node node) 
	{
		String className = node.getString("listener-class", false, true);
		_listenerClasses = LazyList.add(_listenerClasses, className);
	}
	
	protected SipServletHolder getServlet(String className)
	{
		for (int i = LazyList.size(_servlets); i-->0;)
		{
			SipServletHolder holder = (SipServletHolder) LazyList.get(_servlets, i);
			if (className.equals(holder.getClassName()))
				return holder;
		}
		return null;
	}
			
	public void initListeners()
	{
		for (int i = 0; i < LazyList.size(_listenerClasses); i++)
		{
			String lc = (String) LazyList.get(_listenerClasses, i);
			SipServletHolder holder = getServlet(lc);
				
			// Check listener has not been already added.
			boolean found = false;
			for (int j = LazyList.size(_listeners); j--> 0;)
			{
				Object listener = LazyList.get(_listeners, j);
				if (listener.getClass().getName().equals(lc))
				{
					LOG.debug("Found multiple listener declaration " +  lc);
					if (holder != null)
						holder.setServlet((SipServlet) listener);
					found = true;
					break;
				}
			}
			
			if (found)
				continue;
			
			try
			{
				Class listenerClass = _context.getWebAppContext().loadClass(lc);
				Object listener = newListenerInstance(listenerClass);
				
				if (holder != null)
					holder.setServlet((SipServlet) listener);
					
				if (!(listener instanceof EventListener))
					LOG.warn("Not an event listener: " + listener);
				else
					_listeners = LazyList.add(_listeners, listener);
			}
			catch (Exception e) 
			{
				LOG.warn("Could not instantiate listener: " + lc, e);
			}
		}
	}
	
	public void initServletSelection(XmlParser.Node node)
	{
		XmlParser.Node mainServlet = node.get("main-servlet");
		if (mainServlet != null)
			_mainServlet = mainServlet.toString(false, true);
		else
		{
			Iterator it = node.iterator("servlet-mapping");
			while (it.hasNext())
			{
				initServletMapping((XmlParser.Node)it.next());
			}
		}
	}
	
	public void initMainServlet(XmlParser.Node node)
	{
		_mainServlet = node.toString(false, true);
	}
	
	public void initAppName(XmlParser.Node node)
	{
		_appName = node.toString(false, true);
	}
	
	protected Object newListenerInstance(Class clazz) 
		throws InstantiationException, IllegalAccessException 
	{	
		return clazz.newInstance();
	}
	
}
