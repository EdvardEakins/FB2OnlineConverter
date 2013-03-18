package com.adobe.dp.epub.ops;

import java.io.IOException;
import java.io.StringReader;
import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import com.adobe.dp.css.CSSParser;
import com.adobe.dp.css.CSSStylesheet;
import com.adobe.dp.css.InlineRule;
import com.adobe.dp.epub.opf.Publication;
import com.adobe.dp.epub.opf.ResourceRef;
import com.adobe.dp.epub.opf.StyleResource;
import com.adobe.dp.epub.style.EPUBCSSURLFactory;
import com.adobe.dp.epub.util.PathUtil;

class OPSDocumentBuilder extends DefaultHandler {

	Publication epub;

	OPSDocument document;

	boolean isSVG;

	StringBuffer text = new StringBuffer();

	Stack elementStack = new Stack();

	OPSDocumentBuilder(OPSDocument document) {
		this.document = document;
		epub = document.resource.getPublication();
		isSVG = document.resource.getMediaType().equals("image/svg+xml");
	}

	private Element createElement(String uri, String local) throws SAXException {
		if (uri.equals(OPSDocument.xhtmlns)) {
			if (local.equals("img"))
				return document.createImageElement("img");
			if (local.equals("a"))
				return document.createHyperlinkElement("a");
			if (local.equals("td") || local.equals("th"))
				return document.createTableCellElement(local, null, 1, 1);
			return document.createElement(local);
		} else if (uri.equals(OPSDocument.svgns)) {
			if (local.equals("image"))
				return document.createSVGImageElement("image");
			return document.createSVGElement(local);
		} else {
			throw new SAXException("Unsupported namespace: " + uri);
		}
	}

	private XRef makeXRef(String href) {
		href = PathUtil.resolveRelativeReference(document.resource.getName(), href);
		int hash = href.indexOf('#');
		String path;
		String id;
		if (hash < 0) {
			path = href;
			id = null;
		} else {
			path = href.substring(0, hash);
			id = href.substring(hash + 1);
		}
		ResourceRef resref = epub.getResourceRef(path);
		if( resref == null )
			return null;
		return resref.getXRef(id);
	}

	private void assignAttributes(Element element, Attributes attributes) throws SAXException {
		String id = attributes.getValue("id");
		if (id != null)
			element.setId(id);
		String className = attributes.getValue("class");
		if (className != null)
			element.setClassName(className);
		String styleStr = attributes.getValue("style");
		InlineRule style = null;
		if (styleStr != null) {
			CSSParser parser = new CSSParser();
			parser.setCSSURLFactory(new EPUBCSSURLFactory(document.resource));
			style = parser.readInlineStyle(styleStr);
		}
		if (element instanceof ImageElement) {
			ImageElement imageElement = (ImageElement) element;
			String src = attributes.getValue("src");
			if (src != null) {
				String srcName = PathUtil.resolveRelativeReference(document.resource.getName(), src);
				imageElement.setImageResource(epub.getResourceRef(srcName));
			}
		} else if (element instanceof HyperlinkElement) {
			HyperlinkElement hyperlink = (HyperlinkElement) element;
			String href = attributes.getValue("href");
			if (href != null) {
				if (href.matches("^[a-z0-9]+:.*"))
					hyperlink.setExternalHRef(href);
				else {
					XRef xref = makeXRef(href);
					if (xref == null)
						System.err.println("Could not resolve: " + href);
					else
						hyperlink.setXRef(xref);
				}
			}
		} else if (element instanceof TableCellElement) {
			TableCellElement tc = (TableCellElement) element;
			String val = attributes.getValue("colspan");
			if (val != null) {
				try {
					tc.setColSpan(Integer.parseInt(val));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			val = attributes.getValue("rowspan");
			if (val != null) {
				try {
					tc.setRowSpan(Integer.parseInt(val));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			val = attributes.getValue("align");
			if (val != null)
				tc.setAlign(val);
		} else if (element instanceof SVGElement) {
			SVGElement svg = (SVGElement) element;
			int count = attributes.getLength();
			for (int i = 0; i < count; i++) {
				String uri = attributes.getURI(i);
				String name = attributes.getLocalName(i);
				if ((uri.equals("") && (name.equals("class") || name.equals("style") || name.equals("id")))
						|| (uri.equals(OPSDocument.xmlns) && name.equals("lang")))
					continue;
				String value = attributes.getValue(i);
				if (uri.equals(""))
					svg.setAttribute(name, value);
				else
					svg.setAttribute(uri, name, value);
			}
		}
		if (style != null)
			element.setStyle(style);
	}

	private String flushText() {
		if (text.length() > 0) {
			String str = text.toString();
			if (!elementStack.isEmpty()) {
				Element parent = (Element) elementStack.peek();
				if (!parent.isSection())
					parent.add(str);
			}
			text.setLength(0);
			return str;
		}
		return null;
	}

	public void characters(char[] ch, int start, int length) throws SAXException {
		text.append(ch, start, length);
	}

	public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
		text.append(ch, start, length);
	}

	public void startDocument() throws SAXException {
	}

	public void endDocument() throws SAXException {
	}

	public void startPrefixMapping(String prefix, String uri) throws SAXException {
	}

	public void endPrefixMapping(String prefix) throws SAXException {
	}

	public void warning(SAXParseException e) throws SAXException {
	}

	public void error(SAXParseException e) throws SAXException {
	}

	public void fatalError(SAXParseException e) throws SAXException {
	}

	public void processingInstruction(String target, String data) throws SAXException {
	}

	public void setDocumentLocator(Locator locator) {
	}

	public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
		flushText();
		Element element;
		if (uri.equals(OPSDocument.xhtmlns)) {
			if (elementStack.isEmpty()) {
				if (localName.equals("body")) {
					element = document.getBody();
				} else {
					if (localName.equals("link")) {
						String rel = attributes.getValue("rel");
						if (rel.equals("stylesheet")) {
							String href = attributes.getValue("href");
							if (href != null) {
								href = PathUtil.resolveRelativeReference(document.resource.getName(), href);
								document.addStyleResource(epub.getResourceRef(href));
							}
						}
					}
					return;
				}
			} else {
				element = createElement(uri, localName);
			}
		} else {
			element = createElement(uri, localName);
		}
		assignAttributes(element, attributes);
		if (!elementStack.isEmpty()) {
			Element parent = (Element) elementStack.peek();
			parent.add(element);
		}
		elementStack.push(element);
	}

	public void endElement(String uri, String localName, String name) throws SAXException {
		String content = flushText();
		if (uri.equals(OPSDocument.xhtmlns)) {
			if (localName.equals("style")) {
				CSSParser parser = new CSSParser();
				parser.setCSSURLFactory(new EPUBCSSURLFactory(document.resource));
				try {
					CSSStylesheet css = parser.readStylesheet(new StringReader(content));
					String inlineName = PathUtil.resolveRelativeReference(document.resource.getName(), "inline.css");
					StyleResource sr = epub.createStyleResource(inlineName);
					sr.setCSS(css);
					document.addStyleResource(sr.getResourceRef());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		if (!elementStack.isEmpty()) {
			elementStack.pop();
		}
	}

}
