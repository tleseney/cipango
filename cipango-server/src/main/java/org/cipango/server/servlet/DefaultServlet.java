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
		request.createResponse(200).send();
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
}
