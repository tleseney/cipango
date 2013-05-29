// ========================================================================
// Copyright 2006-2013 NEXCOM Systems
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

package org.cipango.diameter.api;

import javax.servlet.sip.SipApplicationSession;

import org.cipango.diameter.ApplicationId;
import org.cipango.diameter.DiameterCommand;
import org.cipango.diameter.ResultCode;
import org.cipango.diameter.base.Common;

/**
 * Represents Diameter request. When receiving an incoming Diameter request the container creates a
 * <code>DiameterServletRequest</code> and passes it to the handling <code>DiameterListener</code>.
 * For outgoing, locally initiated requests, applications call
 * {@linkplain DiameterFactory#createRequest(SipApplicationSession, ApplicationId, DiameterCommand, String)
 * DiameterFactory.createRequest} to obtain a <code>DiameterServletRequest</code> that can then be
 * modified and sent.
 */
public interface DiameterServletRequest extends DiameterServletMessage
{

	/**
	 * Return the content of the destination realm AVP.
	 * @return the content of the destination realm AVP.
	 * @see Common#DESTINATION_REALM
	 */
	public String getDestinationRealm();

	/**
	 * Return the content of the destination host AVP.
	 * @return the content of the destination host AVP.
	 * @see Common#DESTINATION_HOST
	 */
	public String getDestinationHost();

	/**
	 * Creates an answer for this request with the specified result code. 
	 * @param resultCode the answer result code.
	 * @return an answer for this request with the specified result code. 
	 */
	public DiameterServletAnswer createAnswer(ResultCode resultCode);

}