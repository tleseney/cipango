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
package org.cipango.tests.replication;

import static org.cipango.client.test.matcher.SipMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import javax.servlet.sip.SipServletRequest;

import org.cipango.client.Call;
import org.cipango.client.SipMethods;
import org.cipango.client.test.UaRunnable;
import org.cipango.client.test.UasScript;
import org.cipango.tests.UaTestCase;
import org.junit.Test;

public class B2bTest extends UaTestCase
{
	
	/**
	 *  Alice          Cipango           Bob 
	 *    |               |               | 
	 *    | INVITE        |               | 
	 *    |-------------->| INVITE        |
	 *    |               |-------------->|
	 *    |               |       200 OK  |
	 *    |       200 OK  |<--------------|
	 *    |<--------------|               |
	 *    |ACK            |               |
	 *    |-------------->| ACK           |
	 *    |               |-------------->|
	 *    
	 *    Cipango restart        
	 *            
	 *    | BYE           |               | 
	 *    |-------------->| BYE           |
	 *    |               |-------------->|
	 *    |               |       200 OK  |
	 *    |       200 OK  |<--------------|
	 *    |<--------------|               |
	 */
	@Test
	public void testB2b() throws Throwable
	{		
		Call callA;
		Endpoint bob = createEndpoint("bob");
		bob.getUserAgent().setTimeout(25000);
		UaRunnable callB = new UasScript.OkBye(bob.getUserAgent());
		
		callB.start();
		
		SipServletRequest request = _ua.createRequest(SipMethods.INVITE, bob.getUri());
		request.setRequestURI(bob.getContact().getURI());
		callA = _ua.createCall(request);

        assertThat(callA.waitForResponse(), isSuccess());
        callA.createAck().send();
        
        new Restarter().restartCipango();
        
        callA.createBye().send();
        assertThat(callA.waitForResponse(), isSuccess());
        
        callB.assertDone();
		
	}
	
}
