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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import javax.annotation.Resource;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipSessionsUtil;
import javax.servlet.sip.TimerService;

import org.cipango.plus.sipapp.SipResourceDecorator;
import org.cipango.server.sipapp.SipAppContext;
import org.eclipse.jetty.plus.annotation.Injection;
import org.eclipse.jetty.plus.annotation.InjectionCollection;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.webapp.WebAppContext;

public class ResourceAnnotationHandler extends org.eclipse.jetty.annotations.ResourceAnnotationHandler
{
	private static final Logger LOG = Log.getLogger(ResourceAnnotationHandler.class);
	
	public ResourceAnnotationHandler(WebAppContext wac)
	{
		super(wac);
	}

	protected SipAppContext getSipAppCtx()
	{
		return _context.getBean(SipAppContext.class);
	}

	@Override
	@SuppressWarnings("rawtypes")
	public void doHandle(Class clazz)
	{
		if (Util.isServletType(clazz))
		{
			handleClass(clazz);

			Method[] methods = clazz.getDeclaredMethods();
			for (int i = 0; i < methods.length; i++)
				handleMethod(clazz, methods[i]);
			Field[] fields = clazz.getDeclaredFields();
			// For each field, get all of it's annotations
			for (int i = 0; i < fields.length; i++)
				handleField(clazz, fields[i]);
		}
	}

	@Override
	@SuppressWarnings("rawtypes")
	public void handleField(Class clazz, Field field)
	{
		Resource resource = (Resource) field.getAnnotation(Resource.class);
		if (resource != null)
		{
			String jndiName = getSipResourceJndiName(field);
			if (jndiName != null)
			{
				// The Sip resources are injected in JNDI latter (in plus config), so could not
				// check for availability.

				// JavaEE Spec 5.2.3: Field cannot be static
				if (Modifier.isStatic(field.getModifiers()))
				{
					LOG.warn("Skipping Resource annotation on " + clazz.getName() + "." + field.getName()
							+ ": cannot be static");
					return;
				}

				// JavaEE Spec 5.2.3: Field cannot be final
				if (Modifier.isFinal(field.getModifiers()))
				{
					LOG.warn("Skipping Resource annotation on " + clazz.getName() + "." + field.getName()
							+ ": cannot be final");
					return;
				}

				InjectionCollection injections = (InjectionCollection) _context
						.getAttribute(InjectionCollection.INJECTION_COLLECTION);
				if (injections == null)
				{
					injections = new InjectionCollection();
					_context.setAttribute(InjectionCollection.INJECTION_COLLECTION, injections);
				}
				Injection injection = new Injection();
				injection.setTarget(clazz, field, field.getType());
				injection.setJndiName(jndiName);
				injections.add(injection);

			}
		}
		else
			super.handleField(clazz, field);
	}

	private String getSipResourceJndiName(Field field)
	{
		if (field.getType() == SipFactory.class)
		{
			LOG.info("Detect SipFactory Resource from annotation");
			return SipResourceDecorator.JNDI_SIP_PREFIX + getSipAppCtx().getName()
					+ SipResourceDecorator.JNDI_SIP_FACTORY_POSTFIX;
		}
		else if (field.getType() == SipSessionsUtil.class)
		{
			LOG.info("Detect SipSessionsUtil Resource from annotation");
			return SipResourceDecorator.JNDI_SIP_PREFIX + getSipAppCtx().getName()
					+ SipResourceDecorator.JNDI_SIP_SESSIONS_UTIL_POSTFIX;
		}
		else if (field.getType() == TimerService.class)
		{
			LOG.info("Detect TimerService Resource from annotation");
			return SipResourceDecorator.JNDI_SIP_PREFIX + getSipAppCtx().getName()
					+ SipResourceDecorator.JNDI_TIMER_SERVICE_POSTFIX;
		}
		else
		{
			return null;
		}
	}
}
