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
package org.cipango.kaleo.presence;

import junit.framework.TestCase;

import org.cipango.kaleo.presence.pidf.Basic;
import org.cipango.kaleo.presence.pidf.Presence;
import org.cipango.kaleo.presence.pidf.PresenceDocument;
import org.cipango.kaleo.presence.pidf.Tuple;


public class PresentityTest extends TestCase
{
	
	public void testSimple() throws Exception
	{
		Presentity presentity = new Presentity("sip:alice@cipango.org");
		assertTrue(presentity.isDone());
		PresenceDocument doc = (PresenceDocument) presentity.getState().getContent();
		assertEquals(presentity.getNeutralState().getContent().toString(), 
				doc.toString());
		
		PresenceDocument doc1 = PresenceDocument.Factory.newInstance();
		Tuple tuple1 = doc1.addNewPresence().addNewTuple();
		tuple1.setId("tuple1");
		tuple1.addNewStatus().setBasic(Basic.OPEN);
		SoftState state1 = presentity.addState(PresenceEventPackage.PIDF, doc1, 60);
		
		doc = (PresenceDocument) presentity.getState().getContent();
		assertEquals(1, doc.getPresence().getTupleArray().length);
		assertEquals("tuple1", 
				doc.getPresence().getTupleArray(0).getId());
		assertEquals(Basic.OPEN, 
				doc.getPresence().getTupleArray(0).getStatus().getBasic());
		
		PresenceDocument doc2 = PresenceDocument.Factory.newInstance();
		Tuple tuple2 = doc2.addNewPresence().addNewTuple();
		tuple2.setId("tuple2");
		tuple2.addNewStatus().setBasic(Basic.CLOSED);
		SoftState state2 = presentity.addState(PresenceEventPackage.PIDF, doc2, 60);
		
		doc = (PresenceDocument) presentity.getState().getContent();
		assertEquals(2, doc.getPresence().getTupleArray().length);
		assertEquals("tuple1", 
				doc.getPresence().getTupleArray(0).getId());
		assertEquals(Basic.OPEN, 
				doc.getPresence().getTupleArray(0).getStatus().getBasic());
		assertEquals("tuple2", 
				doc.getPresence().getTupleArray(1).getId());
		assertEquals(Basic.CLOSED, 
				doc.getPresence().getTupleArray(1).getStatus().getBasic());
		assertFalse(presentity.isDone());
		
		presentity.removeState(state1.getETag());
		assertNull(presentity.getState(state1.getETag()));
		doc = (PresenceDocument) presentity.getState().getContent();
		assertEquals(1, doc.getPresence().getTupleArray().length);
		assertEquals("tuple2", 
				doc.getPresence().getTupleArray(0).getId());
		
		String etag = state2.getETag();
		assertEquals(state2, presentity.getState(etag));
		doc2 = (PresenceDocument) doc2.copy();
		doc2.getPresence().getTupleArray(0).getStatus().setBasic(Basic.OPEN);
		presentity.modifyState(state2, PresenceEventPackage.PIDF, doc2, 30);
		doc = (PresenceDocument) presentity.getState().getContent();
		assertEquals(Basic.OPEN, 
				doc.getPresence().getTupleArray(0).getStatus().getBasic());
		assertNotSame(etag, state2.getETag());
		
	}

	public void testGetState() throws Exception
	{		
		Presentity presentity = new Presentity("sip:alice@cipango.org");
		assertEquals(presentity.getNeutralState().getContent().toString(), 
				presentity.getState().getContent().toString());
		PresenceDocument doc1 = PresenceDocument.Factory.parse(getClass().getResourceAsStream("/org/cipango/kaleo/sipunit/publish1.xml"));
		presentity.addState(PresenceEventPackage.PIDF, doc1, 60);
		PresenceDocument doc2 = PresenceDocument.Factory.parse(getClass().getResourceAsStream("/pidf1.xml"));
		presentity.addState(PresenceEventPackage.PIDF, doc2, 60);
		Presence presence =  ((PresenceDocument) presentity.getState().getContent()).getPresence();
		assertEquals(2, presence.getTupleArray().length);
		assertEquals(doc1.getPresence().getTupleArray(0).getId(), presence.getTupleArray(0).getId());
		assertEquals(doc2.getPresence().getTupleArray(0).getId(), presence.getTupleArray(1).getId());
		
		PresenceDocument doc3 = PresenceDocument.Factory.parse(getClass().getResourceAsStream("/org/cipango/kaleo/sipunit/publish2.xml"));
		presentity.addState(PresenceEventPackage.PIDF, doc3, 60);
		presence =  ((PresenceDocument) presentity.getState().getContent()).getPresence();
		assertEquals(3, presence.getTupleArray().length);
		
		//System.out.println(presentity.getState().getContent());
	}
	
	public void testNamespaceCollision() throws Exception
	{		
		Presentity presentity = new Presentity("sip:alice@cipango.org");
	
		PresenceDocument doc1 = PresenceDocument.Factory.parse(getClass().getResourceAsStream("/pidfNamespace.xml"));
		presentity.addState(PresenceEventPackage.PIDF, doc1, 60);
		PresenceDocument doc2 = PresenceDocument.Factory.parse(getClass().getResourceAsStream("/pidf1.xml"));
		presentity.addState(PresenceEventPackage.PIDF, doc2, 60);
		Presence presence =  ((PresenceDocument) presentity.getState().getContent()).getPresence();
		//System.out.println(presentity.getState().getContent());
	}
	
	public void testInsertSameId() throws Exception
	{		
		Presentity presentity = new Presentity("sip:alice@cipango.org");
		PresenceDocument doc1 = PresenceDocument.Factory.parse(getClass().getResourceAsStream("/org/cipango/kaleo/sipunit/publish1.xml"));
		presentity.addState(PresenceEventPackage.PIDF, doc1, 60);
		presentity.addState(PresenceEventPackage.PIDF, doc1, 60);
		Presence presence =  ((PresenceDocument) presentity.getState().getContent()).getPresence();
		assertEquals(1, presence.getTupleArray().length);
	}
}
