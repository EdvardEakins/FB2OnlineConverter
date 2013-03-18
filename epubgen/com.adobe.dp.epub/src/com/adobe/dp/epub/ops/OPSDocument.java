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

package com.adobe.dp.epub.ops;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import com.adobe.dp.css.CSSStylesheet;
import com.adobe.dp.css.CascadeEngine;
import com.adobe.dp.epub.dtd.EPUBEntityResolver;
import com.adobe.dp.epub.io.DataSource;
import com.adobe.dp.epub.opf.OPSResource;
import com.adobe.dp.epub.opf.Resource;
import com.adobe.dp.epub.opf.ResourceRef;
import com.adobe.dp.epub.opf.StyleResource;
import com.adobe.dp.epub.otf.FontSubsetter;
import com.adobe.dp.epub.style.Stylesheet;
import com.adobe.dp.xml.util.SMapImpl;
import com.adobe.dp.xml.util.XMLSerializer;

public class OPSDocument {

	OPSResource resource;

	Hashtable idMap = new Hashtable();

	Element body;

	Vector styleResources = new Vector();

	XRef rootXRef;

	int nextid = 1;

	public static final String xhtmlns = "http://www.w3.org/1999/xhtml";

	public static final String svgns = "http://www.w3.org/2000/svg";

	public static final String xlinkns = "http://www.w3.org/1999/xlink";

	public static final String xmlns = "http://www.w3.org/XML/1998/namespace";

	public OPSDocument(OPSResource resource) {
		this.resource = resource;
		if (resource.getMediaType().equals("image/svg+xml"))
			body = new SVGElement(this, "svg");
		else
			body = new HTMLElement(this, "body");
	}

	public int assignPlayOrder(int playOrder) {
		XRef rootXRef = getRootXRef();
		if (rootXRef.playOrderNeeded())
			rootXRef.setPlayOrder(++playOrder);
		return body.assignPlayOrder(playOrder);
	}

	public Element getBody() {
		return body;
	}

	public Element getElementById(String id) {
		return (Element) idMap.get(id);
	}

	void setElementId(Element e, String id) {
		if (e.id != null)
			idMap.remove(e.id);
		Element old = (Element) idMap.put(id, e);
		ResourceRef ref = resource.getResourceRef();
		XRef xref = ref.takeOverUnresolvedXRef(id);
		if (xref != null) {
			xref.targetElement = e;
		}
		e.id = id;
		if (old != null) {
			old.id = null;
			if (old.selfRef != null)
				assignId(old);
		}
	}

	private String newId() {
		while (true) {
			String id = "id" + nextid;
			if (idMap.get(id) == null)
				return id;
			nextid++;
		}
	}

	String assignId(Element e) {
		String id = e.id;
		if (id == null) {
			id = newId();
			e.id = id;
			idMap.put(id, e);
		}
		return id;
	}

	public Iterator styleResources() {
		return styleResources.iterator();
	}

	public void addStyleResource(ResourceRef style) {
		if (style == null)
			throw new IllegalArgumentException("null style");
		styleResources.add(style);
	}

	public void addStyleResource(Resource style) {
		if (style == null)
			throw new IllegalArgumentException("null style");
		styleResources.add(style.getResourceRef());
	}

	public XRef getRootXRef() {
		if (rootXRef == null) {
			rootXRef = resource.getResourceRef().takeOverUnresolvedXRef(null);
			if (rootXRef == null)
				rootXRef = new XRef(resource, (Element) null);
		}
		return rootXRef;
	}

	public HTMLElement createElement(String name) {
		return new HTMLElement(this, name);
	}

	public HyperlinkElement createHyperlinkElement(String name) {
		return new HyperlinkElement(this, name);
	}

	public TableCellElement createTableCellElement(String name, String align, int colSpan, int rowSpan) {
		return new TableCellElement(this, name, align, colSpan, rowSpan);
	}

	public ImageElement createImageElement(String name) {
		return new ImageElement(this, name);
	}

	public SVGElement createSVGElement(String name) {
		return new SVGElement(this, name);
	}

	public SVGImageElement createSVGImageElement(String name) {
		return new SVGImageElement(this, name);
	}

	public void addFonts(FontSubsetter subsetter, StyleResource styleResource) {
		if (!styleResources.contains(styleResource.getResourceRef()))
			return;
		subsetter.setStyles(styleResources);
		body.addFonts(subsetter);
	}

	public void serialize(OutputStream out) throws IOException {
		XMLSerializer ser = new XMLSerializer(out);
		serialize(ser);
	}

	public int getEstimatedSize() {
		return 200 + getBody().getEstimatedSize();
	}

	/**
	 * Split the document. Leave enough content in this document to produce
	 * resource about targetSize bytes; peel off the rest of the content and
	 * place it in the newResource.
	 * 
	 * @param newDoc
	 *            document where peeled content should be placed
	 * @param targetSize
	 *            target size of this document after split
	 * @return true if something was peeled
	 */
	public boolean peelOffBack(OPSDocument newDoc, int targetSize) {
		newDoc.styleResources.addAll(styleResources);
		Element newBody = body.peelElements(newDoc, targetSize, true);
		if (newBody == null)
			return false;
		newDoc.body = newBody;
		return true;
	}

	public void cascadeStyles() {
		CascadeEngine engine = new CascadeEngine();
		Iterator s = styleResources();
		while (s.hasNext()) {
			ResourceRef ref = (ResourceRef) s.next();
			Resource r = ref.getResource();
			if (r instanceof StyleResource) {
				StyleResource sr = (StyleResource) r;
				Stylesheet stylesheet = sr.getStylesheet();
				if (stylesheet != null) {
					CSSStylesheet css = stylesheet.getCSS();
					if (css != null)
						engine.add(css, null);
				}
			}
		}

		// TODO: SVG style elements

		boolean notSVG = resource.getMediaType().equals("image/svg+xml");
		if (notSVG) {
			engine.pushElement(xhtmlns, "html", null);
			engine.pushElement(xhtmlns, "head", null);
			engine.popElement();
		}
		getBody().cascade(engine);
		if (notSVG) {
			engine.popElement();
		}
	}

	public void generateStyles(Stylesheet stylesheet) {
		getBody().generateStyles(stylesheet);
	}

	public void setAssignStylesFlag() {
		getBody().setAssignStylesFlag();
	}

	public void serialize(XMLSerializer ser) {
		ser.startDocument("1.0", "UTF-8");
		boolean isSVG = resource.getMediaType().equals("image/svg+xml");
		if (isSVG) {
			Iterator s = styleResources();
			while (s.hasNext()) {
				ResourceRef sr = (ResourceRef) s.next();
				String href = resource.makeReference(sr.getResourceName(), null);
				ser.processingInstruction("xml-stylesheet", "href=\"" + href + "\" type=\"text/css\"");
			}
			getBody().serialize(ser);
		} else {
			ser.startElement(xhtmlns, "html", null, true);
			ser.newLine();
			ser.startElement(xhtmlns, "head", null, false);
			ser.newLine();
			ser.startElement(xhtmlns, "title", null, false);
			ser.endElement(xhtmlns, "title");
			ser.newLine();
			Iterator s = styleResources();
			while (s.hasNext()) {
				ResourceRef sr = (ResourceRef) s.next();
				String href = resource.makeReference(sr.getResourceName(), null);
				SMapImpl attr = new SMapImpl();
				attr.put(null, "rel", "stylesheet");
				attr.put(null, "type", "text/css");
				attr.put(null, "href", href);
				ser.startElement(xhtmlns, "link", attr, false);
				ser.endElement(xhtmlns, "link");
				ser.newLine();
			}
			ser.endElement(xhtmlns, "head");
			ser.newLine();
			getBody().serialize(ser);
			ser.newLine();
			ser.endElement(xhtmlns, "html");
		}
		ser.newLine();
		ser.endDocument();
	}

	public void load(DataSource data) throws IOException {
		OPSDocumentBuilder builder = new OPSDocumentBuilder(this);
		InputStream in = data.getInputStream();
		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setNamespaceAware(true);
		try {
			SAXParser parser = factory.newSAXParser();
			XMLReader reader = parser.getXMLReader();
			reader.setContentHandler(builder);
			reader.setEntityResolver(EPUBEntityResolver.instance);
			InputSource source = new InputSource(in);
			reader.parse(source);
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			throw new RuntimeException(e.toString());
		} catch (SAXException e) {
			throw new IOException("XML Syntax error in " + resource.getName() + ": " + e.getMessage());
		} finally {
			try {
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void removeAllStyleResources() {
		styleResources.clear();
	}
}
