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

package org.cipango.server.sipapp.rules.request;

import javax.servlet.sip.Address;
import javax.servlet.sip.SipServletRequest;

public class Uri implements Extractor 
{
	private static final int REQUEST = 1;
	private static final int ADDRESS = 2;
	
    private int _inputType;
    
	public Uri(String token) 
    {
		if (token.equals("request")) 
			_inputType = REQUEST;
		else if (token.equals("from"))
			_inputType = ADDRESS;
		else if (token.equals("to")) 
			_inputType = ADDRESS;
		else if (token.equals("route")) 
			_inputType = ADDRESS;
		else 
			throw new IllegalArgumentException("Invalid expression: uri after " + token);
	}
    
	public Object extract(Object input) 
    {
		if (_inputType == REQUEST) 
			return ((SipServletRequest) input).getRequestURI();
		else 
			return ((Address) input).getURI();
	}
}
