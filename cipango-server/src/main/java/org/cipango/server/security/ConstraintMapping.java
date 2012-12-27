/// ========================================================================
// Copyright 2011-2012 NEXCOM Systems
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
package org.cipango.server.security;

import java.util.List;

public class ConstraintMapping
{
    private List<String> _methods;

    private List<String> _servletNames;

    private Constraint _constraint;

    /* ------------------------------------------------------------ */
    /**
     * @return Returns the constraint.
     */
    public Constraint getConstraint()
    {
        return _constraint;
    }

    /* ------------------------------------------------------------ */
    /**
     * @param constraint The constraint to set.
     */
    public void setConstraint(Constraint constraint)
    {
        _constraint = constraint;
    }

	public List<String> getMethods()
	{
		return _methods;
	}

	public void setMethods(List<String> methods)
	{
		_methods = methods;
	}

	public List<String> getServletNames()
	{
		return _servletNames;
	}

	public void setServletNames(List<String> servletNames)
	{
		_servletNames = servletNames;
	}

}
