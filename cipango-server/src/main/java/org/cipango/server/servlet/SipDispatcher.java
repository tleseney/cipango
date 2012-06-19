// ========================================================================
// Copyright 2008-2012 NEXCOM Systems
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

package org.cipango.server.servlet;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.cipango.server.SipMessage;
import org.cipango.server.sipapp.SipAppContext;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class SipDispatcher implements RequestDispatcher
{
	private static final Logger LOG = Log.getLogger(SipDispatcher.class);
	
    private SipServletHolder _holder;
    private SipAppContext _context;
    
    public SipDispatcher(SipAppContext context, SipServletHolder holder)
    {
    	_context = context;
        _holder = holder;
    }
    
    /** 
     * @see javax.servlet.RequestDispatcher#forward(javax.servlet.ServletRequest, javax.servlet.ServletResponse)
     */
    public void forward(ServletRequest request, ServletResponse response) throws ServletException, IOException
    {
		if (LOG.isDebugEnabled())
			LOG.debug("Forwarding to handler: " + _holder.getName());

		// TODO forward
//		SipMessage message = request != null ? (SipMessage) request : (SipMessage) response;
//		message.setHandler(_holder);
//		_context.handle(message);
//		message.setHandler(null);
    }
    
    /**
     * @see javax.servlet.RequestDispatcher#include(javax.servlet.ServletRequest, javax.servlet.ServletResponse)
     */
    public void include(ServletRequest request, ServletResponse response) throws ServletException, IOException
    {
        throw new UnsupportedOperationException("RequestDispatcher.include(ServletRequest, ServletResponse) is not supported in SIP Servlet API");
    }
}
