package org.cipango.server;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;

import javax.management.MBeanServer;
import javax.servlet.ServletException;
import javax.servlet.sip.ServletTimer;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.TimerListener;
import javax.servlet.sip.TimerService;

import org.cipango.server.nio.UdpConnector;
import org.cipango.server.servlet.SipServletHolder;
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
		Server server = new Server();
		WebAppContext webAppContext = new WebAppContext();
		webAppContext.setConfigurationClasses(new String[0]);
		webAppContext.setServer(server);
		context.setWebAppContext(webAppContext);
		server.setHandler(webAppContext);
		server.addBean(sipServer);
		
		context.getSessionHandler().getSessionManager().setSessionTimeout(1);
		context.setName("Default");
		TestServlet testServlet = new TestServlet();
		SipServletHolder holder = new SipServletHolder();
		holder.setServlet(testServlet);
		holder.setInitOrder(1);
		context.getSipServletHandler().addServlet(holder);
		context.addEventListener(testServlet);
		
		sipServer.setHandler(context);
		server.start();
		
	}
	

	static class TestServlet extends SipServlet implements TimerListener
	{
		private TimerService _timerService;
		@Override
		public void init()
		{
			_timerService = (TimerService) getServletContext().getAttribute(TIMER_SERVICE);
		}
		
		protected void doRequest(SipServletRequest request) throws ServletException, IOException
		{
			try
			{
				super.doRequest(request);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		
		protected void doInvite(SipServletRequest request) throws ServletException, IOException
		{
			request.getApplicationSession().setAttribute(SipServletRequest.class.getName(), request);
			_timerService.createTimer(request.getApplicationSession(), 2000, false, request.getSession().getId());
			request.createResponse(SipServletResponse.SC_RINGING).send();
		}
		
		@Override
		protected void doRegister(SipServletRequest request) throws ServletException, IOException
		{
			SipServletResponse response = request.createResponse(200);
			response.send();
		}
		
		@Override
		protected void doMessage(SipServletRequest request) throws ServletException, IOException
		{
			SipServletResponse response = request.createResponse(200);
			response.send();
		}
		
		@Override
		protected void doOptions(SipServletRequest request) throws ServletException, IOException
		{
			SipServletResponse response = request.createResponse(200);
			response.send();
		}
		
		@Override
		protected void doBye(SipServletRequest request) throws ServletException, IOException
		{
			request.createResponse(200).send();
			request.getApplicationSession().invalidate();
		}

		@Override
		protected void doResponse(SipServletResponse response) throws ServletException, IOException
		{
		}

		@Override
		public void timeout(ServletTimer timer)
		{
			SipServletRequest request = (SipServletRequest) timer.getApplicationSession().getAttribute(SipServletRequest.class.getName());
			try
			{
				request.createResponse(200).send();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}

	}
}
