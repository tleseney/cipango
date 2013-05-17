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

import java.io.IOException;

import javax.servlet.ServletException;

import org.cipango.tests.UaTestCase;

public class ServletContextListenerTest extends UaTestCase
{

	public void testInit() throws IOException, ServletException
	{
		sendAndAssertMessage();
	}
	
	/**
	 * 
	 * Test for CIPANGO-199: SIP servlet are initialized before ServletContextListener if listener has been defined in web.xml
	 */
	public void testInitFromWeb() throws IOException, ServletException
	{
		sendAndAssertMessage();
	}
}
