// ========================================================================
// Copyright (c) 2008-2009 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// All rights reserved. This program and the accompanying materials
// are made available under the terms of the Eclipse Public License v1.0
// and Apache License v2.0 which accompanies this distribution.
// The Eclipse Public License is available at 
// http://www.eclipse.org/legal/epl-v10.html
// The Apache License v2.0 is available at
// http://www.opensource.org/licenses/apache2.0.php
// You may elect to redistribute this code under either of these licenses. 
// ========================================================================

package org.cipango.server.security;

import org.eclipse.jetty.security.UserDataConstraint;

/* ------------------------------------------------------------ */
/**
 * Describe an auth and/or data constraint.
 * 
 * 
 */
public class Constraint extends org.eclipse.jetty.util.security.Constraint
{
		
	private boolean _proxyMode;
	
	public boolean isProxyMode()
	{
		return _proxyMode;
	}

	public void setProxyMode(boolean proxyMode)
	{
		_proxyMode = proxyMode;
	}
	
	public UserDataConstraint getUserDataConstraint()
	{
		return UserDataConstraint.get(getDataConstraint());
	}
	
	public void setUserDataConstraint(UserDataConstraint constraint)
	{
		setDataConstraint(constraint.ordinal());
	}
	

}
