package org.cipango.kaleo.xcap.util;

import java.io.IOException;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class ResourceEntityResolver implements EntityResolver {

	public InputSource resolveEntity(String publicId, String systemId)
			throws SAXException, IOException {
		return new InputSource(
				 ResourceEntityResolver.class.getResourceAsStream("/schema/" + systemId));

	}

}
