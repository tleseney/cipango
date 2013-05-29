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
import org.cipango.diameter.base.Common;

/**
 * Factory interface for Diameter abstractions. An instance this class is available to applications
 * through the <code>org.cipango.diameter.DiameterFactory</code> attribute of
 * {@link javax.servlet.ServletContext}
 */
public interface DiameterFactory
{

	/**
	 * Returns a new diameter request object with the specified command, application identifier, and
	 * destination realm. The returned request object exists in a new <code>DiameterSession</code>
	 * which belongs to the specified <code>SipApplicationSession</code>.
	 * 
	 * The container is responsible for assigning the request appropriate origin host, origin realm,
	 * session id AVPs.
	 * 
	 * @param appSession the application session to which the new <code>DiameterSession</code> and
	 *            <code>DiameterRequest</code> belongs
	 * @param id diameter application identifier
	 * @param command diameter command
	 * @param destinationRealm value of destination realm AVP
	 * @return a new diameter request object with the specified command, application identifier, and
	 *         destination realm.
	 * @see Common#DESTINATION_REALM
	 */
	public DiameterServletRequest createRequest(SipApplicationSession appSession, ApplicationId id,
			DiameterCommand command, String destinationRealm);

	/**
	 * Returns a new diameter request object with the specified command, application identifier,
	 * destination realm and destination host. The returned request object exists in a new
	 * <code>DiameterSession</code> which belongs to the specified
	 * <code>SipApplicationSession</code>.
	 * 
	 * The container is responsible for assigning the request appropriate origin host, origin realm,
	 * session id AVPs.
	 * 
	 * @param appSession the application session to which the new <code>DiameterSession</code> and
	 *            <code>DiameterRequest</code> belongs
	 * @param id diameter application identifier
	 * @param command diameter command
	 * @param destinationRealm value of destination realm AVP
	 * @param destinationHost value of destination host AVP
	 * @return a new diameter request object with the specified command, application identifier, and
	 *         destination realm.
	 * @see Common#DESTINATION_REALM
	 * @see Common#DESTINATION_HOST
	 */
	public DiameterServletRequest createRequest(SipApplicationSession appSession, ApplicationId id,
			DiameterCommand command, String destinationRealm, String destinationHost);

}
