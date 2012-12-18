// ========================================================================
// Copyright 2008-2010 NEXCOM Systems
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
package org.cipango.test;

import java.io.*;
import java.util.List;

import javax.servlet.*;
import javax.servlet.sip.*;

public class B2bServlet extends SipServlet
{

	@Override
	protected void doRequest(SipServletRequest request) throws ServletException, IOException
	{
		B2buaHelper helper = request.getB2buaHelper();
		SipServletRequest newRequest;
		if (request.isInitial())
		{
			newRequest = helper.createRequest(request, true, null);
			newRequest.getSession().setHandler(getServletName());
		}
		else
		{
			SipSession session = helper.getLinkedSession(request.getSession());
			if ("ACK".equals(request.getMethod()))
			{
				List<SipServletMessage> l = helper.getPendingMessages(session, UAMode.UAC);
				SipServletResponse response = (SipServletResponse) l.get(0);
				newRequest = response.createAck();
			}
			else if ("CANCEL".equals(request.getMethod()))
			{
				newRequest = helper.createCancel(session);
			}
			else
			{
				newRequest = helper.createRequest(session, request, null);
			}
		}

		newRequest.send();
	}



	protected void doResponse(SipServletResponse response)
	{
		try
		{
			B2buaHelper helper = response.getRequest().getB2buaHelper();
			SipServletRequest linkedRequest = helper
					.getLinkedSipServletRequest(response.getRequest());
			SipServletResponse newResponse = linkedRequest.createResponse(
					response.getStatus(), response.getReasonPhrase());
			newResponse.send();
		}
		catch (Throwable e)
		{
			log("Failed to handle:\n" + response, e);
		}
	}

}
