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
package org.cipango.kaleo.xcap.dao;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;

import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.xerces.parsers.DOMParser;
import org.cipango.kaleo.xcap.XcapException;
import org.cipango.kaleo.xcap.XcapResource;
import org.cipango.kaleo.xcap.XcapResourceProcessor;
import org.cipango.kaleo.xcap.XcapService;
import org.cipango.kaleo.xcap.XcapUri;
import org.cipango.kaleo.xcap.XcapResourceImpl.NodeType;
import org.jaxen.JaxenException;
import org.jaxen.dom.DOMXPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

public class FileXcapDao implements XcapDao
{
	private Logger _log = LoggerFactory.getLogger(FileXcapDao.class);
	
	private File _baseDir;
	private boolean _createUser = true;
	private static TransformerFactory _transformerfactory = TransformerFactory.newInstance(); 
	
	public void init(Collection<XcapResourceProcessor> processors) throws Exception
	{
		if (_baseDir == null)
			throw new IllegalAccessException("Base dirctory is not set");
		_baseDir.mkdirs();
		if (!_baseDir.isDirectory())
			throw new IllegalAccessException("Base directory " + _baseDir + " is not a directory");
		if (!_baseDir.canWrite())
			throw new IllegalAccessException("Base directory " + _baseDir + " is not writable");
		for (XcapResourceProcessor processor : processors)
		{
			File file = new File(_baseDir, processor.getAuid());
			file.mkdir();
			new File(file, XcapUri.GLOBAL).mkdir();
			new File(file, XcapUri.USERS).mkdir();
		}

		_log.debug("File XCAP DAO is started with base directory: {}", _baseDir);
	}
	
	public void delete(XcapResource resource) throws XcapException
	{
		if (!resource.isAllDocument())
		{
			Node node = resource.getSelectedResource().getDom();
			if (node instanceof Attr)
			{
				Attr attr = (Attr) node;
				attr.getOwnerElement().removeAttribute(node.getLocalName());
			}
			else
			{
				node.getParentNode().removeChild(node);
			}
		}
	}

	public XmlResource getDocument(XcapUri uri, boolean create)
			throws XcapException
	{
		File file = getFile(uri);
		if (!file.exists())
		{
			if (!uri.isGlobal() && _createUser && !file.getParentFile().exists()
					&& file.getParentFile().getName().equals(getEscapePath(uri.getUser())))
				file.getParentFile().mkdir();
			return null;
		}
		if (file.isDirectory())
		{
			_log.debug("Find resource with document selector {}, but is a directory", uri.getDocumentSelector());
			return null;
		}
		return new FileXmlResource(file);
	}

	public XmlResource getNode(XcapResource resource) throws XcapException
	{
		return getNode(resource, resource.getXcapUri().getNodeSelector());
	}

	public XmlResource getNode(XcapResource resource, String nodeSelector) throws XcapException
	{
		try
		{
			DOMXPath xPath = new DOMXPath(nodeSelector);
			for (String prefix : resource.getNamespaceContext().keySet())
				xPath.addNamespace(prefix, resource.getNamespaceContext().get(prefix));
			Node node = (Node) xPath.selectSingleNode(resource.getDocument().getDom());
			if (node == null)
				return null;
			return new XpathXmlResource(node);
		}
		catch (JaxenException e)
		{
			throw XcapException.newInternalError(e);
		}
	}
	
	public void save(XcapResource resource) throws XcapException, IOException
	{
		if (XcapService.DELETE.equals(resource.getAction())
				&& resource.isAllDocument())
		{
			FileXmlResource res = (FileXmlResource) resource.getDocument();
			res.getFile().delete();
			resource.setDocument(null);
			return;
		}
			
		File file = ((FileXmlResource) resource.getDocument()).getFile();
		try
		{
			FileOutputStream os = new FileOutputStream(file);
			os.write(getContent(resource.getDocument().getDom(), false));
			os.close();
		} 
		catch (Exception e) 
		{
			throw new XcapException("Unable to save document in " + file, 
					HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
		}
	}

	public void update(XcapResource resource, String content)
			throws XcapException
	{
		if ( resource.getDocument() != null)
			((FileXmlResource) resource.getDocument()).setModified(true);
		
		if (resource.getNodeType() != null && resource.getNodeType() == NodeType.ATTRIBUTE)
		{
			if (resource.isCreation())
			{
				XmlResource parent = getNode(resource, resource.getParentPath());
				Element element = (Element) parent.getDom();
				element.setAttribute(resource.getNodeName(), content);
			}
			else
			{
				Attr attr = (Attr) resource.getSelectedResource().getDom();
				attr.setValue(content);
			}
		}
		else
		{
			Document document = null;
			try {			
				DOMParser parser = new DOMParser();
				parser.parse(new InputSource(new ByteArrayInputStream(content.getBytes())));
				document = parser.getDocument();			
			} catch (Throwable e) {
				XcapException e1 = new XcapException("Unable to read XML content",
						HttpServletResponse.SC_CONFLICT, e);
				StringBuffer sb = new StringBuffer();
				sb.append(XcapException.XCAP_ERROR_HEADER);
				sb.append("<not-well-formed/>");
				sb.append(XcapException.XCAP_ERROR_FOOTER);
				
				e1.setContent(XcapException.XCAP_ERROR_CONTENT_TYPE, sb.toString().getBytes());
	
				throw e1;
			}
			if (resource.isAllDocument())
			{
				resource.setDocument(new FileXmlResource(
						getFile(resource.getXcapUri()),
						document));
			}
			else
			{
				if (resource.isCreation())
				{
					XmlResource parent = getNode(resource, resource.getParentPath());
					Node newNode = parent.getDom().getOwnerDocument().importNode(document.getDocumentElement(), true);	
					parent.getDom().appendChild(newNode);
				}
				else
				{
					Node previous = resource.getSelectedResource().getDom();
					Node newNode = previous.getOwnerDocument().importNode(document.getDocumentElement(), true);	
					previous.getParentNode().replaceChild(newNode, previous);
					((XpathXmlResource) resource.getSelectedResource()).setNode(newNode);
				}
			}
		}
	}
	
	private File getFile(XcapUri uri)
	{
		return new File(_baseDir, getEscapePath(uri.getDocumentSelector()));
	}
	
	private String getEscapePath(String path)
	{
		return path.replace(":", "%3A").replace("@", "%40");
	}
	
	public File getBaseDir()
	{
		return _baseDir;
	}

	public void setBaseDir(File baseDir)
	{
		_baseDir = baseDir;
	}
	
	private static byte[] getContent(Node node, boolean omitXmlDeclaration)
	{
		try
		{
			DOMSource domSource = new DOMSource(node); 
			ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
			
			Transformer transformer = _transformerfactory.newTransformer(); 
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, omitXmlDeclaration ? "yes" : "no");
			transformer.transform(domSource, new StreamResult(byteOut)); 

			return byteOut.toByteArray();
		}
		catch (TransformerException e)
		{
			throw XcapException.newInternalError(e);
		}
	}
	
	public String getFirstExistAncestor(XcapUri uri)
	{
		File file = getFile(uri);
		while (!file.exists() && !file.equals(_baseDir))
			file = file.getParentFile();
		if (file.equals(_baseDir))
			return "";
		return file.getAbsolutePath().substring(_baseDir.getAbsolutePath().length() + 1).replace('\\', '/');
	}

	public static class XpathXmlResource implements XmlResource
	{
		private Node _node;
		
		public XpathXmlResource(Node node)
		{
			_node = node;
		}
		
		public byte[] getBytes()
		{
			return FileXcapDao.getContent(_node, true);
		}

		public Node getDom()
		{
			return _node;
		}
		
		public void setNode(Node node)
		{
			_node = node;
		}
		
	}
	
	public static class FileXmlResource implements XmlResource
	{
		private File _file;
		private Document _document;
		public boolean _modified = false;
		
		public FileXmlResource(File file)
		{
			_file = file;
		}
		
		public FileXmlResource(File file, Document document)
		{
			_file = file;
			_document = document;
			_modified = true;
		}
		
		public byte[] getBytes()
		{
			if (_modified)
			{
				return FileXcapDao.getContent(_document, false);
			}
			else
			{
				try
				{
					FileInputStream is = new FileInputStream(_file);
					ByteArrayOutputStream os = new ByteArrayOutputStream();
					byte[] b = new byte[512];
					int read;
					while ((read = is.read(b)) != -1)
						os.write(b, 0, read);
					b = os.toByteArray();
					os.close();
					is.close();
					return b;
					
				}
				catch (IOException e)
				{
					throw XcapException.newInternalError(e);
				}
			}
		}

		public Node getDom()
		{
			if (_document == null)
			{
				try
				{
					DOMParser parser = new DOMParser();
					parser.parse(new InputSource(new FileInputStream(_file)));
					_document = parser.getDocument();
				}
				catch (Exception e)
				{
					throw XcapException.newInternalError(e);
				}
			}
			return _document;
		}
		
		protected File getFile()
		{
			return _file;
		}
		
		protected void setModified(boolean modified)
		{
			_modified = modified;
		}
	}




}
