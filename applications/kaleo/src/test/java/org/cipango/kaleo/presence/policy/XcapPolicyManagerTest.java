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
package org.cipango.kaleo.presence.policy;

import java.util.ArrayList;
import java.util.List;

import org.cipango.kaleo.presence.Presentity;
import org.cipango.kaleo.presence.policy.PolicyManager.SubHandling;
import org.cipango.kaleo.xcap.AbstractXcapServletTest;
import org.cipango.kaleo.xcap.XcapService;

public class XcapPolicyManagerTest extends AbstractXcapServletTest
{


	private XcapPolicyManager _policyManager;

	private static final String PRES_RULES_1 = 
		"/org.openmobilealliance.pres-rules/users/sip:nicolas@cipango.org/pres-rules";
	
	public void setUp() throws Exception
	{
		super.setUp();
		XcapService xcapService = _xcapServlet.getXcapService();
		_policyManager = new XcapPolicyManager(xcapService);
	}
	
	public void testGetPolicyNicolas() throws Exception
	{
		setContent(PRES_RULES_1);
		Presentity presentity = new Presentity("sip:nicolas@cipango.org");

		assertEquals(SubHandling.ALLOW, 
				_policyManager.getPolicy("sip:user@example.com", presentity));
		
		assertEquals(SubHandling.BLOCK, 
				_policyManager.getPolicy("sip:user@example.com", new Presentity("sip:unknown@cipango.org")));
		
		assertEquals(SubHandling.BLOCK,
				_policyManager.getPolicy("sip:unknown@example.com", presentity));
	}
	
	public void testGetPolicyAlice() throws Exception
	{
		setContent("/org.openmobilealliance.pres-rules/users/sip:alice@cipango.org/pres-rules");
		setContent("/resource-lists/users/sip:alice@cipango.org/index");
		Presentity presentity = new Presentity("sip:alice@cipango.org");
		XcapPolicy policy = (XcapPolicy) _policyManager.getPolicy(presentity);
		assertEquals(SubHandling.CONFIRM,
				_policyManager.getPolicy("sip:unknown@cipango.org", presentity));
		
		assertEquals(2, policy.getXcapResources().size());
		assertEquals("org.openmobilealliance.pres-rules/users/sip:alice@cipango.org/pres-rules", 
				policy.getXcapResources().get(0).toString());
		assertEquals("resource-lists/users/sip:alice@cipango.org/index", 
				policy.getXcapResources().get(1).toString());
		
		assertEquals(SubHandling.ALLOW, 
				_policyManager.getPolicy("sip:allow@cipango.org", presentity));
				
		assertEquals(SubHandling.BLOCK,
				_policyManager.getPolicy("sip:block@cipango.org", presentity));
		
	}
	
	
	public void testGetPolicyIetf() throws Exception
	{
		setContent("/org.openmobilealliance.pres-rules/users/sip:ietf@cipango.org/pres-rules");
		Presentity presentity = new Presentity("sip:ietf@cipango.org");
		
		// Match rule a
		assertEquals(SubHandling.POLITE_BLOCK,
				_policyManager.getPolicy("sip:polite-block@example.com", presentity));
		// Match rule b
		assertEquals(SubHandling.ALLOW,
				_policyManager.getPolicy("sip:alice@example.com", presentity));
		// Match no rule (as use except)
		assertEquals(SubHandling.BLOCK,
				_policyManager.getPolicy("sip:except@cipango.org", presentity));
		// Match rule a and rule b on domain cipango.org but rule b is more permissive
		assertEquals(SubHandling.ALLOW,
				_policyManager.getPolicy("sip:allow@cipango.org", presentity));
		// Match no rule
		assertEquals(SubHandling.BLOCK,
				_policyManager.getPolicy("sip:otherDomain@example.com", presentity));

	}
	
	public void testGetPolicyOma() throws Exception
	{
		setContent("/org.openmobilealliance.pres-rules/users/sip:oma@cipango.org/pres-rules");
		setContent("/resource-lists/users/sip:oma@cipango.org/index");
		Presentity presentity = new Presentity("sip:oma@cipango.org");

		// Granted by resource list
		assertEquals(SubHandling.ALLOW,
				_policyManager.getPolicy("sip:carol@cipango.org", presentity));
		// Own URI
		assertEquals(SubHandling.ALLOW,
				_policyManager.getPolicy("sip:oma@cipango.org", presentity));
		// Other identity
		assertEquals(SubHandling.CONFIRM,
				_policyManager.getPolicy("sip:other-identity@example.com", presentity));
		// Blocked by resource list
		assertEquals(SubHandling.BLOCK,
				_policyManager.getPolicy("sip:edwige@cipango.org", presentity));
	}
	
	/**
	 * Check that a policy listener is invoked when the XCAP document is changed.
	 * @throws Exception
	 */
	public void testPolicyListener() throws Exception
	{
		setContent(PRES_RULES_1);
		Presentity presentity = new Presentity("sip:nicolas@cipango.org");
		final List<Long> policyUpdated = new ArrayList<Long>();
		PolicyListener l = new PolicyListener()
		{
			public void policyHasChanged(Policy policy)
			{
				policyUpdated.add(System.currentTimeMillis());
			}
		};
		Policy policy = _policyManager.getPolicy(presentity);
		policy.addListener(l);
		assertEquals(SubHandling.BLOCK, 
				_policyManager.getPolicy("sip:testInsertElement@example.com", presentity));
		
		
		request.setRequestURI(PRES_RULES_1 + "/~~/cr:ruleset/cr:rule%5b@id=%22a%22%5d/cr:conditions");
		request.setContentType("application/xcap-el+xml");
		byte[] content = getResourceAsBytes("/xcap-root/pres-rules/users/put/element.xml");
		request.setBodyContent(content);
		request.setContentLength(content.length);
		doPut();

		assertEquals(200, response.getStatusCode());
		
		assertEquals(1, policyUpdated.size());
		assertEquals(SubHandling.ALLOW, 
				_policyManager.getPolicy("sip:testInsertElement@example.com", presentity));
		
	}
		
}
