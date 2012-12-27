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

import javax.servlet.ServletContext;

import org.cipango.server.SipServer;
import org.cipango.server.security.authentication.DigestAuthenticator;
import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.util.security.Constraint;

/* ------------------------------------------------------------ */

public class DefaultAuthenticatorFactory implements SipAuthenticator.Factory
{

	public SipAuthenticator getAuthenticator(SipServer server, ServletContext context,
			org.cipango.server.security.SipAuthenticator.AuthConfiguration configuration,
			IdentityService identityService, LoginService loginService)
	{
		String auth=configuration.getAuthMethod();
		if (Constraint.__DIGEST_AUTH.equalsIgnoreCase(auth))
			return new DigestAuthenticator();
		return null;
	}
   


}
