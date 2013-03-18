package com.adobe.dp.epub.dtd;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class EPUBEntityResolver implements EntityResolver {

	Hashtable systemIdMap = new Hashtable();

	EPUBEntityResolver() {
		// fully-resolved names
		systemIdMap.put("http://www.idpf.org/dtds/2007/opf.dtd", "opf20.dtd");
		systemIdMap.put("http://openebook.org/dtds/oeb-1.2/oeb12.ent", "oeb12.dtdinc");
		systemIdMap.put("http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd", "xhtml1-transitional.dtd");
		systemIdMap.put("http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd", "xhtml1-strict.dtd");
		systemIdMap.put("http://www.w3.org/TR/xhtml1/DTD/xhtml-lat1.ent", "xhtml-lat1.dtdinc");
		systemIdMap.put("http://www.w3.org/TR/xhtml1/DTD/xhtml-symbol.ent", "xhtml-symbol.dtdinc");
		systemIdMap.put("http://www.w3.org/TR/xhtml1/DTD/xhtml-special.ent", "xhtml-special.dtdinc");
		systemIdMap.put("http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd", "svg11.dtd");
		systemIdMap.put("http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd", "opf20.dtd");
		systemIdMap.put("http://www.daisy.org/z3986/2005/dtbook-2005-2.dtd", "dtbook-2005-2.dtd");
		systemIdMap.put("http://www.daisy.org/z3986/2005/ncx-2005-1.dtd", "dtd/ncx-2005-1.dtd");
	}

	public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
		InputStream in = null;
		if (systemIdMap != null) {
			String res = (String) systemIdMap.get(systemId);
			if (res != null)
				in = EPUBEntityResolver.class.getResourceAsStream(res);
		}
		if( in == null ) {
			in = new ByteArrayInputStream(new byte[0]);
			System.err.println("Unknown systemId: " + systemId);
		}
		InputSource source = new InputSource(in);
		source.setSystemId(systemId);
		return source;
	}

	public static final EPUBEntityResolver instance = new EPUBEntityResolver();
}
