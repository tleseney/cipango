// ========================================================================
// Copyright 2009 NEXCOM Systems
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
package org.cipango.kaleo.xcap.util;

import junit.framework.TestCase;

public class XcapUtilTest extends TestCase
{

	public void testInsertDefaultNamespace() throws Exception
	{
		assertEquals("/rl:resource-lists/rl:list[@name=\"Default\"]/rl:entry[@uri=\"sip:alice@cipango.org\"]", 
				XcapUtil.insertDefaultNamespace("/resource-lists/list[@name=\"Default\"]/entry[@uri=\"sip:alice@cipango.org\"]", "rl"));
		assertEquals("/rl:resource-lists/rl:list[@name=\"Default\"]/rl:entry/@uri", 
				XcapUtil.insertDefaultNamespace("/resource-lists/list[@name=\"Default\"]/entry/@uri", "rl"));
		assertEquals("/rl:resource-lists/rl:list[@name=\"Default\"]/rl:entry/", 
				XcapUtil.insertDefaultNamespace("/resource-lists/list[@name=\"Default\"]/entry/", "rl"));
		assertEquals("/dd:resource-lists/rl:list[@name=\"Default\"]/rl:entry", 
				XcapUtil.insertDefaultNamespace("/dd:resource-lists/list[@name=\"Default\"]/entry", "rl"));
		
	}
}
