// ========================================================================
// Copyright 2008-2009 NEXCOM Systems
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
package org.cipango.kaleo.xcap;

import org.apache.log4j.Logger;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Level;

public class XcapException extends RuntimeException
{
	/**
	 * Logger for this class
	 */
	private static final Logger log = Logger.getLogger(XcapException.class);

	public static final String SEPARATOR = "\n";
	public static final String XCAP_ERROR_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
			+ SEPARATOR
			+ "<xcap-error xmlns=\"urn:ietf:params:xml:ns:xcap-error\">"
			+ SEPARATOR;
	public static final String XCAP_ERROR_FOOTER = SEPARATOR + "</xcap-error>";

	public static final String XCAP_ERROR_CONTENT_TYPE = "application/xcap-error+xml";
	
	private int status;
	private String reason;
	private String headerName;
	private String headerValue;
	private boolean showStackTrace;
	private Level level;
	private String contentType;
	private byte[] content;
	

	public XcapException(String message, int statusCode, String reasonPhrase,
			Throwable e, boolean showStackTrace)
	{
		super(message, e);
		status = statusCode;
		this.reason = reasonPhrase;
		level = Level.WARN;
		this.showStackTrace = showStackTrace;
	}

	public XcapException(String message, int statusCode, String reasonPhrase,
			Throwable e)
	{
		this(message, statusCode, reasonPhrase, e, true);
	}

	public XcapException(String message, int statusCode, String reasonPhrase)
	{
		this(message, statusCode, reasonPhrase, null, false);
	}

	public XcapException(String message, int statusCode, Throwable e,
			boolean showStackTrace)
	{
		this(message, statusCode, null, e, showStackTrace);
	}

	public XcapException(String message, int statusCode, Throwable e)
	{
		this(message, statusCode, null, e, true);
	}

	public XcapException(String message, int statusCode)
	{
		this(message, statusCode, null, null, false);
	}

	public XcapException(String message, int statusCode, Level logLevel)
	{
		this(message, statusCode, null, null, false);
		this.level = logLevel;
	}

	/**
	 * Should be used only if <code>reasonPhrase</code> is enough
	 * understandable.
	 * 
	 * @param statusCode
	 * @param reasonPhrase
	 */
	public XcapException(int statusCode, String reasonPhrase)
	{
		this(statusCode + " " + reasonPhrase, statusCode, reasonPhrase, null,
				false);
	}

	/**
	 * Should be used only if
	 * <code>SipConstant.getReasonPhrase(statusCode)</code> return a phrase
	 * enough understandable.
	 * 
	 * @param statusCode
	 */
	public XcapException(int statusCode)
	{
		this(statusCode, null);
	}

	public void addHeader(String name, String value)
	{
		headerName = name;
		headerValue = value;
	}

	public int getStatusCode()
	{
		return status;
	}

	public String getReasonPhrase()
	{
		return reason;
	}

	public void sendResponse(HttpServletResponse response)
	{
		try
		{
			if (headerName != null)
			{
				response.addHeader(headerName, headerValue);
			}

			if (content != null)
			{
				response.setContentType(contentType);
				response.getOutputStream().write(content);
				response.getOutputStream().close();
			}

			if (reason != null)
			{
				response.setStatus(status, reason);
			} else
			{
				response.setStatus(status);
			}
		} catch (IOException e)
		{
			log.error("Unable to send error response", e);
		}
	}

	public void setLevel(Level level)
	{
		this.level = level;
	}

	public Level getLogLevel()
	{
		return level;
	}

	public boolean shouldShowStackTrace()
	{
		return showStackTrace;
	}

	public void setContent(String contentType, byte[] content)
	{
		this.contentType = contentType;
		this.content = content;
	}

	public static XcapException newInternalError(Throwable e)
	{
		return new XcapException(e.getMessage(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
	}
}
