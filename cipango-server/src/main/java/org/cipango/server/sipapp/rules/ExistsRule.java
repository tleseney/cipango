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

package org.cipango.server.sipapp.rules;

import javax.servlet.sip.SipServletRequest;

public class ExistsRule extends RequestRule implements MatchingRule {

    public ExistsRule(String var) 
    {
        super(var);
    }
    
    public boolean matches(SipServletRequest request) 
    {
    	return getValue(request) != null;
    }

    public String getExpression() 
    {
        return "(" + getVarName() + " != null)";
    }
}
