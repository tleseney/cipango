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

import java.util.EventObject;

/**
 * Events of this type are sent to objects implementing the <code>DiameterErrorListener</code>
 * interface when an error occurs which is related to the Diameter request processing.
 */
public class DiameterErrorEvent extends EventObject
{
	private static final long serialVersionUID = 1L;
	
	private DiameterServletRequest _request;
	private long _timeout;

	public DiameterErrorEvent(DiameterServletRequest source, long timeout)
	{
		super(source);
		_request = source;
		_timeout = timeout;
	}

	/**
	 * Returns the Diameter request associated with <code>DiameterErrorEvent</code>.
	 * 
	 * @return the Diameter request associated with <code>DiameterErrorEvent</code>.
	 */
	public DiameterServletRequest getRequest()
	{
		return _request;
	}
	
	/**
	 * Return the timeout in milliseconds that throws this event.
	 * @return the timeout in milliseconds that throws this event.
	 */
	public long timeout()
	{
		return _timeout;
	}

}
