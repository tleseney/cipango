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

package org.cipango.server.util;

import javax.servlet.sip.TelURL;

public class TelURLProxy extends URIProxy implements TelURL
{
    static final long serialVersionUID = -3739264693978176010L;
    
	private TelURL _telURL;

	public TelURLProxy(TelURL telURL)
	{
		super(telURL);
		_telURL = telURL;
	}
	
	public String getPhoneContext()
	{
		return _telURL.getPhoneContext();
	}

	public String getPhoneNumber()
	{
		return _telURL.getPhoneNumber();
	}

	public boolean isGlobal()
	{
		return _telURL.isGlobal();
	}

	public void setPhoneNumber(String arg0)
	{
		_telURL.setPhoneNumber(arg0);
	}

	public void setPhoneNumber(String arg0, String arg1)
	{
		_telURL.setPhoneNumber(arg0, arg1);
	}
}
