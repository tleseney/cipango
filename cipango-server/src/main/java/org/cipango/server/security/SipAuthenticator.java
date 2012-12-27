// ========================================================================
// Copyright 2011-2012 NEXCOM Systems
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
package org.cipango.server.security;

import java.util.Set;

import javax.servlet.ServletContext;

import org.cipango.server.SipRequest;
import org.cipango.server.SipServer;
import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;

@ManagedObject
public interface SipAuthenticator
{
	
	public UserIdentity authenticate(SipRequest request, boolean proxyMode, boolean authMandatory);
	
	public void setAuthConfiguration(AuthConfiguration configuration);
	
	@ManagedAttribute("Authentication method")
	public String getAuthMethod();
	
	interface AuthConfiguration
    {
        String getAuthMethod();
        String getRealmName();
        
        /** Get a SecurityHandler init parameter
         * @see SecurityHandler#getInitParameter(String)
         * @param param parameter name
         * @return Parameter value or null
         */
        String getInitParameter(String param);
        
        /* ------------------------------------------------------------ */
        /** Get a SecurityHandler init parameter names
         * @see SecurityHandler#getInitParameterNames()
         * @return Set of parameter names
         */
        Set<String> getInitParameterNames();
        
        LoginService getLoginService();
        IdentityService getIdentityService();   
    }
	
    interface Factory
    {
        SipAuthenticator getAuthenticator(SipServer server, ServletContext context, AuthConfiguration configuration, IdentityService identityService, LoginService loginService);
    }
}
