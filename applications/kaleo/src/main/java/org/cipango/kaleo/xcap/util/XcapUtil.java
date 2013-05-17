package org.cipango.kaleo.xcap.util;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.cipango.kaleo.xcap.XcapException;
import org.iso_relax.verifier.Schema;
import org.iso_relax.verifier.Verifier;
import org.iso_relax.verifier.VerifierConfigurationException;
import org.iso_relax.verifier.VerifierFactory;
import org.iso_relax.verifier.VerifierHandler;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class XcapUtil
{
	/**
	 * Logger for this class
	 */
	protected static final Logger log = Logger.getLogger(XcapUtil.class);

	private static VerifierFactory _factory;
	
	static 
	{
		_factory = new com.sun.msv.verifier.jarv.TheFactoryImpl();
		_factory.setEntityResolver(new ResourceEntityResolver());
	}

	public static Schema getSchema(String xsdResource)
	{
		try
		{
			return _factory.compileSchema(XcapUtil.class
					.getResourceAsStream(xsdResource));
		} catch (Exception e)
		{
			log.error("Unable to compile schema: " + xsdResource, e);
			return null;
		}
	}

	public static void validate(Document document, Schema schema)
			throws XcapException
	{
		try
		{
			Verifier verifier = schema.newVerifier();
			verifier.setErrorHandler(new ErrorHandler()
			{
				public void error(SAXParseException saxParseEx)
				{
					log.error("Error during validation.", saxParseEx);
				}

				public void fatalError(SAXParseException saxParseEx)
				{
					log.fatal("Fatal error during validation.", saxParseEx);
				}

				public void warning(SAXParseException saxParseEx)
				{
					log.warn(saxParseEx);
				}
			});

			VerifierHandler handler = verifier.getVerifierHandler();
			verifier.verify(document);

			if (!handler.isValid())
			{
				XcapException e1 = new XcapException(
						"Unable to validate document after insertion",
						HttpServletResponse.SC_CONFLICT);
				StringBuffer sb = new StringBuffer();
				sb.append(XcapException.XCAP_ERROR_HEADER);
				sb.append("<schema-validation-error/>");
				sb.append(XcapException.XCAP_ERROR_FOOTER);
				e1.setContent(XcapException.XCAP_ERROR_CONTENT_TYPE, sb
						.toString().getBytes());
				throw e1;
			}
		} catch (XcapException e)
		{
			throw e;
		} catch (Throwable e)
		{
			XcapException e1 = new XcapException(
					"Unable to validate document after insertion",
					HttpServletResponse.SC_CONFLICT, e);
			StringBuffer sb = new StringBuffer();
			sb.append(XcapException.XCAP_ERROR_HEADER);
			sb.append("<schema-validation-error/>");
			sb.append(XcapException.XCAP_ERROR_FOOTER);

			e1.setContent(XcapException.XCAP_ERROR_CONTENT_TYPE, sb.toString()
					.getBytes());
			throw e1;
		}
	}

	public static boolean validate(Node node, Schema schema) throws SAXException, VerifierConfigurationException
	{

		Verifier verifier = schema.newVerifier();
		verifier.setErrorHandler(new ErrorHandler()
		{
			public void error(SAXParseException saxParseEx)
			{
				log.error("Error during validation.", saxParseEx);
			}

			public void fatalError(SAXParseException saxParseEx)
			{
				log.fatal("Fatal error during validation.", saxParseEx);
			}

			public void warning(SAXParseException saxParseEx)
			{
				log.warn(saxParseEx);
			}
		});

		VerifierHandler handler = verifier.getVerifierHandler();
		verifier.verify(node);

		return handler.isValid();
	}



	/**
	 * Input: /ruleset/rule[@id=\"a\"]/conditions Output:
	 * /cp:ruleset/cp:rule[@id=\"a\"]/cp:conditions
	 */
	public static String insertDefaultNamespace(String nodeSelector,
			String defaultNamespace)
	{
		StringBuffer sb = new StringBuffer(nodeSelector);
		int indexSlash = sb.indexOf("/");
		int nextSlash;
		int nsSepIndex;
		int indexQuoteMark;
		while (indexSlash != -1)
		{
			nextSlash = sb.indexOf("/", indexSlash + 1);
			nsSepIndex = sb.indexOf(":", indexSlash);
			indexQuoteMark = sb.indexOf("\"", indexSlash);
			if (indexQuoteMark != -1 && indexQuoteMark < nsSepIndex)
				nsSepIndex = -1;
			boolean slashLastChar = indexSlash + 1 == sb.length();
			boolean isAttribute = !slashLastChar && sb.charAt(indexSlash + 1) == '@';
			boolean hasNamespacePrefix = nsSepIndex != -1
					&& (nsSepIndex < nextSlash || nextSlash == -1);
			if (!isAttribute && !hasNamespacePrefix && !slashLastChar)
			{
				sb.insert(indexSlash + 1, defaultNamespace + ":");
			}
			indexSlash = sb.indexOf("/", indexSlash + 1);
		}

		return sb.toString();
	}

}
