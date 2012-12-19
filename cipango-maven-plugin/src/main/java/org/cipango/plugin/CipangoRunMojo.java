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
package org.cipango.plugin;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.cipango.server.AbstractSipConnector;
import org.cipango.server.SipConnector;
import org.cipango.server.SipServer;
import org.cipango.server.Transport;
import org.cipango.server.log.AccessLog;
import org.cipango.server.log.FileMessageLog;
import org.cipango.server.nio.SelectChannelConnector;
import org.cipango.server.nio.UdpConnector;
import org.cipango.server.sipapp.SipAppContext;
import org.cipango.server.sipapp.SipXmlConfiguration;
import org.eclipse.jetty.maven.plugin.JettyRunMojo;
import org.eclipse.jetty.maven.plugin.JettyWebAppContext;
import org.eclipse.jetty.util.ArrayUtil;
import org.eclipse.jetty.webapp.Configuration;

/**
 *  <p>
 *  This goal is used in-situ on a Maven project without first requiring that the project 
 *  is assembled into a war, saving time during the development cycle.
 *  The plugin forks a parallel lifecycle to ensure that the "compile" phase has been completed before invoking Jetty. This means
 *  that you do not need to explicity execute a "mvn compile" first. It also means that a "mvn clean jetty:run" will ensure that
 *  a full fresh compile is done before invoking Jetty.
 *  </p>
 *  <p>
 *  Once invoked, the plugin can be configured to run continuously, scanning for changes in the project and automatically performing a 
 *  hot redeploy when necessary. This allows the developer to concentrate on coding changes to the project using their IDE of choice and have those changes
 *  immediately and transparently reflected in the running web container, eliminating development time that is wasted on rebuilding, reassembling and redeploying.
 *  </p>
 *  <p>
 *  You may also specify the location of a jetty.xml file whose contents will be applied before any plugin configuration.
 *  This can be used, for example, to deploy a static webapp that is not part of your maven build. 
 *  </p>
 *  <p>
 *  There is a <a href="run-mojo.html">reference guide</a> to the configuration parameters for this plugin, and more detailed information
 *  with examples in the <a href="http://docs.codehaus.org/display/JETTY/Maven+Jetty+Plugin">Configuration Guide</a>.
 *  </p>
 * 
 * @goal run
 * @requiresDependencyResolution test
 * @execute phase="test-compile"
 * @description Runs Cipango directly from a maven project
 */
public class CipangoRunMojo extends JettyRunMojo
{
	
	public static final String SIP_PORT_PROPERTY = "sip.port";
	public static final String SIP_HOST_PROPERTY = "sip.host";
		
    /**
     * List of sip connectors to use. If none are configured
     * then UDP and TCP connectors at port 5060 and on first public address. 
     * 
     * You can override this default port number  and host by using the system properties
     *  {{sip.port}} and {{sip.host}} on the command line, eg:  
     *  {{mvn -Dsip.port=9999 -Dsip.host=localhost cipango:run}}.
     * 
     * @parameter 
     */
    private SipConnector[] sipConnectors;
    
    
    /**
     * The sip messages logger to use.
     * If none are configured, then a file message logger is created in the directory
     * <code>target/logs</code>. 
     * @parameter 
     */
    private AccessLog messageLog;
    
    /**
     * A sipdefault.xml file to use instead
     * of the default for the sipapp. Optional.
     *
     * @parameter
     */
    protected File sipDefaultXml;
    
    /**
     * A sip.xml file to be applied AFTER the webapp's sip.xml file. Useful for
     * applying different build profiles, eg test, production etc. Optional.
     * @parameter
     */
    protected File overrideSipXml;
    
    /**
     * Allow to disable annotations parsing.
     * @parameter default-value="true"
     */
    protected boolean annotationsEnabled;
    
       
    /**
     * An instance of org.cipango.server.sipapp.SipAppContext that represents the sipapp.
     * Use any of its setters to configure the sipapp.
     * 
     * @parameter alias="sipAppConfig"
     */
    protected SipAppContext sipApp;
    
    /**
     * A wrapper for the Server object
     */
    private SipServer sipServer = CipangoSipServer.getInstance();
                
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        super.execute();
    }


	@Override
	public void finishConfigurationBeforeStart() throws Exception
	{
		super.finishConfigurationBeforeStart();
		sipServer.setHandler(sipApp);
        sipServer.setConnectors(sipConnectors);
        SipConnector[] connectors = sipServer.getConnectors();

        if (connectors == null|| connectors.length == 0)
        {
            //if a SystemProperty -Dsip.port=<portnum> has been supplied, use that as the default port
        	String portnum = System.getProperty(SIP_PORT_PROPERTY, null);
        	String host = System.getProperty(SIP_HOST_PROPERTY, null);

    		AbstractSipConnector[] sipConnectors = new AbstractSipConnector[2];
    		int port = ((portnum==null||portnum.equals(""))?Transport.TCP.getDefaultPort():Integer.parseInt(portnum.trim()));
    		sipConnectors[0] = new UdpConnector(sipServer);
    		sipConnectors[1] = new SelectChannelConnector(sipServer);
    		if (host != null && !host.trim().equals(""))
    		{
    			sipConnectors[0].setHost(host);
    			sipConnectors[1].setHost(host);
    		}
    		sipConnectors[0].setPort(port);
    		sipConnectors[1].setPort(port);

        	sipServer.setConnectors(sipConnectors);
        }
        
		if (messageLog == null)
		{
            FileMessageLog log = new FileMessageLog();
            log.setFilename(project.getBuild().getDirectory() + "/logs/yyyy_mm_dd.message.log");
            messageLog = log;
		}
        if (sipServer.getAccessLog() == null)
        	sipServer.setAccessLog(messageLog);  
        
	}
	
	private boolean isSipConfigSet()
	{
		Configuration[] configs = webApp.getConfigurations();
		if (configs != null)
		{
			for (Configuration c : configs)
				if (c instanceof SipXmlConfiguration || c instanceof MavenAnnotationConfiguration)
					return true;
			return false;
		}
		
		return true;
	}

	@Override
	public void configureWebApplication () throws Exception
	{
		if (sipApp == null)
			sipApp = new SipAppContext();
		if (webApp == null)
			webApp = new JettyWebAppContext();
		sipApp.setWebAppContext(webApp, true);
		sipApp.setServer(sipServer);
		
		if (!isSipConfigSet())
		{
			webApp.setConfigurations(ArrayUtil.addToArray(webApp.getConfigurations(), new SipXmlConfiguration(), Configuration.class));
			if (annotationsEnabled)
				webApp.setConfigurations(ArrayUtil.addToArray(webApp.getConfigurations(), new MavenAnnotationConfiguration(), Configuration.class));
		}
		
		super.configureWebApplication();
		
		if (sipDefaultXml != null)
            sipApp.setDefaultsDescriptor(sipDefaultXml.getCanonicalPath());
// FIXME
		//        if (overrideSipXml != null)
//        	sipApp.setOverrideSipDescriptor(overrideSipXml.getCanonicalPath());
//        sipApp.setAnnotationsEnabled(annotationsEnabled);
        
        getLog().info("Sip defaults = "+(sipApp.getDefaultsDescriptor()==null?" cipango default":sipApp.getDefaultsDescriptor()));
//        getLog().info("Sip overrides = "+(webApp.getOverrideSipDescriptor()==null?" none":webApp.getOverrideSipDescriptor()));

	}
    
    
}
