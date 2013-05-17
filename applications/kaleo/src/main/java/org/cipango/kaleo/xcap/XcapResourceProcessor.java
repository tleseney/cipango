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

import org.iso_relax.verifier.Schema;


public interface XcapResourceProcessor {

	public String getAuid();
	
	public Schema getXsdSchema();
	
	public Map<String, String> getNamespaceContext();
	
	public String getMimeType();
	
	public void processResource(XcapResource resource);
	
	/**
	 * Checks for any uniqueness constraints identified by
     * the application usage.
	 * 
	 * @see XCAP 8.2.5. Validation
	 * 
	 * @param resource The resource to validate.
	 * @return <code>true</code> if the resource has been validated.
	 */
	public boolean validateResource(XcapResource resource) throws XcapException;
	
	
	/**
	 * Returns the default namespace prefix.
	 * (The prefix used when none is defined)
	 * @return the default namespace prefix.
	 */
	public String getDefaultNamespacePrefix();
}
