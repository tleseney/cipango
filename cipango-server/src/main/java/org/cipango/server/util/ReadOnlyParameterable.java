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

import javax.servlet.sip.Parameterable;

public class ReadOnlyParameterable extends ParameterableProxy
{
    static final long serialVersionUID = 8976005668858290469L;
    
	public ReadOnlyParameterable(Parameterable parameterable)
	{
		super(parameterable);
	}

	@Override
	public void removeParameter(String name)
	{
		throw new IllegalStateException("Read-only");
	}

	@Override
	public void setParameter(String name, String value)
	{
		throw new IllegalStateException("Read-only");
	}

	@Override
	public void setValue(String value)
	{
		throw new IllegalStateException("Read-only");
	}
}
