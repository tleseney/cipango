// ========================================================================
// Copyright 2006-2013 NEXCOM Systems
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
package org.cipango.diameter.node;

import java.io.IOException;

import javax.servlet.sip.SipApplicationSession;

import org.cipango.diameter.ResultCode;
import org.cipango.diameter.api.DiameterServletAnswer;
import org.cipango.diameter.api.DiameterSession;

public class DiameterAnswer extends DiameterMessage implements DiameterServletAnswer
{
	private DiameterRequest _request;
	private ResultCode _resultCode;
	
	public DiameterAnswer()
	{	
	}
	
	public DiameterAnswer(DiameterRequest request, ResultCode resultCode)
	{
		super(request);
		_request = request;
		_resultCode = resultCode;
		
		_avps.add(_resultCode.getAVP());
	}
	
	public void setRequest(DiameterRequest request)
	{
		_request = request;
	}
	
	public DiameterRequest getRequest()
	{
		return _request;
	}
	
	public boolean isRequest()
	{
		return false;
	}
	
	public ResultCode getResultCode()
	{
		return _resultCode;
	}
	
	public void setResultCode(ResultCode resultCode)
	{
		_resultCode = resultCode;
	}
	
	public void send() throws IOException
	{
		_request.getConnection().write(this);
	}
	
	@Override
	public SipApplicationSession getApplicationSession()
	{
		if (_request != null)
			return _request.getApplicationSession();
		return null;
	}

	@Override
	public DiameterSession getSession(boolean create)
	{
		if (_request != null)
			return _request.getSession(create);
		return _session;
	}
}