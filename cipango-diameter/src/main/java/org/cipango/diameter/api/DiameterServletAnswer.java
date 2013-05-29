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

import org.cipango.diameter.ResultCode;

/**
 * Represents Diameter answer. Instances of this class are passed to <code>DiameterListener</code>
 * when the container receives incoming Diameter answers and also, servlets acting as UA servers
 * generates Diameter answers of their own by creating <code>DiameterServletAnswer</code>.
 * 
 */
public interface DiameterServletAnswer extends DiameterServletMessage
{

	/**
	 * Returns the request associated with this answer.
	 * @return the request associated with this answer.
	 */
	public DiameterServletRequest getRequest();
	

	/**
	 * Returns the result code of this answer. 
	 * @return the result code of this answer. 
	 */
	public ResultCode getResultCode();
	
}