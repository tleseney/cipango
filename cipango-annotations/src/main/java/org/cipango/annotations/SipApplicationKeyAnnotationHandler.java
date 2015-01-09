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

import org.eclipse.jetty.annotations.AnnotationParser.ClassInfo;
import org.eclipse.jetty.annotations.AnnotationParser.FieldInfo;
import org.eclipse.jetty.annotations.AnnotationParser.MethodInfo;
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
	
	public void handle(ClassInfo info, String annotationName)
	{
		if (annotationName == null || !"javax.servlet.sip.annotation.SipApplicationKey".equals(annotationName))
            return;
		LOG.warn("@SipApplicationKey annotation not applicable to classes: " + info.getClassName());
	}

	public void handle(MethodInfo info, String annotationName)
	{
		if (annotationName == null || !"javax.servlet.sip.annotation.SipApplicationKey".equals(annotationName))
            return;
		addAnnotation(new SipApplicationKeyAnnotation(_context, info.getClassInfo().getClassName()));
	}

	public void handle(FieldInfo info, String annotationName)
	{
		if (annotationName == null || !"javax.servlet.sip.annotation.SipApplicationKey".equals(annotationName))
            return;
		LOG.warn ("@SipApplicationKey annotation not applicable for fields: "
				+info.getClassInfo().getClassName()+"."+info.getFieldName());
	}

}
