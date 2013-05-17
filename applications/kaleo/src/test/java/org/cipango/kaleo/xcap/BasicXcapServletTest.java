package org.cipango.kaleo.xcap;


public class BasicXcapServletTest extends AbstractXcapServletTest {


	public void testGetXpointer() throws Exception {	
		setContent(XPOINTER_PRES_RULES_URI);
		request.setRequestURI(XPOINTER_PRES_RULES_URI);
		request.setupAddParameter("xmlns(cp", "urn:ietf:params:xml:ns:common-policy)");
		doGet();
		
		assertEquals(200, response.getStatusCode());
		assertEquals(XcapServiceTest.EXPECTED_RESULT, response.getOutputStreamContent());
		assertEquals("application/xcap-el+xml", response.getContentType());
	
	}
	
	public void testMulitpleXpointer() throws Exception {
		setContent(MULTIPLE_XPOINTER_URI);
		request.setRequestURI(MULTIPLE_XPOINTER_URI);
		request.setupAddParameter("xmlns(cp", "urn:ietf:params:xml:ns:common-policy) " +
				"xmlns(pp=urn:ietf:params:xml:ns:pres-rules)");
		doGet();
		
		assertEquals(200, response.getStatusCode());
		assertEquals("application/xcap-el+xml", response.getContentType());
	
	}
	
	public void testGetNoPrefix() throws Exception {
		setContent(NO_PREFIX_PRES_RULES_URI);
		request.setRequestURI(NO_PREFIX_PRES_RULES_URI);
		request.setupAddParameter("xmlns(cp", "urn:ietf:params:xml:ns:common-policy)");
		doGet();
		
		assertEquals(200, response.getStatusCode());
		assertEquals(XcapServiceTest.EXPECTED_RESULT, response.getOutputStreamContent());
		assertEquals("application/xcap-el+xml", response.getContentType());
	}
	
	public void testGetNotAFile() throws Exception {
		request.setRequestURI("xcap-caps/global");
		doGet();
		assertEquals(404, response.getStatusCode());
	}
	
	
	/* FIXME
	public void testGetNamespace() throws Exception {		
		request.setRequestURI(XcapServiceTest.GET_NAMESPACE_BINDINGS);
		doGet();
		
		assertEquals(200, response.getStatusCode());
		assertEquals("<ruleset xmlns:xml=\"http://www.w3.org/XML/1998/namespace\" " +
				"xmlns:cr=\"urn:ietf:params:xml:ns:common-policy\" " +
				"xmlns:pr=\"urn:ietf:params:xml:ns:pres-rules\" " +
				"xmlns=\"urn:ietf:params:xml:ns:pres-rules\"/>",
				response.getOutputStreamContent());
		assertEquals("application/xcap-ns+xml", response.getContentType());
	}
	*/
	
	
	public static final String XPOINTER_PRES_RULES_URI =
		"/pres-rules/users/thomas@cipango.org/index/~~/cp:ruleset/cp:rule%5b@id=%22a%22%5d/cp:conditions";

	public static final String MULTIPLE_XPOINTER_URI =
		"/pres-rules/users/thomas@cipango.org/index/~~/cp:ruleset/cp:rule[@id=\"a\"]/cp:actions/pp:sub-handling";

	
	public static final String NO_PREFIX_PRES_RULES_URI =
		"/pres-rules/users/thomas@cipango.org/index/~~/ruleset/rule%5b@id=%22a%22%5d/conditions";


}
