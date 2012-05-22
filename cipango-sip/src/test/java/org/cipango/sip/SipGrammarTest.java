package org.cipango.sip;

import org.junit.Test;
import static org.junit.Assert.*;

public class SipGrammarTest 
{
	@Test
	public void testChars()
	{
		assertTrue(SipGrammar.Charset.UNRESERVED.contains('S'));
		assertTrue(SipGrammar.Charset.UNRESERVED.contains('0'));
		assertTrue(SipGrammar.Charset.UNRESERVED.contains('*'));
		assertFalse(SipGrammar.Charset.UNRESERVED.contains('%'));
		assertFalse(SipGrammar.Charset.UNRESERVED.contains('#'));
		assertFalse(SipGrammar.Charset.UNRESERVED.contains(-1));
		
		assertTrue(SipGrammar.Charset.LWS.contains(" \t\n"));
		assertFalse(SipGrammar.Charset.ALPHA.contains("azerty."));
	}
}
