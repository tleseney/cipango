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

public class Util
{
	public static boolean isEmpty(String s)
	{
		return s == null || "".equals(s.trim());
	}
	
	@SuppressWarnings("rawtypes")
	public static boolean isServletType (Class c)
    {    
        boolean isServlet = false;
        if (org.eclipse.jetty.annotations.Util.isServletType(c) ||
        		javax.servlet.sip.SipApplicationSessionAttributeListener.class.isAssignableFrom(c) || 
                javax.servlet.sip.SipApplicationSessionActivationListener.class.isAssignableFrom(c) ||
                javax.servlet.sip.SipApplicationSessionListener.class.isAssignableFrom(c) ||
                javax.servlet.sip.SipErrorListener.class.isAssignableFrom(c) ||
                javax.servlet.sip.SipServletListener.class.isAssignableFrom(c) ||
                javax.servlet.sip.SipSessionListener.class.isAssignableFrom(c) ||
                javax.servlet.sip.SipSessionAttributeListener.class.isAssignableFrom(c) ||
                javax.servlet.sip.SipSessionActivationListener.class.isAssignableFrom(c)||
                javax.servlet.sip.TimerListener.class.isAssignableFrom(c))

                isServlet=true;
        
        return isServlet;  
    }
}
