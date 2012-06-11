package org.cipango.server.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

public class DefaultServlet extends SipServlet
{
	@Override
	public void init()
	{
		System.out.println("servlet initialized");
	}
	
	@Override
	protected void doOptions(SipServletRequest request) throws ServletException, IOException
	{
		System.out.println(request.getTransport() + "/" + request.getRemoteAddr() + ":" + request.getRemotePort());
		SipServletResponse response = request.createResponse(200);
		response.addHeader("Server", getServletConfig().getServletName());
		response.send();
	}
}
