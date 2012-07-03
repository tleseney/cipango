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

import java.text.ParseException;

import javax.servlet.sip.SipURI;

import org.cipango.sip.SipURIImpl;

public class Constants
{
	public static SipURI ALICE_URI;
	public static SipURI BOB_URI;
	public static SipURI EXAMPLE_URI;

	static
	{
		try
		{
			ALICE_URI = new SipURIImpl("sips:alice@nexcom.fr");
			BOB_URI = new SipURIImpl("sips:bob@example.com:5061");
			EXAMPLE_URI = new SipURIImpl("example.com", 5070);
		}
		catch (ParseException e)
		{
			e.printStackTrace();
		}
	}
}
