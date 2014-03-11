// ========================================================================
// Copyright 2007-2008 NEXCOM Systems
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

package org.cipango.sip;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.fail;

import java.text.ParseException;

import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.TelURL;

import org.junit.Test;

public class TelURLTest
{

	@Test
	public void testSetPhoneNumber() throws Exception
	{
		TelURL url = (TelURL) URIFactory.parseURI("tel:+1-212-555-0101");
		assertEquals("1-212-555-0101", url.getPhoneNumber());
		assertNull(url.getPhoneContext());
		url.setPhoneNumber("+123-456");
		assertEquals("123-456", url.getPhoneNumber());
		url.setPhoneNumber("123-456", "atlanta.com");
		assertEquals("123-456", url.getPhoneNumber());
		assertEquals("atlanta.com", url.getPhoneContext());
		try	{ url.setPhoneNumber("+1/3"); fail();} catch (IllegalArgumentException e) {	}
		try	{ url.setPhoneNumber("1-212-555-0101"); fail();} catch (IllegalArgumentException e) {}
		try	{ url.setPhoneNumber("+1-212-555-0101", "atlanta.com"); fail();} catch (IllegalArgumentException e) {}assertEquals("#123#", newTelURL("tel:#123#;phone-context=atlanta.com").getPhoneNumber());
		
		url.setPhoneNumber("1ABCDEF2", "atlanta.com");
		assertEquals("*123*",  newTelURL("tel:*123*;phone-context=atlanta.com").getPhoneNumber());
		url.setPhoneNumber("1abcdef2", "atlanta.com");
		try	{ url.setPhoneNumber("+#123#"); fail();} catch (IllegalArgumentException e) {}
		try	{ url.setPhoneNumber("+123A"); fail();} catch (IllegalArgumentException e) {}
		try	{ newTelURL("tel:+*123*").getPhoneNumber(); fail();} catch (ParseException e) {}
		try	{ newTelURL("tel:+123A").getPhoneNumber(); fail();} catch (ParseException e) {}
	}	
	
	private TelURL newTelURL(String url) throws ServletParseException, ParseException {
		return (TelURL) URIFactory.parseURI(url);
	}

	@Test
	public void testParameters() throws Exception
	{
		String tel = "tel:863-1234;phone-context=+1-914-555";
		TelURL url = (TelURL) URIFactory.parseURI(tel);
		assertEquals("863-1234", url.getPhoneNumber());
		assertEquals("+1-914-555", url.getPhoneContext());
		assertFalse(url.isGlobal());
	}
}
