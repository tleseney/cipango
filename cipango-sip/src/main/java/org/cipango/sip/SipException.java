// ========================================================================
// Copyright 2008-2012 NEXCOM Systems
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

package org.cipango.sip;

import java.io.IOException;

import javax.servlet.sip.SipServletResponse;

public class SipException extends IOException
{
	private static final long serialVersionUID = 2490122181651498938L;

	public static final SipException badRequest()
	{
		return new SipException(SipServletResponse.SC_BAD_REQUEST);
	}
	
	public static final SipException serverError()
	{
		return new SipException(SipServletResponse.SC_SERVER_INTERNAL_ERROR);
	}
	
	private int _status;
	private String _reason;
	
	public SipException(int status, String reason)
	{
		_status = status;
		_reason = reason;
	}
	
	public SipException(int status)
	{
		this(status, null);
	}
	
	public int getStatus()
	{
		return _status;
	}
	
	public String getReason()
	{
		return _reason;
	}
}
