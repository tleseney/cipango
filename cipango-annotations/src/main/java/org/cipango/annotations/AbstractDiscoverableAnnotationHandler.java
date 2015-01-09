// ========================================================================
// Copyright 2010-2015 NEXCOM Systems
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
package org.cipango.annotations;

import org.cipango.server.sipapp.SipAppContext;
import org.eclipse.jetty.webapp.DiscoveredAnnotation;
import org.eclipse.jetty.webapp.WebAppContext;


public class AbstractDiscoverableAnnotationHandler extends org.eclipse.jetty.annotations.AbstractDiscoverableAnnotationHandler 
{

	public AbstractDiscoverableAnnotationHandler(WebAppContext context) 
	{
		super(context);
	}

	@Override
	public void addAnnotation(DiscoveredAnnotation a) 
	{
		_context.getBean(SipAppContext.class).getMetaData().addDiscoveredAnnotation(a);
	}

}
