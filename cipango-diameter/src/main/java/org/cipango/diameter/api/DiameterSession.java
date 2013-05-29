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

import java.util.Enumeration;

import javax.servlet.sip.SipApplicationSession;

import org.cipango.diameter.ApplicationId;
import org.cipango.diameter.DiameterCommand;
import org.cipango.diameter.base.Common;
import org.cipango.diameter.base.Common.AuthSessionState;

/**
 * Point-to-point Diameter relationship. 
 */
public interface DiameterSession 
{
	
	public SipApplicationSession getApplicationSession();

	/**
	 * Returns a new <code>DiameterRequest</code>.
	 * @param command the command of the new <code>DiameterRequest</code>.
	 * @param maintained if <code>true</code>, add the AVP Auth-Session-State with the value AuthSessionState.STATE_MAINTAINED.
	 * @return a new <code>DiameterRequest</code>.
	 * @throws java.lang.IllegalStateException if this <code>DiameterSession</code> has been invalidated.
	 * @see Common#AUTH_SESSION_STATE
	 * @see AuthSessionState#STATE_MAINTAINED
	 */
	public DiameterServletRequest createRequest(DiameterCommand command, boolean maintained);
	
	/**
	 * Returns the session ID.
	 * @return the session ID.
	 * @see Common#SESSION_ID
	 */
	public String getId();
	
	public ApplicationId getApplicationId();
	
	/**
	 * Return the destination realm associated with this session.
	 * @return the destination realm associated with this session.
	 * @see Common#DESTINATION_REALM
	 */
	public String getDestinationRealm();
	
	/**
	 * Return the destination host associated with this session.
	 * @return the destination host associated with this session.
	 * @see Common#DESTINATION_HOST
	 */
	public String getDestinationHost();
	

	/**
	 * Returns <code>true</code> if this <code>DiameterSession</code> is valid, <code>false</code>
	 * otherwise. The <code>DiameterSession</code> can be invalidated by calling the method
	 * invalidate() on it.
	 * 
	 * @return <code>true</code> if this <code>DiameterSession</code> is valid, <code>false</code>
	 *         otherwise.
	 */
	public boolean isValid();
	
	/**
	 * Invalidates this session and unbinds any objects bound to it.
	 * 
	 * @throws java.lang.IllegalStateException if this method is called on an invalidated session
	 */
	public void invalidate();
		
	/**
	 * Returns the object bound with the specified name in this session, or null if no object is
	 * bound under the name.
	 * 
	 * @param name a string specifying the name of the object
	 * @return the object with the specified name
	 * @throws NullPointerException if the name is null.
	 * @throws IllegalStateException if session is invalidated
	 */
	public Object getAttribute(String name);

	/**
	 * Returns an Enumeration over the <code>String</code> objects containing the names of all the
	 * objects bound to this session.
	 * 
	 * @return Returns an Enumeration over the <code>String</code> objects containing the names of
	 *         all the objects bound to this session.
	 * @throws IllegalStateException if session is invalidated
	 */
	public Enumeration<String> getAttributeNames();
	
	/**
	 * Removes the object bound with the specified name from this session. If the session does not have an object bound with the specified name, this method does nothing. 
	 * @param name the name of the object to remove from this session 
	 * @throws IllegalStateException if session is invalidated
	 */
	public void removeAttribute(String name);

	/**
	 * Binds an object to this session, using the name specified. If an object of the same name is
	 * already bound to the session, the object is replaced.
	 * 
	 * @param name the name to which the object is bound
	 * @param value the object to be bound
	 * @throws IllegalStateException if session is invalidated
	 * @throws NullPointerException on <code>null</code> <code>name</code> or <code>value</code>.
	 */
	public void setAttribute(String name, Object value);
}