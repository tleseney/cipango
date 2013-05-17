package org.cipango.kaleo.presence.pidf;

import junit.framework.TestCase;

public class PidfTest extends TestCase
{
	public void testParsePidf() throws Exception
	{
		PresenceDocument doc = PresenceDocument.Factory.parse(getClass().getResourceAsStream("/pidf1.xml"));
		Tuple[] tuples = doc.getPresence().getTupleArray();
		assertNotNull(tuples);
		assertEquals(1, tuples.length);
		assertEquals("t7412", tuples[0].getId());
	}
	
	
/*	public void testCompose() throws Exception
	{
		PresenceDocument alice = PresenceDocument.Factory.parse(getClass().getResourceAsStream("/pidf1.xml"));
		Tuple tuple1 = alice.getPresence().getTupleArray(0);
		
		PresenceDocument alice2 = PresenceDocument.Factory.parse(getClass().getResourceAsStream("/org/cipango/kaleo/sipunit/publish1.xml"));
		Tuple tuple2 = alice2.getPresence().getTupleArray(0);
	
		HashMap<String, String> suggestedPrefixes = new HashMap<String, String>();
		suggestedPrefixes.put("urn:ietf:params:xml:ns:pidf:data-model", "dm");
		suggestedPrefixes.put("urn:ietf:params:xml:ns:pidf:rpid", "rpid");
		suggestedPrefixes.put("urn:ietf:params:xml:ns:pidf:cipid", "c");
		
		XmlOptions _xmlOptions = new XmlOptions();
		_xmlOptions.setUseDefaultNamespace();
		_xmlOptions.setSaveSuggestedPrefixes(suggestedPrefixes);
		
		//_xmlOptions.setSaveImplicitNamespaces(suggestedPrefixes);
		_xmlOptions.setSaveAggressiveNamespaces();
		_xmlOptions.setSaveNamespacesFirst();
		
		//PresenceDocument alice3 = PresenceDocument.Factory.newInstance(_xmlOptions);
		boolean s = tuple1.newCursor().copyXml(alice2.getPresence().addNewTuple().newCursor());
		System.out.println("Success: " + s);
		//alice3.getPresence().getDomNode().appendChild(tuple1.getDomNode().cloneNode(true));
		//alice3.addNewPresence();
		//Tuple[] tuples = new Tuple[] { tuple1, tuple2 };
		
		//alice3.getPresence().setTupleArray(tuples);
		//alice3.getPresence().setTupleArray(0, (Tuple) tuple1.copy());
		//alice3.getPresence().setTupleArray(1, tuple2);
		
		System.out.println(alice2.xmlText());
		//System.out.println(new String(new PidfHandler().getBytes(alice3)));
	}*/
	
}
