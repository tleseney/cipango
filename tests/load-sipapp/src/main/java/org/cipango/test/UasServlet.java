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

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;


public class UasServlet extends SipServlet {

		
	protected void doRequest(SipServletRequest req1) throws ServletException,
	IOException {
	if (!"ACK".equals(req1.getMethod())) {
		
		SipServletResponse resp = req1.createResponse(200);
		resp.send();
		if (!"INVITE".equals(req1.getMethod())) 
			req1.getApplicationSession().invalidate();
	}
}


}
