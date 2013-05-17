// ========================================================================
// Copyright 2008-2009 NEXCOM Systems
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

package org.cipango.kaleo.sip;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.cipango.kaleo.Constants;
import org.cipango.kaleo.location.event.RegEventPackage;
import org.cipango.kaleo.presence.PresenceEventPackage;
import org.cipango.kaleo.presence.watcherinfo.WatcherInfoEventPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main Kaleo servlet
 */
public class KaleoServlet extends SipServlet
{
	private static final long serialVersionUID = 1L;
	
	private final Logger _log = LoggerFactory.getLogger(KaleoServlet.class);

	protected void doRequest(SipServletRequest request) // throws ServletException, IOException
	{
		_log.info("handling request: {}", request.getMethod() + " " + request.getRequestURI());
		try
		{
			super.doRequest(request);
		} 
		catch (Exception e)
		{
			_log.warn("Failed to handle request: \n" + request, e);
		}
	}
	
	protected void doRegister(SipServletRequest register) throws ServletException, IOException
	{
		getServletContext().getNamedDispatcher("registrar").forward(register, null);
	}
	
	protected void doPublish(SipServletRequest publish) throws ServletException, IOException
	{
		getServletContext().getNamedDispatcher("presence").forward(publish, null);
	}
	
	protected void doSubscribe(SipServletRequest subscribe) throws ServletException, IOException
	{
		String event = subscribe.getHeader(Constants.EVENT);
		if (RegEventPackage.NAME.equals(event))
			getServletContext().getNamedDispatcher("registrar").forward(subscribe, null);
		else if (PresenceEventPackage.NAME.equals(event)
				|| WatcherInfoEventPackage.NAME.equals(event))
			getServletContext().getNamedDispatcher("presence").forward(subscribe, null);
		else
		{
			SipServletResponse response = subscribe.createResponse(SipServletResponse.SC_BAD_EVENT);
			response.addHeader(Constants.ALLOW_EVENTS, RegEventPackage.NAME);
			response.addHeader(Constants.ALLOW_EVENTS, PresenceEventPackage.NAME);
			response.addHeader(Constants.ALLOW_EVENTS, WatcherInfoEventPackage.NAME);
			response.send();
			response.getApplicationSession().invalidate();
		}
	}
	
	protected void doInvite(SipServletRequest invite) throws ServletException, IOException
	{
		getServletContext().getNamedDispatcher("proxy").forward(invite, null);
	}
}
