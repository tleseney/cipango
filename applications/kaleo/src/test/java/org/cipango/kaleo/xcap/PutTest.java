package org.cipango.kaleo.xcap;


public class PutTest extends AbstractXcapServletTest
{

	public void testNoParent() throws Exception
	{
		setContent("/pres-rules/users/put/index");
		request.setRequestURI("/pres-rules/users/put/notExist/~~/cr:ruleset/");
		doPut();

		assertEquals(409, response.getStatusCode());
		assertEquals(NO_PARENT, response.getOutputStreamContent());
		assertEquals("application/xcap-error+xml", response.getContentType());
	}
	
	public void testNoParent2() throws Exception
	{
		setContent("/pres-rules/users/put/index");
		request.setRequestURI("/pres-rules/users/put/notExist/index");
		doPut();

		assertEquals(409, response.getStatusCode());
		assertEquals(NO_PARENT_2, response.getOutputStreamContent());
		assertEquals("application/xcap-error+xml", response.getContentType());
	}

	public void testNoParentNode() throws Exception
	{
		request.setRequestURI("/pres-rules/users/put/index/~~/cr:ruleset/cr:unknown/cr:bad");
		request.setContentType("application/xcap-el+xml");
		byte[] content = getResourceAsBytes("/xcap-root/pres-rules/users/put/element.xml");
		request.setBodyContent(content);
		request.setContentLength(content.length);
		doPut();

		assertEquals(409, response.getStatusCode());
		assertEquals(NO_PARENT_NODE, response.getOutputStreamContent());
		assertEquals("application/xcap-error+xml", response.getContentType());
	}

	public void testBadContentType() throws Exception
	{
		setContent(PUT_DOCUMENT_URI);
		request.setRequestURI(PUT_DOCUMENT_URI);
		request.setContentType("badContentType");
		byte[] content = getResourceAsBytes("/xcap-root/pres-rules/users/put/allDocument.xml");
		request.setBodyContent(content);
		request.setContentLength(content.length);
		doPut();

		assertEquals(415, response.getStatusCode());
	}

	public void testNotWellFormed() throws Exception
	{
		request.setRequestURI(PUT_DOCUMENT_URI);
		request.setContentType("application/auth-policy+xml");
		byte[] content = getResourceAsBytes("/xcap-root/pres-rules/users/put/notWellFormedDocument.xml");
		request.setBodyContent(content);
		request.setContentLength(content.length);
		doPut();

		assertEquals(409, response.getStatusCode());
		verifyOutputContains("<not-well-formed/>");
		// System.out.println(response.getOutputStreamContent());
		assertEquals("application/xcap-error+xml", response.getContentType());
	}

	public void testPutValidElem() throws Exception
	{
		setContent(PUT_ELEMENT_URI);
		request.setRequestURI(PUT_ELEMENT_URI);
		request.setContentType("application/xcap-el+xml");
		byte[] content = getResourceAsBytes("/xcap-root/pres-rules/users/put/element.xml");
		request.setBodyContent(content);
		request.setContentLength(content.length);
		doPut();

		assertEquals(200, response.getStatusCode());

		// request = getWebMockObjectFactory().createMockRequest();
		request.setRequestURI("/pres-rules/users/put/index/~~/cr:ruleset/cr:rule%5b@id=%22a%22%5d/cr:conditions/cr:identity/cr:one/@id");
		response.clearHeaders();
		response.resetBuffer();
		request.clearHeaders();
		doGet();
		System.out.println(response.getOutputStreamContent());
		response = getWebMockObjectFactory().getMockResponse();
		assertEquals(200, response.getStatusCode());
		assertEquals("sip:testInsertElement@example.com", response
				.getOutputStreamContent());
	}

	public void testPutNewElem() throws Exception
	{
		setContent("/pres-rules/users/put/index");
		request.setRequestURI("/pres-rules/users/put/index/~~/cr:ruleset/cr:rule%5b@id=%22newRule%22%5d");
		request.setContentType("application/xcap-el+xml");
		byte[] content = getResourceAsBytes("/xcap-root/pres-rules/users/put/newElement.xml");
		request.setBodyContent(content);
		request.setContentLength(content.length);
		doPut();

		assertEquals(200, response.getStatusCode());

		// request = getWebMockObjectFactory().createMockRequest();
		request.setRequestURI("/pres-rules/users/put/index/~~/cr:ruleset/cr:rule%5b@id=%22newRule%22%5d/cr:conditions/cr:identity/cr:one/@id");
		response.clearHeaders();
		response.resetBuffer();
		request.clearHeaders();

		doGet();

		response = getWebMockObjectFactory().getMockResponse();
		assertEquals(200, response.getStatusCode());
		assertEquals("sip:testInsertNewElement@cipango.org", response
				.getOutputStreamContent());
	}

	public void testBadValidation() throws Exception
	{

		request.setRequestURI("/pres-rules/users/put/index/~~/cr:ruleset/cr:rule%5b@id=%22newRule%22%5d");
		request.setContentType("application/xcap-el+xml");
		byte[] content = getResourceAsBytes("/xcap-root/pres-rules/users/put/allDocument.xml");
		request.setBodyContent(content);
		request.setContentLength(content.length);
		doPut();

		assertEquals(409, response.getStatusCode());

	}

	public void testPutValidAttribute() throws Exception
	{
		setContent(PutTest.PUT_ATTR_URI);
		request.setRequestURI(PUT_ATTR_URI);
		request.setContentType("application/xcap-att+xml");
		String content = "testPutValidAttribute";
		request.setBodyContent(content);
		request.setContentLength(content.length());
		doPut();

		assertEquals(200, response.getStatusCode());

		// request = getWebMockObjectFactory().createMockRequest();
		request.setRequestURI(PUT_ATTR_URI);
		response.resetBuffer();

		doGet();
		response = getWebMockObjectFactory().getMockResponse();
		assertEquals(200, response.getStatusCode());
		assertEquals(content, response.getOutputStreamContent());
	}

	public void testPutNewAttribute() throws Exception
	{
		setContent(PutTest.PUT_ATTR_URI);
		// First delete attr
		request.setRequestURI(PutTest.PUT_ATTR_URI);
		doDelete();
		assertEquals(200, response.getStatusCode());

		request.setRequestURI(PUT_ATTR_URI);
		request.setContentType("application/xcap-att+xml");
		String content = "testPutNewAttribute";
		request.setBodyContent(content);
		request.setContentLength(content.length());
		doPut();

		assertEquals(200, response.getStatusCode());

		// request = getWebMockObjectFactory().createMockRequest();
		request.setRequestURI(PUT_ATTR_URI);
		response.resetBuffer();

		doGet();
		//System.out.println(response.getOutputStreamContent());
		response = getWebMockObjectFactory().getMockResponse();
		assertEquals(200, response.getStatusCode());
		assertEquals(content, response.getOutputStreamContent());
	}

	public void testPutNewDocument() throws Exception
	{
		request.setRequestURI(PutTest.PUT_DOCUMENT_URI);
		doDelete();
		
		request.setRequestURI(XcapServiceTest.NEW_PRES_RULES_DOC);
		request.setContentType("application/auth-policy+xml");
		byte[] content = getResourceAsBytes("/xcap-root/pres-rules/users/put/allDocument.xml");
		request.setBodyContent(content);
		request.setContentLength(content.length);
		doPut();

		assertEquals(200, response.getStatusCode());

		response.clearHeaders();
		response.resetBuffer();
		request.clearHeaders();

		request.setRequestURI(XcapServiceTest.NEW_PRES_RULES_DOC);
		doGet();

		assertEquals(200, response.getStatusCode());
		System.out.println(response.getOutputStreamContent());
	}

	public void testPutNamespace() throws Exception
	{
		request.setRequestURI(XcapServiceTest.GET_NAMESPACE_BINDINGS);
		request.setContentType("application/xcap-ns+xml");
		byte[] content = "xmlns:xml=\"http://www.w3.org/XML/1998/namespace\""
				.getBytes();
		request.setBodyContent(content);
		request.setContentLength(content.length);
		doPut();

		assertEquals(405, response.getStatusCode());
	}

	/*
	 * public void testNotValidateSpecificAppConstraint() throws Exception {
	 * XcapServlet servlet = (XcapServlet) getServlet(); ProcessorDescription
	 * descr = new
	 * ProcessorDescription("com.nexcom.sipapps.xcap.NotValidateProcessor");
	 * servlet.getManager().registerProcessor(descr);
	 * 
	 * request.setRequestURI(PUT_DOCUMENT_URI);
	 * request.setContentType("application/auth-policy+xml"); byte[] content =
	 * getResourceAsBytes("/xcap-root/pres-rules/users/put/allDocument.xml");
	 * request.setBodyContent(content);
	 * request.setContentLength(content.length); doPut();
	 * 
	 * assertEquals(409, response.getStatusCode());
	 * verifyOutputContains("<constraint-failure/>");
	 * //System.out.println(response.getOutputStreamContent());
	 * assertEquals("application/xcap-error+xml", response.getContentType()); }
	 */

	public void testPutNonImptentAttribute() throws Exception
	{
		setContent(PUT_NON_IMPOTENT_URI);
		
		request.setRequestURI(PUT_NON_IMPOTENT_URI);
		request.setContentType("application/xcap-att+xml");
		String content = "newRule";
		request.setBodyContent(content);
		request.setContentLength(content.length());
		doPut();

		assertEquals(409, response.getStatusCode());
		verifyOutputContains("<cannot-insert/>");

		response.clearHeaders();
		response.resetBuffer();
		request.clearHeaders();

		request
				.setRequestURI("/pres-rules/users/put/index/~~/cr:ruleset/cr:rule/@id");
		doGet();

		System.out.println(response.getOutputStreamContent());
		assertEquals(200, response.getStatusCode());
		assertEquals("a", response.getOutputStreamContent());
	}

	private static final String NO_PARENT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
			+ "<xcap-error xmlns=\"urn:ietf:params:xml:ns:xcap-error\">\n"
			+ "<no-parent><ancestor>http://aloha:8080/pres-rules/users/put</ancestor></no-parent>\n"
			+ "</xcap-error>";
	
	private static final String NO_PARENT_2 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
		+ "<xcap-error xmlns=\"urn:ietf:params:xml:ns:xcap-error\">\n"
		+ "<no-parent><ancestor>http://aloha:8080/pres-rules/users/put</ancestor></no-parent>\n"
		+ "</xcap-error>";

	private static final String NO_PARENT_NODE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
			+ "<xcap-error xmlns=\"urn:ietf:params:xml:ns:xcap-error\">\n"
			+ "<no-parent><ancestor>http://aloha:8080/pres-rules/users/put/index/~~/cr:ruleset</ancestor></no-parent>\n"
			+ "</xcap-error>";

	public static final String PUT_DOCUMENT_URI = "/pres-rules/users/put/index";
	public static final String PUT_ATTR_URI = "/resource-lists/users/nicolas/index/~~/rl:resource-lists/rl:list/@name";
	public static final String PUT_ELEMENT_URI = "/pres-rules/users/put/index/~~/cr:ruleset/cr:rule%5b@id=%22a%22%5d/cr:conditions";
	public static final String PUT_NON_IMPOTENT_URI = "/pres-rules/users/put/index/~~/cr:ruleset/cr:rule%5b@id=%22a%22%5d/@id";

}