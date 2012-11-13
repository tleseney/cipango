package org.cipango.server;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;

import javax.management.MBeanServer;

import org.cipango.server.nio.UdpConnector;
import org.cipango.server.servlet.DefaultServlet;
import org.cipango.server.sipapp.SipAppContext;
import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;


public class Main 
{
	public static int hashCode(String s) 
	{
		int hash = 5381;

		char[] a = s.toCharArray();
		int i = 0;
		for (; i<a.length; i++) {
			hash = ((hash << 5) + hash) + a[i]; /* hash * 33 + c */
		}
		
		return hash & 0x7fffffff;
	}
	
	public static int hashCode2(String s) 
	{
		int hash = 5381;


		byte[] a = s.getBytes();
		int i = 0;
		for (; i<a.length; i++) {
			hash = ((hash << 5) + hash) + a[i]; /* hash * 33 + c */
		}
	
		return hash;
	}
	
	public static void main(String[] args) throws Exception
	{			
		SipServer sipServer = new SipServer();
		
		boolean jmx = true;
		if (jmx)
		{
			MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
			MBeanContainer container = new MBeanContainer(mBeanServer);
			sipServer.addBean(container);
			sipServer.addBean("org.eclipse.jetty.util.log.Log");
		}
		
		UdpConnector connector = new UdpConnector(sipServer);
		connector.setHost(InetAddress.getLocalHost().getHostName());
		
		sipServer.addConnector(connector);
		
		SipAppContext context = new SipAppContext();
		context.getSessionHandler().getSessionManager().setSessionTimeout(1);
		context.setName("Default");
		context.getSipServletHandler().addServlet(DefaultServlet.class.getName());
		
		sipServer.setHandler(context);
		
		sipServer.start();
	}
	

}
