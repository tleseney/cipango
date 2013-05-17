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

package org.cipango.kaleo.location.event;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.apache.xmlbeans.XmlOptions;
import org.cipango.kaleo.event.ContentHandler;

public class ReginfoHandler implements ContentHandler<ReginfoDocument>
{
	private XmlOptions _xmlOptions;
	
	public ReginfoHandler()
	{		
		_xmlOptions = new XmlOptions();
		_xmlOptions.setUseDefaultNamespace();
		_xmlOptions.setSavePrettyPrint(); 
	}
	
	public byte[] getBytes(ReginfoDocument content) throws Exception
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		content.save(out, _xmlOptions);
		return out.toByteArray();
	}

	public ReginfoDocument getContent(byte[] b) throws Exception 
	{
		return ReginfoDocument.Factory.parse(new ByteArrayInputStream(b));
	}
}
