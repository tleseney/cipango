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

import java.util.Map;

import org.cipango.kaleo.xcap.XcapResourceImpl.NodeType;
import org.cipango.kaleo.xcap.dao.XmlResource;



public interface XcapResource {
			
	/**
	 * Returns the selected node.
	 * @return the selected node.
	 */
	public XmlResource getSelectedResource();
	
	public void setDocument(XmlResource resource);
	
	/**
	 * Returns the action requested.
	 * i.e. the HTTP method (GET, PUT or DELETE).
	 * @return the action requested.
	 */
	public String getAction();

	/**
	 * Returns <code>true</code> if all document has been selected.
	 * i.e. if there is no node selector.
	 * @return <code>true</code> if all document has been selected.
	 */
	public boolean isAllDocument();
	
	public String getMimeType();
	
	/**
	 * returns <code>true</code> if it is a creation.
	 * (new document, element or attribute).
	 * @return <code>true</code> if it is a creation.
	 */
	public boolean isCreation();

	public XcapUri getXcapUri();

	public XcapResourceProcessor getProcessor();
		
	public String getPreviousEtag();
	
	public String getNewEtag();
	
	public NodeType getNodeType();

	public String getParentPath();

	public String getNodeName();
	
	public XmlResource getDocument();
	
	public Map<String, String> getNamespaceContext();

}
