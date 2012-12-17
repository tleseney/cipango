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

package org.cipango.server.session.http;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.sip.ConvergedHttpSession;
import javax.servlet.sip.SipApplicationSession;

import org.cipango.server.session.ApplicationSession;
import org.cipango.server.session.SessionHandler;
import org.cipango.server.session.SessionManager.ApplicationSessionScope;
import org.cipango.server.session.scoped.ScopedAppSession;
import org.cipango.server.sipapp.SipAppContext;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.session.AbstractSession;
import org.eclipse.jetty.server.session.HashSessionManager;
import org.eclipse.jetty.server.session.HashedSession;
import org.eclipse.jetty.util.URIUtil;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class ConvergedSessionManager extends HashSessionManager
{
	private static final Logger LOG = Log.getLogger(ConvergedSessionManager.class);
	
	public class Session extends HashedSession implements ConvergedHttpSession
	{
		private ApplicationSession _appSession;
		private String _serverName;
		private String _scheme;
		private int _port;
		private int _securePort;
		
		protected Session(HttpServletRequest httpServletRequest)
        {
           super(ConvergedSessionManager.this, httpServletRequest);
           Request request = (Request) httpServletRequest;
           _serverName = request.getServerName();
           _scheme = request.getScheme();
           _securePort = ((Request) request).getHttpChannel().getHttpConfiguration().getSecurePort();
           _port = request.getServerPort();
           updateSession(request);
           
        }
		
		public void updateSession(HttpServletRequest request)
		{
			if (_appSession != null)
				return;
			
			String uri = request.getRequestURI();
            int semi = uri.lastIndexOf(';');
            if (semi == -1)
            	return;
            
			String appId = null;
            String path_params=uri.substring(semi+1);
            
            if (path_params!=null && path_params.startsWith(SessionHandler.APP_ID))
            {
            	appId = path_params.substring(SessionHandler.APP_ID.length() + 1);
                if(LOG.isDebugEnabled())
                	LOG.debug("Got App ID " + appId + " from URL");
            }

			
			if (appId != null && !appId.trim().equals("")) 
			{
				appId = appId.replace("%3B", ";");
				_appSession = getSipAppContext().getSessionHandler().getSessionManager().getApplicationSession(appId);
				if (_appSession != null && isValid())
					_appSession.addSession(this);
			}
		}
		
		private SipAppContext getSipAppContext()
		{
			return _context.getContextHandler().getBean(SipAppContext.class);
		}

		public String encodeURL(String url)
		{
			String sessionURLPrefix = getSessionIdPathParameterNamePrefix();
			String id= getNodeId();
			int prefix=url.indexOf(sessionURLPrefix);
	        if (prefix!=-1)
	        {
	            int suffix=url.indexOf("?",prefix);
	            if (suffix<0)
	                suffix=url.indexOf("#",prefix);

	            if (suffix<=prefix)
	                return url.substring(0, prefix + sessionURLPrefix.length()) + id;
	            return url.substring(0, prefix + sessionURLPrefix.length()) + id + url.substring(suffix);
	        }

	        // edit the session
	        int suffix=url.indexOf('?');
	        if (suffix<0)
	            suffix=url.indexOf('#');
	        if (suffix<0)
	            return url+sessionURLPrefix+id;
	        return url.substring(0,suffix)+
	            sessionURLPrefix+id+url.substring(suffix);
		}

		public String encodeURL(String relativePath, String scheme)
		{
			StringBuffer sb = new StringBuffer();
			sb.append(scheme).append("://");
			sb.append(_serverName);
			if (_scheme.equalsIgnoreCase(scheme))
			{
				if (_port>0 && 
		                ((scheme.equalsIgnoreCase(URIUtil.HTTP) && _port != 80) || 
		                 (scheme.equalsIgnoreCase(URIUtil.HTTPS) && _port != 443)))
	                sb.append(':').append(_port);
			} 
			else if (URIUtil.HTTPS.equalsIgnoreCase(scheme) && _securePort != 0)
			{
				if (_securePort != 443)
	                sb.append(':').append(_securePort);
			}
			else
			{
				throw new IllegalArgumentException("Scheme " + scheme + " is not the scheme used for this session "
						+ " and unable to detect the port for this scheme");
			}
			sb.append(_context.getContextPath());
			sb.append(relativePath);
			return encodeURL(sb.toString());
		}

		public SipApplicationSession getApplicationSession()
		{
			if (_appSession == null)
			{
				_appSession = getSipAppContext().getSessionHandler().getSessionManager().createApplicationSession();
				if (isValid())
					_appSession.addSession(this);
			}
			return new ScopedAppSession(_appSession);
		}
		
		@Override
		protected boolean access(long time) 
		{
			boolean access = super.access(time);
			if (_appSession != null)
			{
				ApplicationSessionScope scope = _appSession.getSessionManager().openScope(_appSession);
				try
				{
					_appSession.access(time);
				}
				finally
				{
					scope.close();
				}
			}
			return access;
		}
		
		@Override
		 protected void doInvalidate() throws IllegalStateException
		{
			 super.doInvalidate();
			 if (_appSession != null)
				 _appSession.removeSession(this);
		}
	}
	
	@Override
	protected AbstractSession newSession(HttpServletRequest request)
	{
		return new Session(request);
	}

}
