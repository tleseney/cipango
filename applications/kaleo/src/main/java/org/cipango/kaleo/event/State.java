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

package org.cipango.kaleo.event;

/**
 * State information for a resource.
 */
public class State
{
	private String _contentType;
	private Object _content;
	
	public State(String contentType, Object content)
	{
		setContent(contentType, content);
	}
	
	public void setContent(String contentType, Object content)
	{
		_contentType = contentType;
		_content = content;
	}
	
	public Object getContent()
	{
		return _content;
	}
	
	public String getContentType()
	{
		return _contentType;
	}
}
