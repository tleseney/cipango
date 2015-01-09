// ========================================================================
// Copyright (c) 2006-2010 Mort Bay Consulting Pty. Ltd.
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

package org.cipango.server.sipapp;

import java.net.URL;

import javax.servlet.Servlet;
import javax.servlet.sip.SipServlet;

import org.eclipse.jetty.util.Loader;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.Descriptor;
import org.eclipse.jetty.webapp.WebDescriptor;
import org.eclipse.jetty.xml.XmlParser;



/**
 * Descriptor
 *
 * A SIP descriptor (sip.xml/sip-defaults.xml/sip-overrides.xml).
 */
public class SipDescriptor extends Descriptor
{ 
    protected static XmlParser _parserSingleton;

    protected int _version;
    protected boolean _distributable;

    
    @Override
    public void ensureParser()
    throws ClassNotFoundException
    {
        if (_parserSingleton == null)
        {
            _parserSingleton = newParser();
        }
        _parser = _parserSingleton;
    }

    
    public XmlParser newParser()
    throws ClassNotFoundException
    {
        XmlParser xmlParser = new WebDescriptor(null).newParser();
        
        URL jsp21xsd = Loader.getResource(Servlet.class, "org/cipango/server/sipapp/jsp_2_1.xsd");
        redirect(xmlParser,"jsp_2_1.xsd",jsp21xsd);
        
        //set up cache of DTDs and schemas locally        
        URL dtd10 = Loader.getResource(SipServlet.class,"javax/servlet/sip/resources/sip-app_1_0.dtd");
		URL sipapp11xsd = Loader.getResource(SipServlet.class,"javax/servlet/sip/resources/sip-app_1_1.xsd");
        URL javaee5xsd = Loader.getResource(Servlet.class, "javax/servlet/resources/javaee_5.xsd");
        
		redirect(xmlParser, "-//Java Community Process//DTD SIP Application 1.0//EN", dtd10);
		redirect(xmlParser, "javaee_5.xsd", javaee5xsd);
		redirect(xmlParser, "sip-app_1_1.xsd", sipapp11xsd);
		redirect(xmlParser, "http://www.jcp.org/xml/ns/sipservlet/sip-app_1_1.xsd", sipapp11xsd);

		return xmlParser;
    }
    
    
    public SipDescriptor (Resource xml)
    {
        super(xml);
    }
    
    @Override
	public void parse () throws Exception
    {
        super.parse();
        processVersion();
    }
        
    
    public void processVersion()
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
			{
				_version = SipAppContext.VERSION_10;
	            /*String dtd=_parser.getDTD();
	            if (dtd!=null && dtd.indexOf("sip-app_1_0")>=0)
	                _version=SipAppContext.VERSION_10;
	            System.out.println("DTD: " + dtd );*/
			}
		}
	}
          
    public void setDistributable (boolean distributable)
    {
        _distributable = distributable;
    }
    
    public boolean isDistributable()
    {
        return _distributable;
    }
    
    public int getVersion()
    {
    	return _version;
    }
}
