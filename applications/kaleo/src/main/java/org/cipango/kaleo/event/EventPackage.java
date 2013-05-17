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

/**
 * Generic event package. 
 * 
 * @see <a href="http://www.faqs.org/rfcs/rfc3265.html">RFC 3265</a>
 * 
 */
package org.cipango.kaleo.event;

import java.util.List;

public interface EventPackage<T extends EventResource>
{
	String getName();

	int getMinExpires();
	int getMaxExpires();
	int getDefaultExpires();

	List<String> getSupportedContentTypes();

	T get(String uri);

	ContentHandler<?> getContentHandler(String contentType);
}
