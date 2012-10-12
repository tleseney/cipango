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

import java.text.ParseException;

import javax.servlet.sip.URI;


public abstract class URIFactory
{
	public static URI parseURI(String uri) throws ParseException 
	{
		if (uri.startsWith("sip:") || uri.startsWith("sips:"))
			return new SipURIImpl(uri);
		else if (uri.startsWith("tel:") || uri.startsWith("fax:"))
			return new TelURLImpl(uri);
		else
			return new URIImpl(uri);
	}
	
	private URIFactory() { }
}
