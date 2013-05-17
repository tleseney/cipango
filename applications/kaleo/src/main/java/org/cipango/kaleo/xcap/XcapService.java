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
package org.cipango.kaleo.xcap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Level;
import org.cipango.kaleo.Constants;
import org.cipango.kaleo.xcap.XcapResourceImpl.NodeType;
import org.cipango.kaleo.xcap.dao.XcapDao;
import org.cipango.kaleo.xcap.dao.XmlResource;
import org.cipango.kaleo.xcap.util.HexString;
import org.cipango.kaleo.xcap.util.RequestUtil;
import org.cipango.kaleo.xcap.util.XcapUtil;
import org.iso_relax.verifier.VerifierConfigurationException;
import org.jaxen.JaxenException;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class XcapService extends AbstractLifeCycle
{
	public static final String POST = "POST";
	public static final String PUT = "PUT";
	public static final String DELETE = "DELETE";
	public static final String GET = "GET";
	
	private static final String XPOINTER_PREFIX = "xmlns(";
	private static final String XPOINTER_PREFIX_REGEX = "xmlns\\(";

	private final Logger _log = LoggerFactory.getLogger(XcapService.class);

	private Map<String, XcapResourceProcessor> _processors;
	private XcapDao _dao;

	private boolean _validate;
	private boolean _validateOnGet;
	private String _rootName;
	private Map<String, List<XcapListener>> _listeners = new HashMap<String, List<XcapListener>>(); 
	
	@Override
	protected void doStart() throws Exception
	{
		setRootName("xcap");
		_validate = _validateOnGet = false;
		_processors = new HashMap<String, XcapResourceProcessor>();
		createIetfProcessors();
		createOmaProcessors();
		_dao.init(_processors.values());
		initXcapCaps();
	}
	
	public void createIetfProcessors()
	{
		XcapProcessorImpl processor = new XcapProcessorImpl();
		processor.setAuid("resource-lists");
		processor.setDefaultNamespacePrefix("rl");
		processor.setMimeType("application/resource-lists+xml");
		Map<String, String> namespaceContext = new HashMap<String, String>();
		namespaceContext.put("rl", "urn:ietf:params:xml:ns:resource-lists");
		processor.setNamespaceContext(namespaceContext);
		addProcessor(processor);
		
		processor = new XcapProcessorImpl();
		processor.setAuid("pres-rules");
		processor.setDefaultNamespacePrefix("cr");
		processor.setMimeType("application/auth-policy+xml");
		namespaceContext = new HashMap<String, String>();
		namespaceContext.put("pr", "urn:ietf:params:xml:ns:pres-rules");
		namespaceContext.put("cr", "urn:ietf:params:xml:ns:common-policy");
		processor.setNamespaceContext(namespaceContext);
		processor.setXsdSchemaPath("/schema/common-policy.xsd");
		processor.setName("Presence-rules processor");
		addProcessor(processor);
			
		addProcessor(new XcapCapsProcessor());
	}
	
	public void createOmaProcessors()
	{
		XcapProcessorImpl processor = new XcapProcessorImpl();
		processor.setAuid("org.openmobilealliance.access-rules");
		processor.setDefaultNamespacePrefix("cr");
		processor.setMimeType("application/auth-policy+xml");
		Map<String, String> namespaceContext = new HashMap<String, String>();
		namespaceContext.put("pr", "urn:ietf:params:xml:ns:pres-rules");
		namespaceContext.put("cr", "urn:ietf:params:xml:ns:common-policy");
		namespaceContext.put("cp", "urn:oma:xml:xdm:common-policy");
		processor.setNamespaceContext(namespaceContext);
		processor.setName("OMA shared policy processor");
		processor.setXsdSchemaPath("/schema/common-policy.xsd");
		addProcessor(processor);
		
		processor = new XcapProcessorImpl();
		processor.setAuid("org.openmobilealliance.pres-rules");
		processor.setDefaultNamespacePrefix("cr");
		processor.setMimeType("application/auth-policy+xml");
		namespaceContext = new HashMap<String, String>();
		namespaceContext.put("pr", "urn:ietf:params:xml:ns:pres-rules");
		namespaceContext.put("cr", "urn:ietf:params:xml:ns:common-policy");
		namespaceContext.put("cp", "urn:oma:xml:xdm:common-policy");
		processor.setNamespaceContext(namespaceContext);
		processor.setName("OMA presence rules processor");
		processor.setXsdSchemaPath("/schema/common-policy.xsd");
		addProcessor(processor);
		
		processor = new XcapProcessorImpl();
		processor.setAuid("org.openmobilealliance.group-usage-list");
		processor.setDefaultNamespacePrefix("rl");
		processor.setMimeType("application/vnd.oma.group-usage-list+xml");
		namespaceContext = new HashMap<String, String>();
		namespaceContext.put("rl", "urn:ietf:params:xml:ns:resource-lists");
		namespaceContext.put("oru", "urn:oma:xml:xdm:resource-list:oma-uriusage");
		namespaceContext.put("oxe", "urn:oma:xml:xdm:extensions");
		processor.setNamespaceContext(namespaceContext);
		processor.setName("OMA group usage list processor");
		processor.setXsdSchemaPath("/schema/OMA-SUP-XSD_xdm_extensions-V1_0-20080916-C.xsd");
		addProcessor(processor);
		
		processor = new XcapProcessorImpl();
		processor.setAuid("org.openmobilealliance.user-profile");
		processor.setDefaultNamespacePrefix("ur");
		processor.setMimeType("application/vnd.oma.user-profile+xml");
		namespaceContext = new HashMap<String, String>();
		namespaceContext.put("ur", "urn:oma:xml:xdm:user-profile");
		processor.setNamespaceContext(namespaceContext);
		processor.setName("OMA shared profile XDM processor");
		processor.setXsdSchemaPath("/schema/OMA-SUP-XSD_xdm_userProfile-V1_0-20070724-C.xsd");
		addProcessor(processor);
		
		processor = new XcapProcessorImpl();
		processor.setAuid("org.openmobilealliance.locked-user-profile");
		processor.setDefaultNamespacePrefix("ur");
		processor.setMimeType("application/vnd.oma.user-profile+xml");
		namespaceContext = new HashMap<String, String>();
		namespaceContext.put("ur", "urn:oma:xml:xdm:user-profile");
		processor.setNamespaceContext(namespaceContext);
		processor.setName("OMA locked user profile processor");
		processor.setXsdSchemaPath("/schema/OMA-SUP-XSD_xdm_userProfile-V1_0-20070724-C.xsd");
		addProcessor(processor);
		
		processor = new XcapProcessorImpl();
		processor.setAuid("org.openmobilealliance.groups");
		processor.setDefaultNamespacePrefix("ls");
		processor.setMimeType("application/vnd.oma.poc.groups+xml");
		namespaceContext = new HashMap<String, String>();
		namespaceContext.put("ls", "urn:oma:xml:poc:list-service");
		processor.setNamespaceContext(namespaceContext);
		processor.setName("OMA shared group XDM processor");
		// TODO set schema
		addProcessor(processor);
	}
	
	private void initXcapCaps() throws XcapException, IOException
	{
		StringBuilder sb = new StringBuilder();
		sb.append("<?xml version=\"1.0\"?>\n");
		sb.append("<xcap-caps xmlns=\"urn:ietf:params:xml:ns:xcap-caps\"\n");
		sb.append("  xmlns:xsi=\"htt//www.w3.org/2001/XMLSchema-instance\"\n");
		sb.append("  xsi:schemaLocation=\"urn:ietf:params:xml:ns:xcap-caps xcap-caps.xsd\">\n");
		sb.append("<auids>\n");
		for (XcapResourceProcessor processor : _processors.values())
			sb.append("\t<auid>").append(processor.getAuid()).append("</auid>\n");
		sb.append("</auids>\n");
		sb.append("<namespaces>\n");
		for (XcapResourceProcessor processor : _processors.values())
		{
			for (String namespace : processor.getNamespaceContext().values())
				sb.append("\t<namespace>").append(namespace).append("</namespace>\n");
		}
		sb.append("</namespaces>\n");
		sb.append("</xcap-caps>");
		XcapResourceImpl resource = getResource(
				new XcapUri("xcap-caps/global/index", ""), 
				true, "", null);
		_dao.update(resource, sb.toString());
		_dao.save(resource);
	}
	
	public void addProcessor(XcapResourceProcessor processor)
	{
		_processors.put(processor.getAuid(), processor);
	}
	
	public boolean isAuidRegistered(String auid)
	{
		return _processors.containsKey(auid);
	}

	public XcapResourceImpl getResource(XcapUri xcapUri, boolean isPut,
			String requestUrlHead, Map<String, String> requestNamespaceContext)
			throws XcapException
	{
		XcapResourceProcessor processor = _processors.get(xcapUri.getAuid());

		if (processor == null)
			throw new XcapException("Not supported auid: " + xcapUri.getAuid()
					+ " in URI: " + xcapUri,
					HttpServletResponse.SC_NOT_FOUND);

		Document document = null;
		XcapResourceImpl resource = new XcapResourceImpl();

		resource.setXcapUri(xcapUri);
		resource.setProcessor(processor);

		XmlResource xmlResource = _dao.getDocument(xcapUri, isPut && !xcapUri.hasNodeSeparator());
		
		if (xmlResource == null)
		{
			if (isPut)
			{
				locatingParent(xcapUri, requestUrlHead);
				resource.setCreation(true);
				return resource;
			}
			else
			{
				XcapException e = new XcapException("Resource: "
						+ xcapUri.getDocumentSelector() + " not found",
						HttpServletResponse.SC_NOT_FOUND);
				e.setLevel(Level.INFO);
				throw e;
			}
		}
		resource.setDocument(xmlResource);
		// TODO check rootDirectory is in parent dir.

		// TODO authenticate & authorization

		if (_validateOnGet)
		{
			try
			{
				XcapUtil.validate(document, processor.getXsdSchema());
			}
			catch (XcapException e)
			{
				_log.warn("Unable to validated document:" + e.getMessage(), e);
			}
		}

		if (resource.isAllDocument())
			return resource;
		

		String nodeSelector = XcapUtil.insertDefaultNamespace(xcapUri
				.getNodeSelector(), processor.getDefaultNamespacePrefix());

		if (_log.isDebugEnabled())
			_log.debug("select node " + nodeSelector + " in "
					+ xcapUri.getDocumentSelector());

		if (requestNamespaceContext == null)
			requestNamespaceContext = new HashMap<String, String>();
		requestNamespaceContext.putAll(processor.getNamespaceContext());

		resource.setNamespaceContext(requestNamespaceContext);
		
		XmlResource xmlResource2 = _dao.getNode(resource, nodeSelector);

		if (xmlResource2 == null)
		{
			if (isPut)
			{
				// XCAP 8.2.1. Locating the Parent
				String parent = locatingParent(resource, nodeSelector,
						xcapUri.getDocumentSelector(), requestUrlHead);
				resource.setCreation(true);
				NodeType nodeType;
				String nodeName = nodeSelector.substring(parent.length());
				// /@id is an attribute and /service[@id="1"] is an
				// element
				if (nodeName.indexOf('@') != -1
						&& nodeName.indexOf('[') == -1)
				{
					nodeType = NodeType.ATTRIBUTE;
					nodeName = nodeName.substring(nodeName.indexOf('@') + 1);
				}
				else
					nodeType = NodeType.ELEMENT;
				resource.setParent(parent, nodeType, nodeName);
			}
			else
			{
				XcapException e = new XcapException("Resource: "
						+ xcapUri + " not found (no node selected)",
						HttpServletResponse.SC_NOT_FOUND);
				e.setLevel(Level.INFO);
				throw e;
			}
		}
		else
		{
			resource.setCreation(false);
			resource.setSelectedResource(xmlResource2);
		}

		return resource;
	}

	private void locatingParent(XcapUri uri, String requestUrlHead)
			throws XcapException
	{
		// See XCAP 8.2.1. Locating the Parent

		String ancestor = _dao.getFirstExistAncestor(uri);

		if (uri.hasNodeSeparator() || uri.getDocumentSelector().substring(ancestor.length() + 1).indexOf('/') != -1)
		{
			XcapException e = new XcapException("parent does not exist",
					HttpServletResponse.SC_CONFLICT);
			StringBuffer sb = new StringBuffer();
			sb.append(XcapException.XCAP_ERROR_HEADER);
			sb.append("<no-parent><ancestor>");
			String url = requestUrlHead + _rootName + ancestor;
			sb.append(RequestUtil.filter(url));
			sb.append("</ancestor></no-parent>");
			sb.append(XcapException.XCAP_ERROR_FOOTER);

			e.setContent(XcapException.XCAP_ERROR_CONTENT_TYPE, sb.toString()
					.getBytes());
			throw e;
		}
	}

	/**
	 * Throws a XcapException if parent of nodeSelector does not exist else
	 * returns the parent of nodeSelector.
	 * 
	 * @param resource
	 * @param nodeSelector
	 * @param documentSelector
	 * @throws XcapException
	 * @throws XMLDBException 
	 * @throws XMLDBException
	 * @throws JaxenException
	 */
	private String locatingParent(XcapResource resource, String nodeSelector,
			String documentSelector, String requestUrlHead)
			throws XcapException
	{
		// See XCAP 8.2.1. Locating the Parent
		int index = nodeSelector.lastIndexOf('/');
		String parent = nodeSelector.substring(0, index);
		String firstExistAncestor = getFirstExistNodeAncestor(resource, parent);
		if (!parent.equals(firstExistAncestor))
		{
			XcapException e = new XcapException("parent does not exist",
					HttpServletResponse.SC_CONFLICT);
			StringBuffer sb = new StringBuffer();
			sb.append(XcapException.XCAP_ERROR_HEADER);
			sb.append("<no-parent><ancestor>");
			String url = requestUrlHead + _rootName + documentSelector;
			if (firstExistAncestor != null)
			{
				url += XcapUri.NODE_SELECTOR_SEPARATOR
						+ firstExistAncestor;
			}
			sb.append(RequestUtil.filter(url));
			sb.append("</ancestor></no-parent>");
			sb.append(XcapException.XCAP_ERROR_FOOTER);

			e.setContent(XcapException.XCAP_ERROR_CONTENT_TYPE, sb.toString()
					.getBytes());
			throw e;
		}
		else
		{
			return parent;
		}
	}

	private String getFirstExistNodeAncestor(XcapResource resource, String nodeSelector) throws XcapException
	{
		XmlResource xmlResource = _dao.getNode(resource, nodeSelector);
		if (xmlResource != null)
			return nodeSelector;
		else
		{
			int index = nodeSelector.lastIndexOf('/');
			if (index != -1)
			{
				String parent = nodeSelector.substring(0, index);
				return getFirstExistNodeAncestor(resource, parent);
			}
			else
				return null;
		}
	}

	public void service(HttpServletRequest request,
			HttpServletResponse response) throws IOException, SAXException, VerifierConfigurationException
	{
		String method = request.getMethod();
		String requestUri = request.getRequestURI();
		StringBuffer requestUrl = request.getRequestURL();
		String head = requestUrl.substring(0, requestUrl.indexOf(requestUri));

		try
		{
			if ("POST".equals(method))
			{
				throw new XcapException(
						HttpServletResponse.SC_METHOD_NOT_ALLOWED);
			}

			Map<String, String> context = getXpointer(request);
			
			XcapUri xcapUri = new XcapUri(requestUri, _rootName);
			XcapResourceImpl resource = getResource(xcapUri, PUT
					.equals(method), head, context);
			
			ifMatchConditionalProcessing(request, resource);
			ifNoneMatchConditionalProcessing(request, resource);

			resource.setAction(method);

			if (method.equals(GET))
				doGet(response, resource);
			else if (method.equals(PUT))
				doPut(request, resource);
			else if (method.equals(DELETE))
				doDelete(request, resource);


			if (method.equals(PUT) || method.equals(DELETE))
			{
				checkIfSameNodeSelected(resource);
				resource.getProcessor().processResource(resource);
				_dao.save(resource);
				notifyResourceChanged(resource);
			}

			String newEtag = getEtag(resource);
			resource.setNewEtag(newEtag);
			response.setHeader(Constants.ETAG, newEtag);
			response.setStatus(HttpServletResponse.SC_OK);
			if (_log.isDebugEnabled())
				_log.debug(method + " " + requestUri + " sucessful");
		}
		catch (XcapException e)
		{
			if (e.shouldShowStackTrace())
				_log.info("Unable to process " + method + " " + requestUri, e);
			else
				_log.info("Unable to process " + method + " " + requestUri
						+ ": " + e.getMessage());
			e.sendResponse(response);
		}
	}

	private void doGet(HttpServletResponse response, XcapResourceImpl resource) throws IOException
	{
		response.setContentType(resource.getMimeType());
		if (resource.isAllDocument())
		{
			response.getOutputStream().write(
					resource.getDocument().getBytes());
		}
		else
		{
			XmlResource selectedResource = resource.getSelectedResource();
			switch (resource.getNodeType())
			{
			case ATTRIBUTE:
				response.getOutputStream().write(((Attr) selectedResource.getDom()).getValue().getBytes());
				break;
			case ELEMENT:
				response.getOutputStream().write(selectedResource.getBytes());
				break;
			case NAMESPACE:			
				Node node = selectedResource.getDom();
				String prefix = "<" + node.getNodeName() + " ";
				response.getOutputStream().write(prefix.getBytes());
				response.getOutputStream().write(node.getNamespaceURI().getBytes());
				response.getOutputStream().write("/>".getBytes());
				break;
			default:
				break;
			}

		}
	}

	private void doPut(HttpServletRequest request, XcapResourceImpl resource)
			throws IOException, XcapException, SAXException, VerifierConfigurationException
	{
		throwExceptionIfNamespace(resource);
		String content = getContent(request, resource);

		_dao.update(resource, content);

		if (_validate)
			XcapUtil.validate(resource.getDocument().getDom(), resource
					.getProcessor().getXsdSchema());
		
		validateSpecificAppResource(resource);	
	}

	private void doDelete(HttpServletRequest request, XcapResourceImpl resource)
			throws XcapException, SAXException, VerifierConfigurationException
	{
		throwExceptionIfNamespace(resource);
		_dao.delete(resource);
		if (!resource.isAllDocument())
		{
			if (_validate)
			{
				XcapUtil.validate(resource.getDocument().getDom(), 
						resource.getProcessor().getXsdSchema());
			}
			validateSpecificAppResource(resource);
		}
	}

	private void validateSpecificAppResource(XcapResourceImpl resource)
			throws XcapException
	{
		if (!resource.getProcessor().validateResource(resource))
		{
			XcapException e = new XcapException(
					"Specific application resource validation failed",
					HttpServletResponse.SC_CONFLICT);
			StringBuffer sb = new StringBuffer();
			sb.append(XcapException.XCAP_ERROR_HEADER);
			sb.append("<constraint-failure/>");
			sb.append(XcapException.XCAP_ERROR_FOOTER);
			e.setContent(XcapException.XCAP_ERROR_CONTENT_TYPE, sb.toString()
					.getBytes());
			throw e;
		}
	}

	@SuppressWarnings("unchecked")
	private void ifMatchConditionalProcessing(HttpServletRequest request,
			XcapResourceImpl resource) throws XcapException
	{
		Enumeration ifMatchEnum = request.getHeaders(Constants.IF_MATCH);
		String currentEtag = getEtag(resource);
		resource.setPreviousEtag(currentEtag);

		if (ifMatchEnum != null && ifMatchEnum.hasMoreElements())
		{

			while (ifMatchEnum.hasMoreElements())
			{
				String element = (String) ifMatchEnum.nextElement();
				String[] matchEtags = element.split(",");

				for (int i = 0; i < matchEtags.length; i++)
				{
					if (Constants.WILCARD.equals(matchEtags[i].trim()))
					{
						if (resource.isAllDocument() && resource.isCreation())
						{
							throw new XcapException(
									"Conditional processing failed: "
											+ "If-match: * and new document creation",
									HttpServletResponse.SC_PRECONDITION_FAILED);
						}
						else if (_log.isDebugEnabled())
							_log.debug("wilcard entity tags has matched");
					}
					else if (currentEtag.equals(matchEtags[i].trim()))
					{
						if (_log.isDebugEnabled())
							_log.debug("entity tag has matched");
						return;
					}
				}
			}
			throw new XcapException("Conditional processing failed: "
					+ "If-match: present and none match",
					HttpServletResponse.SC_PRECONDITION_FAILED);
		}
	}

	@SuppressWarnings("unchecked")
	private void ifNoneMatchConditionalProcessing(HttpServletRequest request,
			XcapResourceImpl resource) throws XcapException
	{
		Enumeration ifNoneMatchEnum = request
				.getHeaders(Constants.IF_NONE_MATCH);

		if (ifNoneMatchEnum != null && ifNoneMatchEnum.hasMoreElements())
		{
			String currentEtag = getEtag(resource);
			while (ifNoneMatchEnum.hasMoreElements())
			{
				String element = (String) ifNoneMatchEnum.nextElement();
				String[] noneMatchEtags = element.split(",");

				for (int i = 0; i < noneMatchEtags.length; i++)
				{
					if (Constants.WILCARD.equals(noneMatchEtags[i].trim()))
					{
						if (resource.isAllDocument() && resource.isCreation())
						{
							if (_log.isDebugEnabled())
								_log.debug("wilcard entity tag has matched");
						}
						else
						{
							throw new XcapException(
									"Conditional processing failed: "
											+ "If-None-match: * and not new document creation",
									HttpServletResponse.SC_PRECONDITION_FAILED);
						}
					}
					else if (currentEtag.equals(noneMatchEtags[i].trim()))
					{
						if ("GET".equals(request.getMethod()))
						{
							throw new XcapException(
									"Conditional processing failed: "
											+ "If-None-match: present and match",
									HttpServletResponse.SC_NOT_MODIFIED);
						}
						else
						{
							throw new XcapException(
									"Conditional processing failed: "
											+ "If-None-match: present and match",
									HttpServletResponse.SC_PRECONDITION_FAILED);
						}
					}
				}
			}
		}
	}

	private void throwExceptionIfNamespace(XcapResourceImpl resource)
			throws XcapException
	{
		if (!resource.isAllDocument()
				&& resource.getNodeType() == NodeType.NAMESPACE)
		{
			// If the request URI contained a namespace-selector, the server
			// MUST
			// reject the request with a 405 (Method Not Allowed) and MUST
			// include
			// an Allow header field including the GET method.
			XcapException e = new XcapException(
					HttpServletResponse.SC_METHOD_NOT_ALLOWED);
			e.addHeader(Constants.ALLOW, GET);
			throw e;
		}
	}

	public String getEtag(XcapResourceImpl resource) {
		try {
            if (resource.getDocument() == null)
            	return "notExist";
            
            MessageDigest md = MessageDigest.getInstance("MD5");
                       
            md.update(resource.getDocument().getBytes());
            return HexString.bufferToHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
           _log.error("Unable to initialize Message Digest (for etags).", e);
           return "defaultEtag";
        } catch (Throwable e) {
            _log.error("Unable to to create etags", e);
            return "defaultEtag";
         }
	}

	/**
	 * XCAP 8.2.2. Verifying Document Content
	 */
	private String getContent(HttpServletRequest request,
			XcapResourceImpl resource) throws XcapException
	{
		if (request.getContentLength() <= 0)
		{
			throw new XcapException("No content received ",
					HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
		}

		// If the MIME type in the Content-Type header field of the request is
		// not equal to the MIME type defined for the application usage, the
		// server MUST reject the request with a 415.
		if (!resource.getMimeType().equals(request.getContentType()))
		{
			throw new XcapException("Bad content type: "
					+ request.getContentType() + " should be:"
					+ resource.getMimeType(),
					HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
		}

		try
		{
			InputStream is = request.getInputStream();
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			int read;
			byte[] buffer = new byte[128];
			while ((read = is.read(buffer)) != -1)
			{
				os.write(buffer, 0, read);
			}
			return new String(new String(os.toByteArray()));
		}
		catch (Throwable e)
		{
			XcapException e1 = new XcapException("Unable to read content",
					HttpServletResponse.SC_CONFLICT);
			StringBuffer sb = new StringBuffer();
			sb.append(XcapException.XCAP_ERROR_HEADER);
			sb.append("<not-xml-att-value/>");
			sb.append(XcapException.XCAP_ERROR_FOOTER);

			e1.setContent(XcapException.XCAP_ERROR_CONTENT_TYPE, sb
					.toString().getBytes());
			throw e1;
		}

	}

	@SuppressWarnings("unchecked")
	private Map<String, String> getXpointer(HttpServletRequest request)
	{
		Enumeration enumeration = request.getParameterNames();
		Map<String, String> context = new HashMap<String, String>();
		while (enumeration.hasMoreElements())
		{
			String name = (String) enumeration.nextElement();
			if (name.startsWith(XPOINTER_PREFIX))
			{
				String prefix = name.substring(XPOINTER_PREFIX.length());
				String[] values = request.getParameter(name).split(
						XPOINTER_PREFIX_REGEX);
				String ns = values[0].substring(0, values[0].indexOf(')'));
				context.put(prefix, ns);

				for (int i = 1; i < values.length; i++)
				{
					int index = values[i].indexOf('=');
					prefix = values[i].substring(0, index);
					ns = values[i].substring(index + 1, values[i].indexOf(')'));
					context.put(prefix, ns);
				}
			}
		}
		return context;
	}

	private void checkIfSameNodeSelected(XcapResourceImpl resource)
			throws XcapException
	{
		if (!resource.getXcapUri().hasNodeSeparator() || resource.isCreation())
			return;

		try
		{
			// boolean match =
			// resource.getSelectedNode().matches(resource.getNodeSelector());
			
			XmlResource xmlResource = _dao.getNode(resource);
			boolean valid = false;
			if (xmlResource == null)
			{
				valid = resource.getAction().equals(DELETE);
			}
			else
			{
				Node node = xmlResource.getDom();
				valid = node.equals(resource.getSelectedResource().getDom())
						&& resource.getAction().equals(PUT);
			}
			if (!valid)
			{
				XcapException e1 = new XcapException(
						"Request no more select the same node",
						HttpServletResponse.SC_CONFLICT);
				StringBuffer sb = new StringBuffer();
				sb.append(XcapException.XCAP_ERROR_HEADER);
				sb.append("<cannot-insert/>");
				sb.append(XcapException.XCAP_ERROR_FOOTER);
				e1.setContent(XcapException.XCAP_ERROR_CONTENT_TYPE, sb
						.toString().getBytes());
				throw e1;
			}
		}
		catch (XcapException e)
		{
			throw e;
		}
		catch (Throwable e)
		{
			XcapException e1 = new XcapException(
					"Cannot check if select the same node",
					HttpServletResponse.SC_CONFLICT, e);
			StringBuffer sb = new StringBuffer();
			sb.append(XcapException.XCAP_ERROR_HEADER);
			sb.append("<cannot-insert/>");
			sb.append(XcapException.XCAP_ERROR_FOOTER);
			e1.setContent(XcapException.XCAP_ERROR_CONTENT_TYPE, sb.toString()
					.getBytes());
			throw e1;
		}
	}

	public XcapDao getDao()
	{
		return _dao;
	}

	public void setDao(XcapDao dao)
	{
		_dao = dao;
	}

	public boolean isValidate()
	{
		return _validate;
	}

	public void setValidate(boolean validate)
	{
		_validate = validate;
	}

	public boolean isValidateOnGet()
	{
		return _validateOnGet;
	}

	public void setValidateOnGet(boolean validateOnGet)
	{
		_validateOnGet = validateOnGet;
	}

	public String getRootName()
	{
		return _rootName;
	}

	public void setRootName(String name)
	{
		if (name == "" || "/".equals(name))
			_rootName = "/";
		else 
		{
			if (name.startsWith("/") && name.endsWith("/")) 
				_rootName = name;
			else
				_rootName = "/" +  name + "/";	
		}
	}
	
	public void addListener(XcapListener l, XcapUri uri)
	{
		synchronized (_listeners)
		{
			List<XcapListener> list = _listeners.get(uri.getDocumentSelector());
			if (list == null)
			{
				list = new ArrayList<XcapListener>();
				_listeners.put(uri.getDocumentSelector(), list);
			}
			if (!list.contains(l))
				list.add(l);
		}
	}
	
	public void removeListener(XcapListener l, XcapUri uri)
	{
		synchronized (_listeners)
		{
			List<XcapListener> list = _listeners.get(uri.getDocumentSelector());
			if (list != null)
			{
				list.remove(l);
				if (list.isEmpty())
					_listeners.remove(uri.getDocumentSelector());
			}
		}
	}
	
	private void notifyResourceChanged(XcapResource resource)
	{
		List<XcapListener> list = null;
		synchronized (_listeners)
		{
			list = _listeners.get(resource.getXcapUri().getDocumentSelector());
		}
		if (list != null)
		{
			Iterator<XcapListener> it = list.iterator();
			while (it.hasNext())
				it.next().documentChanged(resource);
		}
	}

}
