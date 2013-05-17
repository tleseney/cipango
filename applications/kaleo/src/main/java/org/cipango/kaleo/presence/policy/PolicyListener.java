package org.cipango.kaleo.presence.policy;

import java.util.EventListener;

public interface PolicyListener extends EventListener 
{
	
	public void policyHasChanged(Policy policy);
}
