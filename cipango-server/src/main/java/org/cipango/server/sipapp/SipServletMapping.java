// ========================================================================
// Copyright 2008-2012 NEXCOM Systems
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

package org.cipango.server.sipapp;

import org.cipango.server.sipapp.rules.MatchingRule;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;

@ManagedObject("Servlet Mapping")
public class SipServletMapping 
{
    private String _servletName;
    private MatchingRule _rule;
    
    @ManagedAttribute(value="Servlet Name", readonly=true)
	public String getServletName() 
    {
		return _servletName;
	}
	
    @ManagedAttribute(value="Matching rule", readonly=true)
	public MatchingRule getMatchingRule() 
    {
		return _rule;
	}
	
    @ManagedAttribute(value="Matching rule (human readable)", readonly=true)
	public String getMatchingRuleExpression() 
    {
		return _rule.getExpression();
	}
	
	public void setServletName(String servletName)
    {
		_servletName = servletName;
	}
	
	public void setMatchingRule(MatchingRule rule) 
    {
		_rule = rule;
	}
	
	@Override
	public String toString()
	{
		return "[" + getMatchingRuleExpression() + "]==>" + getServletName();
	}
}
