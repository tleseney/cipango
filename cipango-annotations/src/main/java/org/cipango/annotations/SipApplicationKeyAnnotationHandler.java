// ========================================================================
// Copyright 2010 NEXCOM Systems
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

import java.util.List;

import org.eclipse.jetty.annotations.AbstractDiscoverableAnnotationHandler;
import org.eclipse.jetty.annotations.AnnotationParser.Value;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.webapp.WebAppContext;

public class SipApplicationKeyAnnotationHandler extends AbstractDiscoverableAnnotationHandler
{
	private static final Logger LOG = Log.getLogger(SipApplicationKeyAnnotationHandler.class);

	public SipApplicationKeyAnnotationHandler(WebAppContext context)
	{
		super(context);
	}

	public void handleClass(String className, int version, int access, String signature, String superName,
			String[] interfaces, String annotation, List<Value> values)
	{
		LOG.warn("@SipApplicationKey annotation not applicable to classes: " + className);
	}

	public void handleMethod(String className, String methodName, int access, String desc, String signature,
			String[] exceptions, String annotation, List<Value> values)
	{
		addAnnotation(new SipApplicationKeyAnnotation(_context, className));
	}
	
	public void handleField(String className, String fieldName, int access, String fieldType,
			String signature, Object value, String annotation, List<Value> values)
	{
		LOG.warn("@SipApplication annotation not applicable for fields: " + className + "." + fieldName);
	}


}
