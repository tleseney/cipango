// ========================================================================
// Copyright 2009 NEXCOM Systems
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
package org.cipango.kaleo.xcap.dao;

import java.io.IOException;
import java.util.Collection;

import org.cipango.kaleo.xcap.XcapException;
import org.cipango.kaleo.xcap.XcapResource;
import org.cipango.kaleo.xcap.XcapResourceProcessor;
import org.cipango.kaleo.xcap.XcapUri;

public interface XcapDao
{

	public void init(Collection<XcapResourceProcessor> processors) throws Exception;

	public XmlResource getDocument(XcapUri uri, boolean create)
			throws XcapException;

	public XmlResource getNode(XcapResource resource) throws XcapException;

	public XmlResource getNode(XcapResource resource, String nodeSelector) throws XcapException;

	public void update(XcapResource xcapResource, String content)
			throws XcapException;

	public void save(XcapResource resource) throws XcapException, IOException;

	public void delete(XcapResource resource) throws XcapException;

	public String getFirstExistAncestor(XcapUri uri);
}