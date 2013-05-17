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

package org.cipango.kaleo.presence.pidf;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;

import org.apache.xmlbeans.XmlOptions;
import org.cipango.kaleo.event.ContentHandler;

public class PidfHandler implements ContentHandler<PresenceDocument>
{
	private XmlOptions _xmlOptions;
	
	public PidfHandler()
	{
		HashMap<String, String> suggestedPrefixes = new HashMap<String, String>();
		suggestedPrefixes.put("urn:ietf:params:xml:ns:pidf:data-model", "dm");
		suggestedPrefixes.put("urn:ietf:params:xml:ns:pidf:rpid", "rpid");
		suggestedPrefixes.put("urn:ietf:params:xml:ns:pidf:cipid", "c");
		
		_xmlOptions = new XmlOptions();
		_xmlOptions.setUseDefaultNamespace();
		_xmlOptions.setSaveSuggestedPrefixes(suggestedPrefixes);
		
		/*
		_xmlOptions.setSaveImplicitNamespaces(suggestedPrefixes);
		_xmlOptions.setSaveAggressiveNamespaces();
		_xmlOptions.setSaveNamespacesFirst(); */
	}
	
	public PresenceDocument getContent(byte[] b) throws Exception
	{
		return PresenceDocument.Factory.parse(new ByteArrayInputStream(b));
	}
	
	public byte[] getBytes(PresenceDocument content) throws Exception
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		content.save(out, _xmlOptions);
		return out.toByteArray();
	}
}
