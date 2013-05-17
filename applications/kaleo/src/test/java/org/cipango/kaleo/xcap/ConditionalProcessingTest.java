package org.cipango.kaleo.xcap;

import org.cipango.kaleo.Constants;


public class ConditionalProcessingTest extends AbstractXcapServletTest {

	
	public void setUp() throws Exception {
		super.setUp();	
	}
	
	public void testValidDoc() throws Exception {
		setContent(PutTest.PUT_DOCUMENT_URI);
		// GET
		request.setRequestURI(PutTest.PUT_DOCUMENT_URI);
		doGet();
		
		assertEquals(200, response.getStatusCode());
		assertEquals("application/auth-policy+xml", response.getContentType());
		String etag = response.getHeader(Constants.ETAG);
		
		// PUT
		request.setHeader(Constants.IF_MATCH, etag);
		request.setContentType("application/auth-policy+xml");
		byte[] content = getResourceAsBytes("/xcap-root/pres-rules/users/put/allDocument.xml");
		request.setBodyContent(content);
		request.setContentLength(content.length);
		doPut();
		
		assertEquals(200, response.getStatusCode());
		String etag2 = response.getHeader(Constants.ETAG);
		assertNotSame(etag, etag2);
		
//		 GET
		request.setRequestURI(PutTest.PUT_DOCUMENT_URI);
		request.setHeader(Constants.IF_MATCH, etag2);
		doGet();
		
		assertEquals(200, response.getStatusCode());
		assertEquals("application/auth-policy+xml", response.getContentType());
		String etag3 = response.getHeader(Constants.ETAG);
		assertEquals(etag2, etag3);
	}	
	
	public void testBadEtag() throws Exception {
		setContent(PutTest.PUT_DOCUMENT_URI);
		// PUT
		request.setRequestURI(PutTest.PUT_DOCUMENT_URI);
		request.setHeader(Constants.IF_MATCH, "badEtag");
		request.setContentType("application/auth-policy+xml");
		byte[] content = getResourceAsBytes("/xcap-root/pres-rules/users/put/allDocument.xml");
		request.setBodyContent(content);
		request.setContentLength(content.length);
		doPut();
		
		assertEquals(412, response.getStatusCode());
	}	
	
	public void testNotChanged() throws Exception {	
		setContent(PutTest.PUT_DOCUMENT_URI);
//		 GET
		request.setRequestURI(PutTest.PUT_DOCUMENT_URI);
		doGet();

		assertEquals(200, response.getStatusCode());
		assertEquals("application/auth-policy+xml", response.getContentType());
		String etag = response.getHeader(Constants.ETAG);
		
		request.setHeader(Constants.IF_NONE_MATCH, etag);
		doGet();
		
		assertEquals(304, response.getStatusCode());	
	}	
	
	public void testMultipleEtags() throws Exception {
		setContent(PutTest.PUT_DOCUMENT_URI);
//		 GET
		request.setRequestURI(PutTest.PUT_DOCUMENT_URI);
		doGet();
		
		assertEquals(200, response.getStatusCode());
		assertEquals("application/auth-policy+xml", response.getContentType());
		String etag = response.getHeader(Constants.ETAG);
		
		request.addHeader(Constants.IF_NONE_MATCH, "oneEtag");
		request.addHeader(Constants.IF_NONE_MATCH, etag + " , anotherEtag");
		doGet();
		
		assertEquals(304, response.getStatusCode());	
	}	
	
	public void testWilcard() throws Exception {
		// DELETE
		request.setRequestURI("/pres-rules/users/put/new");
		doDelete();

		// PUT
		request.setHeader(Constants.IF_NONE_MATCH, "*");
		request.setContentType("application/auth-policy+xml");
		byte[] content = getResourceAsBytes("/xcap-root/pres-rules/users/put/allDocument.xml");
		request.setBodyContent(content);
		request.setContentLength(content.length);
		doPut();
		
		assertEquals(200, response.getStatusCode());
		String etag = response.getHeader(Constants.ETAG);	
		
//		 PUT
		request.setHeader(Constants.IF_NONE_MATCH, etag);
		request.setBodyContent(content);
		request.setContentLength(content.length);
		doPut();
		
		assertEquals(412, response.getStatusCode());
		
//		 PUT
		request.setHeader(Constants.IF_MATCH, "*");
		request.setBodyContent(content);
		request.setContentLength(content.length);
		doPut();
		
		assertEquals(412, response.getStatusCode());
	}	
	
	
}
