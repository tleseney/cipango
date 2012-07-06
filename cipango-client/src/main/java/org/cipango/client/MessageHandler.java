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
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

/**
 * MessageHandler is the minimal API to be implemented by an object which is to
 * be aware of SIP messages. It is more efficiently used in conjunction with
 * {@link SipClient}, in which case an object implementing this interface must
 * be stored as the attribute named <code>MessageHandler.class.getName()</code>
 * either in the sent requests, or in the established SIP sessions.
 * <p>
 * When handling SIP messages, {@link SipClient} first checks for a
 * MessageHandler instance in the received request (or in the request associated
 * to the received response), and then in the message's session if unsuccessful.
 * Received initial requests, which normally do not have any handler defined,
 * are handled directly by the concerned {@link UserAgent} if any. If no handler
 * or user agent can be found, messages are dropped.
 */
public interface MessageHandler 
{
	void handleRequest(SipServletRequest request) throws IOException, ServletException;

	void handleResponse(SipServletResponse response) throws IOException, ServletException;
}
