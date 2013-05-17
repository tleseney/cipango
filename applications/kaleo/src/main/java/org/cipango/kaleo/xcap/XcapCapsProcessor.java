package org.cipango.kaleo.xcap;

import java.util.HashMap;
import java.util.Map;

public class XcapCapsProcessor extends XcapProcessorImpl
{
	public XcapCapsProcessor()
	{
		setAuid("xcap-caps");
		setMimeType("application/xcap-caps+xml");
		setName("XCAP Server Capabilities");
		Map<String, String> namespaceContext = new HashMap<String, String>();
		namespaceContext.put("caps", "urn:ietf:params:xml:ns:xcap-caps");
		setNamespaceContext(namespaceContext);
		setXsdSchemaPath("/schema/xcap-caps.xsd");
	}
	
	@Override
	public boolean validateResource(XcapResource resource) throws XcapException
	{
		return false;
	}
}
