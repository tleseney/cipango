// ========================================================================
// Copyright 2006-2015 NEXCOM Systems
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
package org.cipango.diameter.app;

import org.cipango.diameter.api.DiameterErrorListener;
import org.cipango.diameter.api.DiameterListener;
import org.eclipse.jetty.servlet.ServletContextHandler.Decorator;
import org.eclipse.jetty.webapp.WebAppContext;

public class DiameterDecorator implements Decorator
{

	private DiameterContext _context;
	private WebAppContext _appContext;
	
	public DiameterDecorator(DiameterContext context, WebAppContext appContext)
	{
		_context = context;
		_appContext = appContext;
	}

	@Override
	public <T> T decorate(T o) 
	{
		if (o instanceof DiameterListener)
			_context.addListener(_appContext, (DiameterListener) o);
		if (o instanceof DiameterErrorListener)
			_context.addErrorListener(_appContext, (DiameterErrorListener) o);
		return o;
	}

	@Override
	public void destroy(Object o) 
	{
		if (o instanceof DiameterListener)
			_context.removeListener(_appContext, (DiameterListener) o);
		if (o instanceof DiameterErrorListener)
			_context.removeErrorListener(_appContext, (DiameterErrorListener) o);
		
	}

}
