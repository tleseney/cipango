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
package org.cipango.tests.integration;

import static org.cipango.tests.matcher.SipMatchers.hasStatus;
import static org.hamcrest.MatcherAssert.assertThat;

import javax.servlet.sip.SipServletResponse;

import org.cipango.client.Call;
import org.cipango.tests.UaTestCase;
import org.junit.Test;

public class UasTest extends UaTestCase
{

	/**
	 * Test if a CANCEL is received and servlet still locks the session,
	 * the servlet is able to sent a 200 INVITE and is CANCEL is not notified
	 * to application
	 * <pre>
	 * Alice                         AS
	 *   | INVITE                     |
	 *   |--------------------------->|
	 *   |                        180 |
	 *   |<---------------------------|
	 *   | CANCEL                     |
	 *   |--------------------------->|
 	 *   |                 200/INVITE |
	 *   |<---------------------------|
	 *   |                 481/CANCEL |
	 *   |<---------------------------|
	 *   |                        ACK |
	 *   |--------------------------->|
	 *   | BYE                        |
	 *   |--------------------------->|
	 *   |                        200 |
	 *   |<---------------------------|
	 * </pre>
	 */
	@Test
	public void testLateInvite200() throws Exception
	{
		Call call = _ua.createCall(_ua.getFactory().createURI(getTo()));
		assertThat(call.waitForResponse(), hasStatus(SipServletResponse.SC_RINGING));
		call.createCancel().send();
        assertThat(call.waitForResponse(), hasStatus(SipServletResponse.SC_OK));        
        // CANCEL response is filtered by container
        checkForFailure();
	}
	
	/**
	 * Ensure that response bigger than MTU can be sent.
	 */
	@Test
	public void testBigResponse() throws Exception
	{
		sendAndAssertMessage();
	}
	
}
