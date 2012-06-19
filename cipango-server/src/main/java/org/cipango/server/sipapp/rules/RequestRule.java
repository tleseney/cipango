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
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.sip.SipServletRequest;

import org.cipango.server.sipapp.rules.request.*;


public abstract class RequestRule implements MatchingRule 
{
    private String _varName;
    private List<Extractor> _extractors;
    
    protected RequestRule(String varName) 
    {
    	_varName = varName;
    	_extractors = new ArrayList<Extractor>();
    	StringTokenizer st = new StringTokenizer(varName, ".");
		String lastToken = st.nextToken();
		if (!lastToken.equals("request"))
			throw new IllegalArgumentException("Expression does not start with request: " + varName);
		
		while (st.hasMoreTokens()) 
        {
			String token = st.nextToken();
			if (token.equals("from")) 
				_extractors.add(new From(lastToken));
			else if (token.equals("uri")) 
				_extractors.add(new Uri(lastToken));
			else if (token.equals("method")) 
				_extractors.add(new Method(lastToken));
			else if (token.equals("user")) 
				_extractors.add(new User(lastToken));
			else if (token.equals("scheme")) 
				_extractors.add(new Scheme(lastToken));
			else if (token.equals("host")) 
				_extractors.add(new Host(lastToken));
			else if (token.equals("port")) 
				_extractors.add(new Port(lastToken));
			else if (token.equals("tel")) 
				_extractors.add(new Tel(lastToken));
			else if (token.equals("display-name")) 
				_extractors.add(new DisplayName(lastToken));
			else if (token.equals("to")) 
				_extractors.add(new To(lastToken));
            else if (token.equals("route"))
                _extractors.add(new Route(lastToken));
			else if (token.equals("param")) 
            {
				if (!st.hasMoreTokens()) 
					throw new IllegalArgumentException("No param name: " + varName);
				
				String param = st.nextToken();
				_extractors.add(new Param(lastToken, param));
				if (st.hasMoreTokens()) 
					throw new IllegalArgumentException("Invalid var: " + st.nextToken() + " in " + varName);
				
			}
            else 
                throw new IllegalArgumentException("Invalid property: " + token + " in " + varName);
			lastToken = token;
		}
    }
    
    public String getValue(SipServletRequest request)
    {
    	Object o = request;
		for (int i = 0; i < _extractors.size(); i++) 
        {
			Extractor e = (Extractor) _extractors.get(i);
			o = e.extract(o);
			if (o == null) return null;
		}
		return o.toString();
    }
    
    public String getVarName() 
    {
    	return _varName;
    }
}
