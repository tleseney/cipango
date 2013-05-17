package org.cipango.kaleo.xcap;

public class DeleteTest extends AbstractXcapServletTest
{

	public void testDeleteDocument() throws Exception
	{
		setContent(PutTest.PUT_DOCUMENT_URI);
		request.setRequestURI(PutTest.PUT_DOCUMENT_URI);
		doDelete();
		assertEquals(200, response.getStatusCode());

		doDelete();
		assertEquals(404, response.getStatusCode());
	}

	public void testDeleteAttribute() throws Exception
	{
		setContent(PutTest.PUT_ATTR_URI);
		request.setRequestURI(PutTest.PUT_ATTR_URI);
		doDelete();
		assertEquals(200, response.getStatusCode());

		doDelete();
		assertEquals(404, response.getStatusCode());
	}

	public void testDeleteElement() throws Exception
	{
		setContent(PutTest.PUT_ELEMENT_URI);
		request.setRequestURI(PutTest.PUT_ELEMENT_URI);
		doDelete();
		assertEquals(200, response.getStatusCode());

		doDelete();
		assertEquals(404, response.getStatusCode());
	}
}
