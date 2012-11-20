// ========================================================================
// Copyright 2011 NEXCOM Systems
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

package org.cipango.client;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.sip.SipServletResponse;

/**
 * A specialized {@link MessageHandler} with seamless authentication handling.
 */
public interface ChallengedMessageHandler extends MessageHandler 
{
	/**
	 * Handles responses containing a challenge.
	 * 
	 * @param response
	 * @return <code>true</code> if the message should be forwarded up to this
	 *         <code>MessageHandler</code> user, <code>false</code> otherwise.
	 * @throws IOException
	 * @throws ServletException
	 */
	boolean handleAuthentication(SipServletResponse response) throws IOException, ServletException;
}
