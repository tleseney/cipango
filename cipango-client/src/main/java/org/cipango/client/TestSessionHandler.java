package org.cipango.client;

import static junit.framework.Assert.fail;

import javax.servlet.sip.SipServletRequest;

public class TestSessionHandler extends SessionHandler
{
	public SipServletRequest waitForRequest(String method)
	{
		return waitForRequest(method, true);
	}
	
	/**
	 * Wait for a request.
	 * 
	 * @param method the method expected.
	 * @param strict if <code>true</code>, fails if received a request with the wrong method else,
	 *            ignore request with wrong method.
	 * @param timeout
	 * @return
	 */
	public SipServletRequest waitForRequest(String method, boolean strict)
	{
		synchronized (_requests)
		{
			long end = System.currentTimeMillis() + getTimeout();
			
			SipServletRequest request = waitForRequest(false);
			
			if (request == null)
				fail("No request received");

			if (strict && !request.getMethod().equals(method))
				fail("Received " + request.getMethod() + " when expected " + method);

			while (!request.getMethod().equals(method))
			{
				long timeout = end - System.currentTimeMillis();
				
				if (timeout <= 0)
					fail("No request received");
				
				doWait(timeout, true);
				request = getUnreadRequest();
				if (request == null)
					fail("No request received");
			}
			setRead(request);
			return request;
		}
	}

}
