/*******************************************************************************
 * Copyright (c) 2009, Adobe Systems Incorporated
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 *
 * ·        Redistributions of source code must retain the above copyright 
 *          notice, this list of conditions and the following disclaimer. 
 *
 * ·        Redistributions in binary form must reproduce the above copyright 
 *		   notice, this list of conditions and the following disclaimer in the
 *		   documentation and/or other materials provided with the distribution. 
 *
 * ·        Neither the name of Adobe Systems Incorporated nor the names of its 
 *		   contributors may be used to endorse or promote products derived from
 *		   this software without specific prior written permission. 
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR 
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/

package com.adobe.dp.epub.opf;

import com.adobe.dp.epub.dtd.EPUBEntityResolver;
import com.adobe.dp.epub.io.ContainerSource;
import com.adobe.dp.epub.util.PathUtil;
import com.adobe.dp.epub.util.Translit;
import com.adobe.dp.xml.util.SMapImpl;
import com.adobe.dp.xml.util.XMLSerializer;
import com.sun.org.apache.xalan.internal.xsltc.runtime.Hashtable;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

public class OPFResource extends Resource {

	static final String opfns = "http://www.idpf.org/2007/opf";

	static final String opfmedia = "application/oebps-package+xml";

	static final String dcns = "http://purl.org/dc/elements/1.1/";

	private static final String dctermsns = "http://purl.org/dc/terms/";

	private static final String xsins = "http://www.w3.org/2001/XMLSchema-instance";

	class XMLHandler extends DefaultHandler {

		String section;

		String uniqueIdentifier;

		String metaName;

		String metaNS;

		StringBuffer metaValue = new StringBuffer();

		boolean primaryId;

		Hashtable idmap = new Hashtable();

		String base;

		XMLHandler(String base) {
			this.base = base;
		}

		public void characters(char[] ch, int start, int length) throws SAXException {
			if (metaName != null)
				metaValue.append(ch, start, length);
		}

		public void endElement(String uri, String localName, String name) throws SAXException {
			if (uri.equals(opfns)) {
				if (section != null && localName.equals(section))
					section = null;
			}
			if (metaName != null) {
				if (primaryId)
					epub.addPrimaryIdentifier(metaValue.toString());
				else
					epub.addMetadata(metaNS, metaName, metaValue.toString());
				metaName = null;
				metaNS = null;
				metaValue.delete(0, metaValue.length());
				primaryId = false;
			}
		}

		public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
			if (uri.equals(opfns)) {
				if (localName.equals("package")) {
					uniqueIdentifier = attributes.getValue("unique-identifier");
					return;
				}
				if (localName.equals("metadata") || localName.equals("manifest")) {
					section = localName;
					return;
				}
				if (localName.equals("spine")) {
					section = "spine";
					String toc = attributes.getValue("toc");
					if (toc != null) {
						Object res = idmap.get(toc);
						if (res != null)
							epub.toc = (Resource) res;
					}
					return;
				}
				if (section.equals("metadata")) {
					if (localName.equals("meta")) {
						String mname = attributes.getValue("name");
						String mvalue = attributes.getValue("content");
						epub.addMetadata(null, mname, mvalue);
					}
				} else if (section.equals("manifest")) {
					if (localName.equals("item")) {
						String href = attributes.getValue("href");
						String type = attributes.getValue("media-type");
						String id = attributes.getValue("id");
						if (href != null) {
							href = PathUtil.resolveRelativeReference(base, href);
							try {
								Resource res = epub.loadResource(href, type);
								if (res != null && id != null)
									idmap.put(id, res);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
				} else if (section.equals("spine")) {
					if (localName.equals("itemref")) {
						String idref = attributes.getValue("idref");
						if (idref != null) {
							Object res = idmap.get(idref);
							if (res != null)
								epub.addToSpine((Resource) res);
						}
					}
				}
			} else if (section.equals("metadata")) {
				if (metaName == null) {
					metaNS = uri;
					metaName = localName;
					if (metaNS.equals(dcns) && metaName.equals("identifier")) {
						String id = attributes.getValue("id");
						primaryId = id != null && id.equals(uniqueIdentifier);
					}
				}
			}
		}
	}

	OPFResource(Publication epub, String name) {
		super(epub, name, opfmedia, null);
	}

	public void load(ContainerSource container, String opfName) throws IOException {
		InputStream in = container.getDataSource(opfName).getInputStream();
		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setNamespaceAware(true);
		try {
			SAXParser parser = factory.newSAXParser();
			XMLReader reader = parser.getXMLReader();
			XMLHandler handler = new XMLHandler(opfName);
			reader.setContentHandler(handler);
			reader.setEntityResolver(EPUBEntityResolver.instance);
			InputSource source = new InputSource(in);
			reader.parse(source);
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			throw new RuntimeException(e.toString());
		} catch (SAXException e) {
			throw new IOException("XML Syntax error in OPF: " + e.getMessage());
		} finally {
			try {
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void serialize(OutputStream out) throws IOException {
		XMLSerializer ser = new XMLSerializer(out);
		ser.startDocument("1.0", "UTF-8");
		SMapImpl nsmap = new SMapImpl();
		nsmap.put(null, "opf", opfns);
		nsmap.put(null, "dc", dcns);
		nsmap.put(null, "dcterms", dctermsns);
		nsmap.put(null, "xsi", xsins);
		ser.setPreferredPrefixMap(nsmap);
		SMapImpl attrs = new SMapImpl();
		attrs.put(null, "version", "2.0");
		attrs.put(null, "unique-identifier", "bookid");
		ser.startElement(opfns, "package", attrs, true);
		ser.newLine();
		ser.startElement(opfns, "metadata", null, false);
		ser.newLine();

        BitmapImageResource coverImage = epub.getCoverImage();
        if (coverImage != null) {
            epub.addMetadata(null, "cover", epub.assignId(coverImage));
        }


		Iterator it = epub.metadata.iterator();
		int identifierCount = 0;
		while (it.hasNext()) {
			Publication.SimpleMetadata item = (Publication.SimpleMetadata) it.next();
			if (item.ns != null && item.ns.equals(dcns) && item.name.equals("identifier")) {
				attrs = new SMapImpl();
				attrs.put(null, "id", (identifierCount == 0 ? "bookid" : "bookid" + identifierCount));
				identifierCount++;
			} else {
				attrs = null;
			}
			String value = item.value;
			if (epub.isTranslit())
				value = Translit.translit(value);
			if (item.ns == null) {
				attrs = new SMapImpl();
				attrs.put(null, "name", item.name);
				attrs.put(null, "content", value);
				ser.startElement(opfns, "meta", attrs, false);
				ser.endElement(opfns, "meta");
				ser.newLine();
			} else {
				ser.startElement(item.ns, item.name, attrs, false);
				char[] arr = value.toCharArray();
				ser.text(arr, 0, arr.length);
				ser.endElement(item.ns, item.name);
				ser.newLine();
			}
		}
		ser.endElement(opfns, "metadata");
		ser.newLine();
		ser.startElement(opfns, "manifest", null, false);
		ser.newLine();
		it = epub.resources();
		while (it.hasNext()) {
			Resource r = (Resource) it.next();
			if (r != this) {
				attrs = new SMapImpl();
				attrs.put(null, "id", epub.assignId(r));
				attrs.put(null, "href", makeReference(r.getName(), null));
				attrs.put(null, "media-type", r.mediaType);
				ser.startElement(opfns, "item", attrs, false);
				ser.endElement(opfns, "item");
				ser.newLine();
			}
		}
		ser.endElement(opfns, "manifest");
		ser.newLine();
		attrs = new SMapImpl();
		if (epub.toc != null)
			attrs.put(null, "toc", epub.assignId(epub.toc));
		if (epub.pageMap != null)
			attrs.put(null, "page-map", epub.assignId(epub.pageMap));
		ser.startElement(opfns, "spine", attrs, false);
		ser.newLine();
		it = epub.spine();
		while (it.hasNext()) {
			Resource r = (Resource) it.next();
			attrs = new SMapImpl();
			attrs.put(null, "idref", epub.assignId(r));
			ser.startElement(opfns, "itemref", attrs, false);
			ser.endElement(opfns, "itemref");
			ser.newLine();
		}
		ser.endElement(opfns, "spine");
		ser.newLine();
        if (epub.toc != null || coverImage != null) {
            ser.startElement(opfns, "guide", null, false);

            Resource tocHtml = epub.getResourceByName("OPS/content.xhtml");
            if (tocHtml != null) {
                attrs = new SMapImpl();
                attrs.put(null, "type", "toc");
                attrs.put(null, "title", "Table of content");
                attrs.put(null, "href", "content.xhtml");
                ser.startElement(opfns, "reference", attrs, false);
                ser.endElement(opfns, "reference");
                ser.newLine();
            }
            if (coverImage != null) {
                attrs = new SMapImpl();
                attrs.put(null, "type", "cover");
                attrs.put(null, "title", "cover");
                String coverImageName = coverImage.getName().substring(epub.getContentFolder().length() + 1);
                attrs.put(null, "href", coverImageName);
                ser.startElement(opfns, "reference", attrs, false);
                ser.endElement(opfns, "reference");
                ser.newLine();
            }
            ser.endElement(opfns, "guide");

        }


		ser.endElement(opfns, "package");
		ser.newLine();
		ser.endDocument();
	}
}
