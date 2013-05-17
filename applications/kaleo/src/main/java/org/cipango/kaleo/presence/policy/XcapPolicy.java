// ========================================================================
// Copyright 2009 NEXCOM Systems
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
package org.cipango.kaleo.presence.policy;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.xmlbeans.XmlCursor;
import org.cipango.kaleo.Resource;
import org.cipango.kaleo.policy.ConditionsType;
import org.cipango.kaleo.policy.ExtensibleType;
import org.cipango.kaleo.policy.IdentityType;
import org.cipango.kaleo.policy.ManyType;
import org.cipango.kaleo.policy.RuleType;
import org.cipango.kaleo.policy.RulesetDocument;
import org.cipango.kaleo.policy.ValidityType;
import org.cipango.kaleo.policy.RulesetDocument.Ruleset;
import org.cipango.kaleo.policy.oma.ExternalListDocument.ExternalList;
import org.cipango.kaleo.policy.presence.SubHandlingDocument;
import org.cipango.kaleo.presence.policy.PolicyManager.SubHandling;
import org.cipango.kaleo.xcap.XcapException;
import org.cipango.kaleo.xcap.XcapListener;
import org.cipango.kaleo.xcap.XcapResource;
import org.cipango.kaleo.xcap.XcapService;
import org.cipango.kaleo.xcap.XcapUri;
import org.eclipse.jetty.util.LazyList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XcapPolicy implements Policy
{
	private static final String OMA_COMMON_POLICY = "urn:oma:xml:xdm:common-policy";
	private static final String PRES_RULES = "urn:ietf:params:xml:ns:pres-rules";
	
	private static final Logger __log = LoggerFactory.getLogger(XcapPolicy.class);
	private Object _listeners; //LazyList<PolicyListener>
	private String _resourceUri;
	private XcapListener _xcapListener;
	private XcapService _xcapService;
	private List<XcapUri> _xcapResources = new ArrayList<XcapUri>();
	
	public XcapPolicy(Resource resource, XcapService xcapService)
	{
		_resourceUri = resource.getUri();
		_xcapService = xcapService;
		addXcapRessource(new XcapUri("org.openmobilealliance.pres-rules", _resourceUri, "pres-rules", null));
	}
	
	public String getResourceUri()
	{
		return _resourceUri;
	}
		
	private void addXcapRessource(XcapUri xcapUri)
	{
		XcapUri uri = new XcapUri(xcapUri.getDocumentSelector());
		if (!_xcapResources.contains(uri))
		{
			_xcapResources.add(uri);
			if (_listeners != null)
				_xcapService.addListener(_xcapListener, uri);
		}
	}
	
	public SubHandling getPolicy(String subscriberUri)
	{
		try
		{
			XcapUri xcapUri = _xcapResources.get(0);
			XcapResource xcapResource = _xcapService.getResource(xcapUri, false, null, null);
			Ruleset ruleset = RulesetDocument.Factory.parse(xcapResource.getSelectedResource().getDom()).getRuleset();
			SubHandling best = null;
			String domain = null;
			if (subscriberUri.indexOf('@') != -1)
				domain = subscriberUri.substring(subscriberUri.indexOf('@') + 1);
			
			for (int i = 0; i < ruleset.getRuleArray().length; i++)
			{
				RuleType rule = ruleset.getRuleArray(i);
				if (match(rule.getConditions(), subscriberUri, domain))
				{
					SubHandling subHandling = getSubHandling(rule.getActions());
					if (best == null || subHandling.getValue() > best.getValue())
						best = subHandling;
				}
			}
			if (best == null)
			{
				for (int i = 0; i < ruleset.getRuleArray().length; i++)
				{
					RuleType rule = ruleset.getRuleArray(i);
					if (matchOma(rule.getConditions(), subscriberUri))
					{
						SubHandling subHandling = getSubHandling(rule.getActions());
						if (best == null || subHandling.getValue() > best.getValue())
							best = subHandling;
					}
				}
			}
			if (best == null)
			{
				for (int i = 0; i < ruleset.getRuleArray().length; i++)
				{
					RuleType rule = ruleset.getRuleArray(i);
					if (matchOmaOtherIdentity(rule.getConditions()))
					{
						SubHandling subHandling = getSubHandling(rule.getActions());
						if (best == null || subHandling.getValue() > best.getValue())
							best = subHandling;
					}
				}
			}
			__log.debug("Got policy " + best + " for subscriber {} and resource {}", subscriberUri, _resourceUri);
			if (best == null)
				return SubHandling.BLOCK;
			return best;
		}
		catch (XcapException e) 
		{
			__log.debug("Unable to find policy for subcription: "  + subscriberUri, e);
			return SubHandling.BLOCK;
		}
		catch (Exception e) 
		{
			__log.warn("Unable to find policy for subcription: "  + subscriberUri, e);
			return SubHandling.BLOCK;
		}	
	}
	
	private SubHandling getSubHandling(ExtensibleType actions)
	{
		XmlCursor cursor = actions.newCursor();
		cursor.toChild(PRES_RULES, "sub-handling");
		SubHandlingDocument.SubHandling subHandling = (SubHandlingDocument.SubHandling) cursor.getObject();
		
		if (subHandling.enumValue().equals(SubHandlingDocument.SubHandling.ALLOW))
			return SubHandling.ALLOW;
		else if (subHandling.enumValue().equals(SubHandlingDocument.SubHandling.CONFIRM))
			return SubHandling.CONFIRM;
		else if (subHandling.enumValue().equals(SubHandlingDocument.SubHandling.BLOCK))
			return SubHandling.BLOCK;
		else if (subHandling.enumValue().equals(SubHandlingDocument.SubHandling.POLITE_BLOCK))
			return SubHandling.POLITE_BLOCK;
		
		throw new IllegalArgumentException("No sub-handling block");
	}

	private boolean match(ConditionsType conditions, String subscriberAor, String domain)
	{
		boolean matchIdentity = conditions.getIdentityArray().length == 0;
		for (int i = 0; i < conditions.getIdentityArray().length; i++)
		{
			IdentityType identity = conditions.getIdentityArray(i);
			for (int j = 0; j < identity.getOneArray().length; j++)
				if (identity.getOneArray(j).getId().equals(subscriberAor))
					matchIdentity = true;
			for (int j = 0; j < identity.getManyArray().length; j++)
			{
				ManyType manyType = identity.getManyArray(j);
				if (manyType.getDomain().equals(domain))
				{
					boolean match = true;
					for (int k = 0; k < manyType.getExceptArray().length; k++)
						if (manyType.getExceptArray(k).getId().equals(subscriberAor))
							match = false;
					if (match)
						matchIdentity = true;
				}
			}
		}
		
		if (!matchIdentity)
			return false;
		
		boolean matchValidity = conditions.getValidityArray().length == 0;
		Date now = new Date();
		for (int i = 0; i < conditions.getValidityArray().length; i++)
		{
			ValidityType validity = conditions.getValidityArray(i);
			for (int j = 0; j < validity.getFromArray().length; j++)
				if (validity.getFromArray(j).after(now) && validity.getUntilArray(j).before(now))
					matchValidity = true;
		}
		if (!matchValidity)
			return false;
		
		// TODO check sphere
		
		// Ensure that at least one condition has match
		return conditions.getIdentityArray().length != 0 || conditions.getValidityArray().length != 0;
	}
	
	private boolean matchOma(ConditionsType conditions, String subscriberAor)
	{
		try
		{
			XmlCursor cursor = conditions.newCursor();
			cursor.push();
			if (cursor.toChild(OMA_COMMON_POLICY, "external-list"))
			{
				ExternalList list = (ExternalList) cursor.getObject();
				for (int i = 0; i < list.getEntryArray().length; i++)
				{
					String anchor = list.getEntryArray(i).getAnc();
					if (match(anchor, subscriberAor))
						return true;
				}
			}
			cursor.pop();
			if (cursor.toChild(OMA_COMMON_POLICY, "anonymous-request"))
			{
				return subscriberAor.equals("sip:anonymous@anonymous.invalid");
				// TODO add better support to anonymous-request
			}
			
		}
		catch (Throwable e) {
			__log.warn("Unable to check OMA conditions for subscriber " + subscriberAor, e);
		}
		return false;
	}
	
	
	private boolean match(String anchor, String subscriberAor)
	{
		int index = anchor.indexOf("://");
		// Assume it is the same XCAP server
		String uri = anchor.substring(anchor.indexOf(_xcapService.getRootName(), index + 3));
		XcapUri xcapUri = new XcapUri(uri, _xcapService.getRootName());
		
		// Listen for changes for this resource.
		addXcapRessource(xcapUri); 
		
		XcapResource xcapResource = _xcapService.getResource(xcapUri, false, null, null);
		NodeList nodes = xcapResource.getSelectedResource().getDom().getChildNodes();
		for (int j = 0; j < nodes.getLength(); j++)
		{
			Node node = nodes.item(j);
			if ("entry".equals(node.getLocalName()))
			{
				Element element = (Element) node;
				if (subscriberAor.equals(element.getAttribute("uri")))
					return true;
			}
			else if ("external".equals(node.getLocalName()))
			{
				Element element = (Element) node;
				anchor = element.getAttribute("anchor");
				if (match(anchor, subscriberAor))
					return true;
			}
		}
		return false;
	}
	
	
	private boolean matchOmaOtherIdentity(ConditionsType conditions)
	{
		XmlCursor cursor = conditions.newCursor();
		return cursor.toChild(OMA_COMMON_POLICY, "other-identity");
	}
	
	public void addListener(PolicyListener l)
	{
		if (!LazyList.contains(_listeners, l))
			_listeners = LazyList.add(_listeners, l);
		if (_xcapListener == null)
		{
			_xcapListener = new XcapListenerImpl();
			Iterator<XcapUri> it = _xcapResources.iterator();
			while (it.hasNext())
				_xcapService.addListener(_xcapListener, it.next());
		}
	}
	

	public void removeListener(PolicyListener l)
	{
		_listeners = LazyList.remove(_listeners, l);
		if (_listeners == null && _xcapListener != null)
		{
			Iterator<XcapUri> it = _xcapResources.iterator();
			while (it.hasNext())
				_xcapService.removeListener(_xcapListener, it.next());
			_xcapListener = null;
		}
		
	}
	
	public List<XcapUri> getXcapResources()
	{
		return _xcapResources;
	}
	
	class XcapListenerImpl implements XcapListener
	{

		public void documentChanged(XcapResource resource)
		{
			__log.debug("Policy for resource " + _resourceUri + " has changed");
			for (int i = 0; i < LazyList.size(_listeners); i++)
				((PolicyListener) LazyList.get(_listeners, i)).policyHasChanged(XcapPolicy.this);
		}
		
	}


	
}
