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
package org.cipango.kaleo.xcap;

import junit.framework.TestCase;

public class XcapUriTest extends TestCase
{

	public void testParsing() throws Exception
	{
		String requestUri = "/cipango-kaleo/xcap/resource-lists/users/sip:carol@cipango.org/index/~~/resource-lists/list[@name=\"Default\"]/entry[@uri=\"sip:alice@cipango.org\"]";
		XcapUri uri = new XcapUri(requestUri, "/cipango-kaleo/xcap/");
		assertEquals("resource-lists/users/sip:carol@cipango.org/index", uri.getDocumentSelector());
		assertFalse(uri.isGlobal());
		assertEquals("sip:carol@cipango.org", uri.getUser());
		assertEquals("resource-lists", uri.getAuid());
		assertEquals("/resource-lists/list[@name=\"Default\"]/entry[@uri=\"sip:alice@cipango.org\"]", uri.getNodeSelector());
		assertEquals(requestUri.substring("/cipango-kaleo/xcap/".length()), uri.toString());
	}
	
	public void testDecoding() throws Exception
	{
		String requestUri = "/cipango-kaleo/xcap/resource-lists/users/sip:carol@cipango.org/index/~~/resource-lists/list%5B@name=%22Default%22%5D/entry%5B@uri=%22sip%3Aalice%40cipango.org%22%5D";
		XcapUri uri = new XcapUri(requestUri, "/cipango-kaleo/xcap/");
		assertEquals("resource-lists/users/sip:carol@cipango.org/index", uri.getDocumentSelector());
		assertEquals("/resource-lists/list[@name=\"Default\"]/entry[@uri=\"sip:alice@cipango.org\"]", uri.getNodeSelector());
		//System.out.println(uri);
	}
}
