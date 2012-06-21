package org.cipango.annotations.resources;

import javax.annotation.Resource;
import javax.servlet.sip.ServletTimer;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipSessionsUtil;
import javax.servlet.sip.TimerListener;
import javax.servlet.sip.TimerService;
import javax.servlet.sip.annotation.SipApplicationKey;
import javax.servlet.sip.annotation.SipListener;

@javax.servlet.sip.annotation.SipServlet
@SipListener
public class AnnotedServlet extends SipServlet implements TimerListener
{
	@Resource
	public SipFactory sipFactory;
	
	@Resource
	public TimerService timerService;
	
	@Resource
	public SipSessionsUtil sessionsUtil;
	
	public void timeout(ServletTimer arg0)
	{
		
	}
	
	@SipApplicationKey
	public static String getSessionKey(SipServletRequest request)
	{
		return request.getCallId();
	}

}
