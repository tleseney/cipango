// ========================================================================
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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import org.cipango.server.SipConnector;
import org.cipango.server.SipRequest;
import org.cipango.server.servlet.SipServletHolder;
import org.eclipse.jetty.security.UserDataConstraint;
import org.eclipse.jetty.server.UserIdentity;

public class ConstraintSecurityHandler extends SipSecurityHandler<RoleInfo>
{

    private final List<ConstraintMapping> _constraintMappings= new CopyOnWriteArrayList<ConstraintMapping>();
    private final Set<String> _roles = new CopyOnWriteArraySet<String>();
    private boolean _strict = true;
    
    private Map<String, Map<String, RoleInfo>> _servletsMap = new HashMap<String, Map<String,RoleInfo>>();

    
    /* ------------------------------------------------------------ */
    /** Get the strict mode.
     * @return true if the security handler is running in strict mode.
     */
    public boolean isStrict()
    {
        return _strict;
    }

    /* ------------------------------------------------------------ */
    /** Set the strict mode of the security handler.
     * <p>
     * When in strict mode (the default), the full servlet specification
     * will be implemented.
     * If not in strict mode, some additional flexibility in configuration
     * is allowed:<ul>
     * <li>All users do not need to have a role defined in the deployment descriptor
     * <li>The * role in a constraint applies to ANY role rather than all roles defined in
     * the deployment descriptor.
     * </ul>
     *
     * @param strict the strict to set
     * @see #setRoles(Set)
     * @see #setConstraintMappings(List, Set)
     */
    public void setStrict(boolean strict)
    {
        _strict = strict;
    }

    /* ------------------------------------------------------------ */
    /**
     * @return Returns the constraintMappings.
     */
    public List<ConstraintMapping> getConstraintMappings()
    {
        return _constraintMappings;
    }

    /* ------------------------------------------------------------ */
    public Set<String> getRoles()
    {
        return _roles;
    }

    /* ------------------------------------------------------------ */
    /**
     * Process the constraints following the combining rules in Servlet 3.0 EA
     * spec section 13.7.1 Note that much of the logic is in the RoleInfo class.
     *
     * @param constraintMappings
     *            The constraintMappings to set, from which the set of known roles
     *            is determined.
     */
    public void setConstraintMappings(List<ConstraintMapping> constraintMappings)
    {
        setConstraintMappings(constraintMappings,null);
    }

    /* ------------------------------------------------------------ */
    /**
     * Process the constraints following the combining rules in Servlet 3.0 EA
     * spec section 13.7.1 Note that much of the logic is in the RoleInfo class.
     *
     * @param constraintMappings
     *            The constraintMappings to set.
     * @param roles The known roles (or null to determine them from the mappings)
     */
    public void setConstraintMappings(List<ConstraintMapping> constraintMappings, Set<String> roles)
    {
        if (isStarted())
            throw new IllegalStateException("Started");
        _constraintMappings.clear();
        _constraintMappings.addAll(constraintMappings);

        if (roles==null)
        {
            roles = new HashSet<String>();
            for (ConstraintMapping cm : constraintMappings)
            {
                String[] cmr = cm.getConstraint().getRoles();
                if (cmr!=null)
                {
                    for (String r : cmr)
                        if (!"*".equals(r))
                            roles.add(r);
                }
            }
        }
        setRoles(roles);
    }

    /* ------------------------------------------------------------ */
    /**
     * Set the known roles.
     * This may be overridden by a subsequent call to {@link #setConstraintMappings(ConstraintMapping[])} or
     * {@link #setConstraintMappings(List, Set)}.
     * @see #setStrict(boolean)
     * @param roles The known roles (or null to determine them from the mappings)
     */
    public void setRoles(Set<String> roles)
    {
        if (isStarted())
            throw new IllegalStateException("Started");

        _roles.clear();
        _roles.addAll(roles);
    }



    /* ------------------------------------------------------------ */
    /**
     * @see org.eclipse.jetty.security.ConstraintAware#addConstraintMapping(org.eclipse.jetty.security.ConstraintMapping)
     */
    public void addConstraintMapping(ConstraintMapping mapping)
    {
        _constraintMappings.add(mapping);
        if (mapping.getConstraint()!=null && mapping.getConstraint().getRoles()!=null)
            for (String role :  mapping.getConstraint().getRoles())
                addRole(role);
    }

    /* ------------------------------------------------------------ */
    /**
     * @see org.eclipse.jetty.security.ConstraintAware#addRole(java.lang.String)
     */
    public void addRole(String role)
    {
        _roles.add(role);
    }
    
    @Override
    protected void doStart() throws Exception
    {
        _servletsMap.clear();
        if (_constraintMappings!=null)
        {
            for (ConstraintMapping mapping : _constraintMappings)
            {
                processConstraintMapping(mapping);
            }
        }
        
        super.doStart();
    }

    @Override
    protected void doStop() throws Exception
    {
    	_servletsMap.clear();
        _constraintMappings.clear();
        _roles.clear();
        super.doStop();
    }
    
	@Override
	protected RoleInfo prepareConstraintInfo(SipServletHolder holder, SipRequest request)
	{
		Map<String, RoleInfo> mappings = _servletsMap.get(holder.getName());
		// FIXME merge in case method && servlets

        if (mappings != null)
        {
            String method = request.getMethod();
            RoleInfo roleInfo = mappings.get(method);

            if (roleInfo == null)
                roleInfo = mappings.get(null);

            return roleInfo;
        }
        mappings = _servletsMap.get(null);

        if (mappings != null)
            return mappings.get(request.getMethod());        
        
        return null;
	}

	@Override
	protected boolean checkUserDataPermissions(SipServletHolder holder, SipRequest request,
			RoleInfo constraintInfo) throws IOException
	{
	    if (constraintInfo == null)
	        return true;
	
	    if (constraintInfo.isForbidden())
	        return false;
	    	

	    UserDataConstraint dataConstraint = constraintInfo.getUserDataConstraint();
	    if (dataConstraint == null || dataConstraint == UserDataConstraint.None)
	    {
	        return true;
	    }
	    
	    SipConnector connector = null;
	    
	    if (request.getConnection() != null)
	    	connector = request.getConnection().getConnector();
	    

	    return true;
	
	    /* TODO
	    if (dataConstraint == UserDataConstraint.Integral)
	    {
	        if (connector.isIntegral(request))
	            return true;
	        if (connector.getConfidentialPort() > 0)
	        {
	            String url = connector.getIntegralScheme() + "://" + request.getServerName() + ":" + connector.getIntegralPort() + request.getRequestURI();
	            if (request.getQueryString() != null)
	                url += "?" + request.getQueryString();
	            response.setContentLength(0);
	            response.sendRedirect(url);
	        }
	        else
	            response.sendError(Response.SC_FORBIDDEN,"!Integral");
	
	        request.setHandled(true);
	        return false;
	    }
	    else if (dataConstraint == UserDataConstraint.Confidential)
	    {
	        if (connector.isConfidential(request))
	            return true;
	
	        if (connector.getConfidentialPort() > 0)
	        {
	            String url = connector.getConfidentialScheme() + "://" + request.getServerName() + ":" + connector.getConfidentialPort()
	                    + request.getRequestURI();
	            if (request.getQueryString() != null)
	                url += "?" + request.getQueryString();
	
	            response.setContentLength(0);
	            response.sendRedirect(url);
	        }
	        else
	            response.sendError(Response.SC_FORBIDDEN,"!Confidential");
	
	        request.setHandled(true);
	        return false;
	        
	    }
	    else
	    {
	        throw new IllegalArgumentException("Invalid dataConstraint value: " + dataConstraint);
	    }
	    */
	    
	}

	@Override
	protected boolean isAuthMandatory(SipRequest baseRequest, RoleInfo constraintInfo)
	{
		 if (constraintInfo == null)
	           return false;
	     return constraintInfo.isChecked();
	}

	@Override
	protected boolean checkSipResourcePermissions(SipServletHolder holder, SipRequest request,
			RoleInfo constraintInfo, UserIdentity userIdentity) throws IOException
	{
		if (constraintInfo == null)
        {
            return true;
        }

        if (!constraintInfo.isChecked())
        {
            return true;
        }

        if (constraintInfo.isAnyRole() /*&& request.getAuthType()!=null*/)
            return true;

        for (String role : constraintInfo.getRoles())
        {
            if (userIdentity.isUserInRole(role, null))
                return true;
        }
        return false;
	}

	@Override
	protected boolean isProxyMode(SipRequest baseRequest, RoleInfo constraintInfo)
	{
		if (constraintInfo == null)
            return true;

		return constraintInfo.isProxyMode();
	}
	
	protected void processConstraintMapping(ConstraintMapping mapping)
    {	
		if (mapping.getServletNames() == null)
		{
			for (String method : mapping.getMethods())
				processMapping(null, method, mapping.getConstraint());
		}
		else
		{
			for (String servletName : mapping.getServletNames())
			{
				if (mapping.getMethods() == null)
					processMapping(servletName, null, mapping.getConstraint());
				else
				{
					for (String method : mapping.getMethods())
						processMapping(servletName, method, mapping.getConstraint());
				}
			}
		}
    }
		
	private void processMapping(String servletName, String method, Constraint constraint)
	{
        Map<String, RoleInfo> mappings = (Map<String, RoleInfo>)_servletsMap.get(servletName);
        if (mappings == null)
        {
            mappings = new HashMap<String, RoleInfo>();
            _servletsMap.put(servletName,mappings);
        }
        RoleInfo allMethodsRoleInfo = mappings.get(null);
        if (allMethodsRoleInfo != null && allMethodsRoleInfo.isForbidden())
            return;


        RoleInfo roleInfo = mappings.get(method);
        if (roleInfo == null)
        {
            roleInfo = new RoleInfo();
            mappings.put(method,roleInfo);
            if (allMethodsRoleInfo != null)
            {
                roleInfo.combine(allMethodsRoleInfo);
            }
        }
        if (roleInfo.isForbidden())
            return;

        boolean forbidden = constraint.isForbidden();
        roleInfo.setForbidden(forbidden);
        if (forbidden)
        {
            if (method == null)
            {
                mappings.clear();
                mappings.put(null,roleInfo);
            }
        }
        else
        {
            UserDataConstraint userDataConstraint = UserDataConstraint.get(constraint.getDataConstraint());
            roleInfo.setUserDataConstraint(userDataConstraint);
            roleInfo.setProxyMode(constraint.isProxyMode());
            
            boolean checked = constraint.getAuthenticate();
            roleInfo.setChecked(checked);
            if (roleInfo.isChecked())
            {
                if (constraint.isAnyRole())
                {
                    if (_strict)
                    {
                        // * means "all defined roles"
                        for (String role : _roles)
                            roleInfo.addRole(role);
                    }
                    else
                        // * means any role
                        roleInfo.setAnyRole(true);
                }
                else
                {
                    String[] newRoles = constraint.getRoles();
                    for (String role : newRoles)
                    {
                        if (_strict &&!_roles.contains(role))
                            throw new IllegalArgumentException("Attempt to use undeclared role: " + role + ", known roles: " + _roles);
                        roleInfo.addRole(role);
                    }
                }
            }
            if (method == null)
            {
                for (Map.Entry<String, RoleInfo> entry : mappings.entrySet())
                {
                    if (entry.getKey() != null)
                    {
                        RoleInfo specific = entry.getValue();
                        specific.combine(roleInfo);
                    }
                }
            }
        }
	}
	
	@SuppressWarnings("unchecked")
	@Override
    public void dump(Appendable out,String indent) throws IOException
    {
        dumpThis(out);
        dump(out,indent,Arrays.asList(getBeans(),Collections.singleton(_roles),_servletsMap.entrySet()));
    }
}
