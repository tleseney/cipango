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

import org.cipango.client.Call;
import org.cipango.client.SipMethods;
import org.cipango.tests.UaTestCase;
import org.junit.Test;

public class UasTest extends UaTestCase
{

	/**
	 *  Alice          Cipango
	 *    |               |
	 *    | INVITE        |
	 *    |-------------->|
 	 *    |       180     |
	 *    |<--------------|
	 *    |       200 OK  |
	 *    |<--------------|
	 *    |ACK            |
	 *    |-------------->|
	 *    
	 *     Cipango restart
	 *            
	 *    | BYE           |
	 *    |-------------->|
	 *    |       200 OK  |
	 *    |<--------------|
	 */
	@Test
	public void testUas() throws Exception
	{
		Call call = _ua.createCall(_ua.getFactory().createURI(getTo()));
        assertThat(call.waitForResponse(), isSuccess());
        call.createAck().send();
        
        new Restarter().restartCipango();
        
        call.createRequest(SipMethods.BYE).send();
        assertThat(call.waitForResponse(), isSuccess());		
	}
	
}
