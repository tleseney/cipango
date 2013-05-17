package org.cipango.kaleo.xcap;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.cipango.kaleo.web.XcapServlet;
import org.cipango.kaleo.xcap.dao.FileXcapDao;

import com.mockrunner.mock.web.MockHttpServletRequest;
import com.mockrunner.mock.web.MockHttpServletResponse;
import com.mockrunner.servlet.BasicServletTestCaseAdapter;

public abstract class AbstractXcapServletTest extends BasicServletTestCaseAdapter {

	protected XcapServlet _xcapServlet;
	private File _xcapRoot;
	
	protected void setUp() throws Exception {
		super.setUp();
		
		_xcapServlet = new XcapServlet();
		XcapService xcapService = new XcapService();
		FileXcapDao dao = new FileXcapDao();
		_xcapRoot = new File("target/test-data");
		_xcapRoot.mkdirs();
		dao.setBaseDir(_xcapRoot);
		xcapService.setDao(dao);
		xcapService.start();
		xcapService.setRootName("/");
		_xcapServlet.setXcapService(xcapService);
		
		setServlet(_xcapServlet);
	
		response = getWebMockObjectFactory().getMockResponse();
		request = getWebMockObjectFactory().getMockRequest();

	}
	
	protected void setContent(String xcapUri) throws Exception
	{
		XcapServiceTest.setContent(_xcapServlet.getXcapService(), _xcapRoot, xcapUri);
	}
	
	public void copyFile(String source, String destination) throws IOException {
		File sourceFile = new File(xcapRoot, source);
		InputStream is = new FileInputStream(sourceFile);
		File outputFile = new File(xcapRoot, destination);
		FileOutputStream os = new FileOutputStream(outputFile);
		int read;
		byte[] buffer = new byte[1024];
		while ((read = is.read(buffer)) != -1) {
			os.write(buffer, 0, read);
		}
		os.close();
		is.close();
	}
	
	public byte[] getResourceAsBytes(String resourceName) throws IOException {
		InputStream is = AbstractXcapServletTest.class.getResourceAsStream(resourceName);
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int read;
		while ((read = is.read(buffer)) != -1) {
			os.write(buffer, 0, read);
		}
		return os.toByteArray();
	}

	
	public void doPut() {
		request.setRequestURL(
				XcapServiceTest.HEAD + request.getRequestURI());
		super.doPut();
	}
	
	public void doGet() {
		request.setRequestURL(
				XcapServiceTest.HEAD + request.getRequestURI());
		super.doGet();
	}
	
	public void doDelete() {
		request.setRequestURL(
				XcapServiceTest.HEAD + request.getRequestURI());
		super.doDelete();
	}
	
	protected MockHttpServletResponse response;
	protected MockHttpServletRequest request;
	protected File xcapRoot;
}
