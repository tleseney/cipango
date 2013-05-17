package org.cipango.kaleo.xcap;

import java.util.Map;

import org.cipango.kaleo.xcap.dao.XmlResource;
import org.w3c.dom.Node;

public class XcapResourceImpl implements XcapResource
{


	public enum NodeType
	{
		ATTRIBUTE("application/xcap-att+xml"),
		ELEMENT("application/xcap-el+xml"),
		NAMESPACE("application/xcap-ns+xml");
		
		private String _mimeType;
		
		private NodeType(String mimeType)
		{
			_mimeType = mimeType;
		}
		
		public String getMimeType()
		{
			return _mimeType;
		}
	}
	
	private XcapUri _xcapUri;
	private XmlResource _selectedResource;
	private String _action;
	private boolean _creation;
	private XcapResourceProcessor _processor;
	private String _previousEtag;
	private String _newEtag;

	private String _parentPath;
	private NodeType _nodeType;
	private String _nodeName;

	private XmlResource _document;
	
	private Map<String, String> _namespaceContext;
	
	public XcapResourceImpl()
	{

	}

	public String getAction()
	{
		return _action;
	}

	public void setAction(String action)
	{
		_action = action;
	}

	/**
	 * Returns <code>true</code> if all document has been selected. i.e. if
	 * there is no node selector.
	 * 
	 * @return <code>true</code> if all document has been selected.
	 */
	public boolean isAllDocument()
	{
		return !_xcapUri.hasNodeSeparator();
	}

	public String getMimeType()
	{
		if (isAllDocument())
			return _processor.getMimeType();
		else
			return _nodeType.getMimeType();
	}

	public boolean isCreation()
	{
		return _creation;
	}

	public XcapResourceProcessor getProcessor()
	{
		return _processor;
	}

	public void setProcessor(XcapResourceProcessor processor)
	{
		_processor = processor;
	}

	public void setCreation(boolean creation)
	{
		_creation = creation;
	}

	private void setNodeType(short nodeType)
	{
		if (_nodeType == null)
		{
			if (nodeType == Node.ATTRIBUTE_NODE)
				_nodeType = NodeType.ATTRIBUTE;
			else if (nodeType == Node.ELEMENT_NODE)
				_nodeType = NodeType.ELEMENT;
			else if (nodeType == 13)
				_nodeType = NodeType.NAMESPACE;
			else
				throw new IllegalArgumentException("Invalid node type: " + nodeType);
				
		}
	}

	public void setSelectedResource(XmlResource resource)
	{
		_selectedResource = resource;
		setNodeType(_selectedResource.getDom().getNodeType());
	}

	/**
	 * Used when creation set to <code>true</code>.
	 * 
	 * @param parent
	 * @param nodeType
	 * @param nodeName
	 *            the node name (required for attribute creation).
	 */
	public void setParent(String parentPath, NodeType nodeType, String nodeName)
	{
		_parentPath = parentPath;
		_nodeType = nodeType;
		_nodeName = nodeName;
	}

	public String getParentPath()
	{
		return _parentPath;
	}

	public NodeType getNodeType()
	{
		return _nodeType;
	}

	public String getNodeName()
	{
		return this._nodeName;
	}


	public String getNewEtag()
	{
		return this._newEtag;
	}

	public void setNewEtag(String newEtag)
	{
		this._newEtag = newEtag;
	}

	public String getPreviousEtag()
	{
		return this._previousEtag;
	}

	public void setPreviousEtag(String previousEtag)
	{
		this._previousEtag = previousEtag;
	}

	public XmlResource getDocument()
	{
		return _document;
	}

	public void setDocument(XmlResource resource)
	{
		_document = resource;
	}

	public XcapUri getXcapUri()
	{
		return _xcapUri;
	}

	public void setXcapUri(XcapUri xcapUri)
	{
		_xcapUri = xcapUri;
	}

	public XmlResource getSelectedResource()
	{
		if (isAllDocument())
			return _document;
		return _selectedResource;
	}

	public Map<String, String> getNamespaceContext()
	{
		return _namespaceContext;
	}

	public void setNamespaceContext(Map<String, String> namespaceContext)
	{
		_namespaceContext = namespaceContext;
	}


}
