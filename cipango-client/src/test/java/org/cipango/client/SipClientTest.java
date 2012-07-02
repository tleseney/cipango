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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.text.ParseException;

import org.jmock.Mockery;
import org.junit.Test;

public class SipClientTest 
{
    Mockery _context = new Mockery();

	@Test
	public void testConstructor() throws ParseException
	{
		SipClient c = new SipClient("192.168.2.1", 5080);
		assertThat(c.getFactory(), is(notNullValue()));
		assertThat(c.getContact().getHost(), is("192.168.2.1"));
		assertThat(c.getContact().getPort(), is(5080));
	}
	
	@Test
	public void testConstructorWithNoHost()
	{
		SipClient c = new SipClient(5090);
		assertThat(c.getFactory(), is(notNullValue()));
		assertThat(c.getContact().getHost(), is("127.0.0.1"));
		assertThat(c.getContact().getPort(), is(5090));
	}
}
