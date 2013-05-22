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
package org.cipango.kaleo.integration;

import static org.cipango.client.test.matcher.SipMatchers.hasStatus;
import static org.cipango.client.test.matcher.SipMatchers.isSuccess;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import javax.servlet.sip.SipServletResponse;

import org.cipango.client.Call;
import org.cipango.client.test.TestAgent;
import org.cipango.client.test.UaRunnable;
import org.cipango.client.test.UasScript;

public class ProxyTest extends UaTestCase 
{

	@Override
	protected void setUp() throws Exception
	{
		super.setUp();
		assertThat(getAlice().register(1800), is(true));
		getBob().getContact().setQ(1.0f);
		assertThat(getBob().register(1800), is(true));
	}
	
	@Override
	protected void tearDown() throws Exception
	{
		getAlice().unregister();
		getBob().unregister();
		super.tearDown();
	}
	
	
	/**
	 * <pre>
	 *  Alice        Kaleo       Bob
	 * 1  | INVITE     |          |
	 *    |----------->|          |
	 * 2  |            | INVITE   |
	 *    |            |--------->|
	 * 3  |            |      180 |
	 *    |            |<---------|
	 * 4  |        180 |          |
	 *    |<-----------|          |  
	 * 5  |            |      200 |
	 *    |            |<---------|
	 * 6  |        200 |          |
	 *    |<-----------|          |
	 * 7  | ACK        |          |
	 *    |---------------------->|
	 * 8  | BYE        |          |
	 *    |---------------------->|
	 * 9  |            |      200 |
	 *    |<----------------------|
	 * </pre>
	 */
	public void testSimpleCall() throws Throwable
	{
		UaRunnable callB = new UasScript.RingingOkBye(getBob());
		callB.start();
		Call callA = getAlice().createCall(getBob().getAor().getURI());

		assertThat(callA.waitForResponse(), hasStatus(SipServletResponse.SC_RINGING));
		assertThat(callA.waitForResponse(), hasStatus(SipServletResponse.SC_OK));
		callA.createAck().send();
		callA.createBye().send();

		SipServletResponse response = callA.waitForResponse();
		assertThat(response, isSuccess());
		callB.assertDone();
	}
	
	
//	public void testQ() throws Exception
//	{
//		TestAgent bob2 = newUserAgent("bob");
//		bob2.getContact().setQ(0.7f);
//		bob2.register(1500);
//		
//	}
	
	
	
	public void testNotFound() throws Exception
	{
		Call callA = getAlice().createCall(getAlice().getFactory().createSipURI("notFoud", getDomain()));
		assertThat(callA.waitForResponse(), hasStatus(SipServletResponse.SC_NOT_FOUND));
	}
}
