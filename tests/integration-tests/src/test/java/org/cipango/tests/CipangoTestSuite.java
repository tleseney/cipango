// ========================================================================
// Copyright 2010 NEXCOM Systems
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
package org.cipango.tests;

import java.util.Enumeration;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class CipangoTestSuite
{
	public static Test suite()
	{
		TestSuite suite = new TestSuite(CipangoTestSuite.class.getName());

		suite.addTestSuite(org.cipango.tests.integration.AuthenticationTest.class);
		//suite.addTestSuite(org.cipango.sipunit.test.B2bHelperForkTest.class);
		suite.addTestSuite(org.cipango.tests.integration.B2bHelperTest.class);
		suite.addTestSuite(org.cipango.tests.integration.DoubleRecordRouteTest.class);
		suite.addTestSuite(org.cipango.tests.integration.InvalidateWhenReadyTest.class);
		suite.addTestSuite(org.cipango.tests.integration.MethodAuthenticationTest.class);
		suite.addTestSuite(org.cipango.tests.integration.ProxyAuthenticationTest.class);
		suite.addTestSuite(org.cipango.tests.integration.ProxyTest.class);
		suite.addTestSuite(org.cipango.tests.integration.ReliableTest.class);
		suite.addTestSuite(org.cipango.tests.integration.ServletContextListenerTest.class);
		suite.addTestSuite(org.cipango.tests.integration.SipApplicationKeyTest.class);
		suite.addTestSuite(org.cipango.tests.integration.SipApplicationSessionTest.class);
		suite.addTestSuite(org.cipango.tests.integration.TcpTest.class);
		suite.addTestSuite(org.cipango.tests.integration.UacTest.class);
		
		//suite = filterOneTest(suite, "testCancel");
		return suite;
	}
	
	protected static TestSuite filterOneTest(TestSuite suite, String testName)
	{
		TestSuite filtered = new TestSuite(suite.getName());
		Enumeration<Test> tests = suite.tests();
		while (tests.hasMoreElements())
		{
			TestSuite classTestSuite = (TestSuite) tests.nextElement();
			TestSuite filteredClassSuite = new TestSuite(classTestSuite.getName());
			Enumeration<Test> testcases = classTestSuite.tests();
			while (testcases.hasMoreElements())
			{
				TestCase testCase = (TestCase) testcases.nextElement();
				if (testCase.getName().equals(testName))
					filteredClassSuite.addTest(testCase);
			}
			if (filteredClassSuite.testCount() > 0)
				filtered.addTest(filteredClassSuite);
		}
		return filtered;
	}
}
