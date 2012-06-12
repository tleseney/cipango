package org.cipango.server;

import org.cipango.server.nio.UdpConnector;
import org.cipango.server.servlet.DefaultServlet;
import org.cipango.server.sipapp.SipAppContext;


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
		
		UdpConnector connector = new UdpConnector();
		connector.setHost("192.168.2.127");
		
		sipServer.addConnector(connector);
		
		SipAppContext context = new SipAppContext();
		context.getSipServletHandler().addSipServlet(DefaultServlet.class.getName());
		
		sipServer.setHandler(context);
		
		sipServer.start();
	}
}
