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

import java.util.ArrayList;

import javax.servlet.sip.SipServletRequest;

public class AndRule implements MatchingRule 
{
    private ArrayList<MatchingRule> _criteria = new ArrayList<MatchingRule>();
    
    public AndRule() { }
    
    public void addCriterion(MatchingRule c) 
    {
       _criteria.add(c); 
    }
    
    public boolean matches(SipServletRequest request) 
    {
        for (int i = 0; i < _criteria.size(); i++) 
        {
            MatchingRule c = (MatchingRule) _criteria.get(i);
            if (!c.matches(request)) 
                return false;
        }
        return true;
    }

    public String getExpression() 
    {
        StringBuffer sb = new StringBuffer("(");
        boolean first = true;
         
        for (int i = 0; i < _criteria.size(); i++) 
        {
            MatchingRule c = (MatchingRule) _criteria.get(i);
            if (first) 
                first = false;
            else 
                sb.append(" and ");
            sb.append(c.getExpression());
        }
        sb.append(")");
        return sb.toString();
    }
}
