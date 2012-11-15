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

package org.cipango.dar;

import javax.servlet.sip.ar.spi.SipApplicationRouterProvider;

import org.cipango.dar.DefaultApplicationRouter;

public class DefaultApplicationRouterProvider extends SipApplicationRouterProvider
{
	public DefaultApplicationRouterProvider() {}
	
	public javax.servlet.sip.ar.SipApplicationRouter getSipApplicationRouter()
	{
		return new DefaultApplicationRouter();
	}
}
