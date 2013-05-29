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

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.sip.SipApplicationSession;

import org.cipango.diameter.AVPList;
import org.cipango.diameter.DiameterCommand;
import org.cipango.diameter.Type;

/**
 * Base class for diameter requests and answers.
 */
public interface DiameterServletMessage
{
	public <T> T get(Type<T> type);
	
	public <T> void add(Type<T> type, T value);

	public int getApplicationId();

	public int getHopByHopId();

	public int getEndToEndId();

	public DiameterCommand getCommand();

	public String getOriginHost();

	public String getOriginRealm();

	public String getSessionId();
	
	public int size();

	/**
	 * Returns a list with all messages AVPs.
	 * @return a list with all messages AVPs.
	 */
	public AVPList getAVPs();

	public void setAVPList(AVPList avps);

	/**
	 * Returns the <code>DiameterSession</code> to which this message belongs. If the session didn't
	 * already exist it is created. This method is equivalent to calling <code>getSession(true)</code>.
	 * 
	 * @return the <code>DiameterSession</code> to which this message belongs.
	 */
	public DiameterSession getSession();

	/**
	 * Returns the <code>DiameterSession</code> to which this message belongs.
	 * 
	 * @param create indicates whether the session is created if it doesn't already exist
	 * @return the <code>DiameterSession</code> to which this message belongs , or <code>null</code>
	 *         if one hasn't been created and <code>create</code> is false
	 */
	public DiameterSession getSession(boolean create);

	public SipApplicationSession getApplicationSession();

	public boolean isRequest();

	public void send() throws IOException;

	/**
	 * Returns the value of the named attribute as an Object, or null if no attribute of the given
	 * name exists.
	 * 
	 * @param name a String specifying the name of the attribute
	 * @return an Object containing the value of the attribute, or null if the attribute does not
	 *         exist
	 */
	public Object getAttribute(String name);

	/**
	 * Removes the named attribute from this message. Nothing is done if the message did not already
	 * contain the specified attribute.
	 * 
	 * @param name a String specifying the name of the attribute
	 */
	public void removeAttribute(String name);

	/**
	 * Returns an Enumeration containing the names of the attributes available to this message
	 * object. This method returns an empty Enumeration if the message has no attributes available
	 * to it.
	 * 
	 * @return an Enumeration of strings containing the names of the message's attributes
	 */
	public Enumeration<String> getAttributeNames();

	/**
	 * Stores an attribute in this message.
	 * @param name a String specifying the name of the attribute
	 * @param o the Object to be stored 
	 * @throws NullPointerException if either of name or o is null.
	 */
	public void setAttribute(String name, Object o);

}