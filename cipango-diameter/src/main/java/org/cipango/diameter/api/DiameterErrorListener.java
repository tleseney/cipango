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

import java.util.EventListener;

/**
 * Causes applications to be notified of various error conditions occurring during regular Diameter
 * message processing.
 */
public interface DiameterErrorListener extends EventListener
{

	/**
	 * Invoked by the container to notify an application that no answer was received for a diameter
	 * request.
	 * 
	 * @param e the event that identifies the request.
	 */
	public void noAnswerReceived(DiameterErrorEvent e);
}
