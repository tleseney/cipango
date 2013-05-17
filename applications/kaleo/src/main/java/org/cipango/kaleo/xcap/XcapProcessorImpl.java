package org.cipango.kaleo.xcap;

import java.util.Map;

import org.cipango.kaleo.xcap.util.XcapUtil;
import org.iso_relax.verifier.Schema;

public class XcapProcessorImpl implements XcapResourceProcessor
{
	private String _auid;
	private String _defaultNamespacePrefix;
	private String _mimeType;
	private Map<String, String> _namespaceContext;
	private Schema _xsdSchema;
	private String _name;
	
	public boolean validateResource(XcapResource resource) throws XcapException
	{
		return true;
	}

	public String getAuid()
	{
		return _auid;
	}

	public void setAuid(String auid)
	{
		_auid = auid;
	}

	public String getDefaultNamespacePrefix()
	{
		return _defaultNamespacePrefix;
	}

	public void setDefaultNamespacePrefix(String defaultNamespacePrefix)
	{
		_defaultNamespacePrefix = defaultNamespacePrefix;
	}

	public String getMimeType()
	{
		return _mimeType;
	}

	public void setMimeType(String mimeType)
	{
		_mimeType = mimeType;
	}

	public Map<String, String> getNamespaceContext()
	{
		return _namespaceContext;
	}

	public void setNamespaceContext(Map<String, String> namespaceContext)
	{
		_namespaceContext = namespaceContext;
	}

	public Schema getXsdSchema()
	{
		return _xsdSchema;
	}
	
	public void setXsdSchemaPath(String path)
	{
		_xsdSchema = XcapUtil.getSchema(path);
	}

	public void setXsdSchema(Schema xsdSchema)
	{
		_xsdSchema = xsdSchema;
	}

	public void processResource(XcapResource resource)
	{

	}

	public String getName()
	{
		return _name;
	}

	public void setName(String name)
	{
		_name = name;
	}
	
	public String toString()
	{
		if (_name != null)
			return _name;
		return _auid;
	}

}
