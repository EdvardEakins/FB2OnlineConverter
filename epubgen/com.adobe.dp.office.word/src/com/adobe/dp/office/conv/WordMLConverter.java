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

package com.adobe.dp.office.conv;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Stack;
import java.util.StringTokenizer;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import com.adobe.dp.css.CSSLength;
import com.adobe.dp.css.CSSNumber;
import com.adobe.dp.css.CSSParser;
import com.adobe.dp.css.InlineRule;
import com.adobe.dp.epub.io.BufferedDataSource;
import com.adobe.dp.epub.io.ContainerSource;
import com.adobe.dp.epub.io.DataSource;
import com.adobe.dp.epub.io.StringDataSource;
import com.adobe.dp.epub.opf.OPSResource;
import com.adobe.dp.epub.opf.Publication;
import com.adobe.dp.epub.opf.Resource;
import com.adobe.dp.epub.opf.StyleResource;
import com.adobe.dp.epub.ops.Element;
import com.adobe.dp.epub.ops.HyperlinkElement;
import com.adobe.dp.epub.ops.ImageElement;
import com.adobe.dp.epub.ops.OPSDocument;
import com.adobe.dp.epub.ops.SVGElement;
import com.adobe.dp.epub.ops.SVGImageElement;
import com.adobe.dp.epub.ops.XRef;
import com.adobe.dp.office.drawing.PictureData;
import com.adobe.dp.office.metafile.EMFParser;
import com.adobe.dp.office.metafile.WMFParser;
import com.adobe.dp.office.vml.VMLGroupElement;
import com.adobe.dp.office.vml.VMLPathConverter;
import com.adobe.dp.office.word.BRElement;
import com.adobe.dp.office.word.BodyElement;
import com.adobe.dp.office.word.ContainerElement;
import com.adobe.dp.office.word.DrawingElement;
import com.adobe.dp.office.word.FootnoteElement;
import com.adobe.dp.office.word.FootnoteReferenceElement;
import com.adobe.dp.office.word.LastRenderedPageBreakElement;
import com.adobe.dp.office.word.NumberingLabel;
import com.adobe.dp.office.word.ParagraphElement;
import com.adobe.dp.office.word.ParagraphProperties;
import com.adobe.dp.office.word.PictElement;
import com.adobe.dp.office.word.RunElement;
import com.adobe.dp.office.word.RunProperties;
import com.adobe.dp.office.word.SmartTagElement;
import com.adobe.dp.office.word.Style;
import com.adobe.dp.office.word.TXBXContentElement;
import com.adobe.dp.office.word.TabElement;
import com.adobe.dp.office.word.TableCellElement;
import com.adobe.dp.office.word.TableCellProperties;
import com.adobe.dp.office.word.TableElement;
import com.adobe.dp.office.word.TableProperties;
import com.adobe.dp.office.word.TableRowElement;
import com.adobe.dp.office.word.TextElement;
import com.adobe.dp.office.word.WordDocument;

class WordMLConverter {

	private StyleConverter styleConverter;

	private HashSet listElements = new HashSet();

	private Publication epub;

	private OPSDocument chapter;

	private OPSResource resource;

	private WordDocument doc;

	private boolean hadSpace = false;

	private Hashtable footnoteMap;

	private Hashtable convResources = new Hashtable();

	private ContainerSource wordResources;

	private Hashtable metadataNS = new Hashtable();

	private static final String mediaFolder = "OPS/media/";

	private boolean hasEpubMetadata;

	boolean includeWordMetadata = true;

	boolean chapterNameWasSet;

	boolean chapterSplitAllowed;

	private String nextPageName;

	boolean useWordPageBreaks;

	StringBuffer styleAcc = new StringBuffer();

	StringBuffer injectAcc = new StringBuffer();

	PrintWriter log;

	String nextResourceName;

	String nextResourceMediaType;

	Stack nesting = new Stack();

	int lastNumId = -1;

	WordMLConverter(WordDocument doc, Publication epub, StyleConverter styleConverter, PrintWriter log) {
		this.log = log;
		this.doc = doc;
		this.epub = epub;
		this.styleConverter = styleConverter;
		this.metadataNS.put("DC", Publication.dcns);
		this.chapterSplitAllowed = true;
	}

	WordMLConverter(WordMLConverter parent, OPSResource chapterResource) {
		this.log = parent.log;
		this.resource = chapterResource;
		this.chapter = chapterResource.getDocument();
		this.doc = parent.doc;
		this.epub = parent.epub;
		this.styleConverter = new StyleConverter(true);
		this.chapterSplitAllowed = false;
	}

	WordMLConverter(WordMLConverter parent, StyleConverter styleConverter) {
		this.log = parent.log;
		this.chapter = parent.chapter;
		this.doc = parent.doc;
		this.epub = parent.epub;
		this.styleConverter = styleConverter;
		this.chapterSplitAllowed = false;
	}

	static class NestingItem {

		NestingItem(Element opsElement, int listLevel) {
			this.opsElement = opsElement;
			this.listLevel = listLevel;
		}

		Element opsElement;

		int listLevel;

		CSSLength topMargin;
	}

	class WMFResourceWriter implements ResourceWriter {

		public StreamAndName createResource(String base, String suffix, boolean doNotCompress) throws IOException {
			String name = mediaFolder + "wmf-" + base + suffix;
			name = epub.makeUniqueResourceName(name);
			BufferedDataSource dataSource = new BufferedDataSource();
			epub.createBitmapImageResource(name, "image/png", dataSource);
			return new StreamAndName(name.substring(mediaFolder.length()), dataSource.getOutputStream());
		}

	}

	class XMLInjector extends DefaultHandler {

		Stack nesting = new Stack();

		XMLInjector() {
			nesting.push(getCurrentOPSContainer());
		}

		Element parent() {
			return (Element) nesting.lastElement();
		}

		public void characters(char[] chars, int index, int len) throws SAXException {
			parent().add(new String(chars, index, len));
		}

		public void endElement(String uri, String localName, String qName) throws SAXException {
			nesting.pop();
		}

		public void startElement(String uri, String localName, String qName, Attributes attrs) throws SAXException {
			Element e = null;
			if (uri == null || uri.equals("") || uri.equals(OPSDocument.xhtmlns)) {
				if (localName.equals("th") || localName.equals("td")) {
					String align = attrs.getValue("align");
					String colspanStr = attrs.getValue("colspan");
					int colspan = 1;
					if (colspanStr != null) {
						try {
							colspan = Integer.parseInt(colspanStr);
						} catch (Exception ex) {
							ex.printStackTrace(log);
						}
					}
					String rowspanStr = attrs.getValue("rowspan");
					int rowspan = 1;
					if (rowspanStr != null) {
						try {
							rowspan = Integer.parseInt(rowspanStr);
						} catch (Exception ex) {
							ex.printStackTrace(log);
						}
					}
					e = chapter.createTableCellElement(localName, align, colspan, rowspan);
				} else if (localName.equals("img")) {
					ImageElement imageElement = chapter.createImageElement(localName);
					String src = attrs.getValue("src");
					if (src != null) {
						Resource imageResource = epub.getResourceByName("OPS/" + src);
						imageElement.setImageResource(imageResource);
					}
					e = imageElement;
				} else {
					e = chapter.createElement(localName);
				}
			} else if (uri.equals(OPSDocument.svgns)) {
				SVGElement svg;
				if (localName.equals("image")) {
					SVGImageElement svgImage = chapter.createSVGImageElement("image");
					String href = attrs.getValue(OPSDocument.xlinkns, "href");
					if (href != null) {
						Resource imageResource = epub.getResourceByName("OPS/" + href);
						svgImage.setImageResource(imageResource);
					}
					svg = svgImage;
				} else
					svg = chapter.createSVGElement(localName);
				int count = attrs.getLength();
				for (int i = 0; i < count; i++) {
					String attrNS = attrs.getURI(i);
					String attrName = attrs.getLocalName(i);
					if (attrNS.equals("")) {
						if (attrName.equals("id") || attrName.equals("style") || attrName.equals("class"))
							continue;
					}
					if (localName.equals("image") && attrNS.equals(OPSDocument.xlinkns) && attrName.equals("href"))
						continue;
					String attrValue = attrs.getValue(i);
					svg.setAttribute(attrNS, attrName, attrValue);
				}
				e = svg;
			}
			if (e == null) {
				// unknown container
				e = chapter.createElement("span");
			}
			String id = attrs.getValue("id");
			if (id != null)
				e.setId("id");
			String className = attrs.getValue("class");
			if (className != null)
				e.setClassName(className);
			String style = attrs.getValue("style");
			if (style != null) {
				CSSParser parser = new CSSParser();
				InlineRule parsedStyle = parser.readInlineStyle(style);
				e.setStyle(parsedStyle);
			}
			parent().add(e);
			nesting.push(e);
		}

		public InputSource resolveEntity(String publicId, String systemId) throws SAXException {
			throw new SAXException("External entities not allowed");
		}

	}

	Element getCurrentOPSContainer() {
		return ((NestingItem) nesting.peek()).opsElement;
	}

	NestingItem getOPSContainer(ParagraphElement wordElement, boolean forceNew) {
		ParagraphElement wp = (ParagraphElement) wordElement;
		ParagraphProperties pp = wp.getParagraphProperties();
		int level = -1;
		int numId = -1;
		NumberingLabel label = pp.getNumberingLabel();
		if (label != null) {
			level = label.getLevel();
			numId = label.getNumId();
		}
		NestingItem item = (NestingItem) nesting.peek();
		if (numId < 0 || (lastNumId >= 0 && lastNumId != numId)) {
			// end all lists
			while (true) {
				if (item.listLevel < 0)
					break;
				nesting.pop();
				item = (NestingItem) nesting.peek();
			}
		}
		if (numId <= 0 || !listElements.contains(wordElement)) {
			// possibly create nesting structure for non-list paragraphs
			boolean createDiv = true;
			if (item.opsElement.getElementName().equals("div")) {
				if (forceNew) {
					nesting.pop();
					item = (NestingItem) nesting.peek();
				} else {
					createDiv = false;
				}
			}
			if (createDiv) {
				Element e = chapter.createElement("div");
				item.opsElement.add(e);
				NestingItem div = new NestingItem(e, -1);
				nesting.push(div);
				item = div;
			}
		} else {
			// create nesting structure for list numId
			if (item.opsElement.getElementName().equals("div")) {
				nesting.pop();
				item = (NestingItem) nesting.peek();
			}
			lastNumId = numId;
			while (level > item.listLevel) {
				if (item.opsElement.getElementName().equals("ul")) {
					Element e = chapter.createElement("li");
					item.opsElement.add(e);
					e.setClassName("nested");
					NestingItem li = new NestingItem(e, item.listLevel);
					nesting.push(li);
					item = li;
				}
				Element e = chapter.createElement("ul");
				item.opsElement.add(e);
				NestingItem ul = new NestingItem(e, item.listLevel + 1);
				nesting.push(ul);
				item = ul;
			}
			while (level < item.listLevel || item.opsElement.getElementName().equals("li")) {
				nesting.pop();
				item = (NestingItem) nesting.peek();
			}
		}
		return item;
	}

	int pushOPSContainer(Element p) {
		int size = nesting.size();
		nesting.push(new NestingItem(p, -1));
		return size;
	}

	void restoreOPSContainer(int size) {
		nesting.setSize(size);
	}

	void useWordPageBreaks() {
		useWordPageBreaks = true;
	}

	void setFootnoteMap(Hashtable footnoteMap) {
		this.footnoteMap = footnoteMap;
	}

	Publication getPublication() {
		return epub;
	}

	private Resource getImageResource(PictureData data, String nameOverride, String mediaTypeOverride) {
		String src = data.getSrc();
		if (src == null)
			return null;
		String epubName = (String) convResources.get(src);
		if (epubName == null || nameOverride != null) {
			DataSource dataSource = wordResources.getDataSource(src);
			int index = src.lastIndexOf('/');
			String shortName = src.substring(index + 1);
			String mediaType;
			if (mediaTypeOverride != null)
				mediaType = mediaTypeOverride;
			else {
				mediaType = doc.getResourceMediaType(src);
				if (mediaType.equals("image/x-wmf")) {
					WMFResourceWriter dw = new WMFResourceWriter();
					GDISVGSurface svg = new GDISVGSurface(dw);
					try {
						WMFParser parser = new WMFParser(dataSource.getInputStream(), svg);
						parser.readAll();
					} catch (IOException e) {
						e.printStackTrace(log);
						return null;
					}
					dataSource = new StringDataSource(svg.getSVG());
					mediaType = "image/svg+xml";
					shortName = shortName + ".svg";
				} else if (mediaType.equals("application/octet-stream") && src.endsWith(".emf")) {
					// don't support EMF yet
					if (false) {
						WMFResourceWriter dw = new WMFResourceWriter();
						GDISVGSurface svg = new GDISVGSurface(dw);
						try {
							EMFParser parser = new EMFParser(dataSource.getInputStream(), svg);
							parser.readAll();
						} catch (IOException e) {
							// e.printStackTrace();
							e.printStackTrace(log);
							return null;
						}
						dataSource = new StringDataSource(svg.getSVG());
						mediaType = "image/svg+xml";
						shortName = shortName + ".svg";
					}
				}
			}
			if (nameOverride != null)
				epubName = "OPS/" + nameOverride;
			else
				epubName = mediaFolder + shortName;
			if (dataSource == null)
				return null;
			epub.createBitmapImageResource(epubName, mediaType, dataSource);
			if (nameOverride != null)
				convResources.put(src, epubName);
		}
		return epub.getResourceByName(epubName);
	}

	private void resetSpaceProcessing() {
		hadSpace = false;
	}

	private void treatAsSpace() {
		hadSpace = true;
	}

	private String processSpaces(String s) {
		int len = s.length();
		StringBuffer result = null;
		int last = 0;
		for (int i = 0; i < len; i++) {
			char c = s.charAt(i);
			if (c == ' ') {
				if (hadSpace) {
					if (result == null)
						result = new StringBuffer();
					result.append(s.substring(last, i));
					result.append('\u00A0'); // nbsp
					last = i + 1;
				} else {
					hadSpace = true;
				}
			} else {
				hadSpace = false;
			}
		}
		if (result != null) {
			result.append(s.substring(last));
			return result.toString();
		}
		return s;
	}

	void setImageWidth(Element img, String baseClassName, double widthPt, float emScale) {
		InlineRule rule = new InlineRule();
		if (styleConverter.usingPX()) {
			rule.set("width", new CSSLength(widthPt, "px"));
		} else {
			double defaultFontSize = styleConverter.defaultFontSize;
			double widthEM = widthPt / (emScale * defaultFontSize / 2);
			rule.set("width", new CSSLength(widthEM, "em"));
		}
		rule.set("max-width", new CSSLength(100, "%"));
		img.setStyle(rule);
		img.setClassName(baseClassName);
	}

	void parseAndInjectXML(StringBuffer xml) {
		try {
			XMLInjector injector = new XMLInjector();
			SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setNamespaceAware(true);
			SAXParser parser = factory.newSAXParser();
			XMLReader reader = parser.getXMLReader();
			reader.setContentHandler(injector);
			reader.setEntityResolver(injector);
			InputSource source = new InputSource(new StringReader(xml.toString()));
			source.setSystemId("");
			reader.parse(source);
		} catch (Exception e) {
			// e.printStackTrace();
			e.printStackTrace(log);
		}
	}

	String getMagicStyleName(ParagraphProperties pp) {
		if (pp == null)
			return null;
		Style ps = pp.getParagraphStyle();
		if (ps == null)
			return null;
		String name = ps.getName();
		if (name == null)
			return null;
		String ln = name.toLowerCase();
		if (ln.startsWith("*epub"))
			return ln.substring(5);
		return null;
	}

	String getMagicStyleName(RunProperties rp) {
		if (rp == null)
			return null;
		Style rs = rp.getRunStyle();
		if (rs == null)
			return null;
		String name = rs.getName();
		if (name == null)
			return null;
		String ln = name.toLowerCase();
		if (ln.startsWith("*epub"))
			return ln.substring(5);
		return null;
	}

	boolean processEPUBCommand(String command, int depth) {
		if (command.startsWith(".")) {
			// log.println("Command: " + command);
			int index = command.indexOf(' ');
			String cmd;
			String param;
			if (index > 0) {
				cmd = command.substring(1, index);
				param = command.substring(index + 1).trim();
			} else {
				cmd = command.substring(1);
				param = "";
			}
			if (cmd.equals("chapter")) {
				String newChapterName = "OPS/" + param;
				if (chapterNameWasSet) {
					if (!chapterSplitAllowed || depth > 1)
						log.println("chapter split not allowed here");
					else {
						resource = epub.createOPSResource(newChapterName);
						return false;
					}
				} else {
					epub.renameResource(resource, newChapterName);
					chapterNameWasSet = true;
				}
			} else if (cmd.equals("columns")) {
				InlineRule rule = new InlineRule();
				rule.set("oeb-column-number", new CSSNumber(Integer.parseInt(param)));
				resource.getDocument().getBody().setStyle(rule);
			} else if (cmd.equals("pageMap")) {
				if (param.length() == 0 || param.toLowerCase().startsWith("t") || param.equals("1"))
					epub.usePageMap();
			} else if (cmd.equals("translit")) {
				if (param.length() == 0 || param.toLowerCase().startsWith("t") || param.equals("1"))
					epub.setTranslit(true);
				else
					epub.setTranslit(false);
			} else if (cmd.equals("fontMangling")) {
				if (param.toLowerCase().equals("adobe"))
					epub.useAdobeFontMangling();
				else
					epub.useIDPFFontMangling();
			} else if (cmd.equals("resource")) {
				StringTokenizer tok = new StringTokenizer(param);
				nextResourceName = tok.nextToken();
				if (tok.hasMoreTokens())
					nextResourceMediaType = tok.nextToken();
				else
					nextResourceMediaType = null;
			} else if (cmd.equals("page")) {
				Element parent = getCurrentOPSContainer();
				if (!parent.content().hasNext()) {
					XRef xref = createOPSContainerXRefIfPossible();
					if (xref != null) {
						epub.getTOC().addPage(param, xref);
					} else {
						epub.getTOC().addPage(param, parent.getSelfRef());
					}
				} else {
					nextPageName = param;
				}
			}
		}
		return true;
	}

	XRef createOPSContainerXRefIfPossible() {
		int depth = nesting.size() - 1;
		Element lastGood = null;
		while (depth >= 0) {
			NestingItem item = (NestingItem) nesting.elementAt(depth--);
			Iterator content = item.opsElement.content();
			if (content.hasNext() && (content.next() != null && content.hasNext()))
				break;
			lastGood = item.opsElement;
		}
		if (lastGood != null)
			return lastGood.getSelfRef();
		return null;
	}

	void processEPUBMetadata(String command) {
		if (command.startsWith(".")) {
			StringTokenizer tok = new StringTokenizer(command);
			int count = tok.countTokens();
			String cmd = tok.nextToken();
			if (cmd.equals(".namespace")) {
				if (count == 3) {
					String prefix = tok.nextToken();
					String ns = tok.nextToken();
					metadataNS.put(prefix, ns);
				}
			} else if (cmd.equals(".includeWordMetadata")) {
				includeWordMetadata = true;
			}
		} else {
			int index = command.indexOf(' ');
			if (index > 0) {
				String name = command.substring(0, index);
				String value = command.substring(index + 1);
				index = name.indexOf('.');
				if (index > 0) {
					String prefix = name.substring(0, index);
					String ns = (String) metadataNS.get(prefix);
					if (ns != null) {
						name = name.substring(index + 1);
						epub.addMetadata(ns, name, value);
						return;
					}
				}
				epub.addMetadata(null, name, value);
			}
		}
	}

	void flushMagic() {
		if (injectAcc.length() > 0) {
			parseAndInjectXML(injectAcc);
			injectAcc.delete(0, injectAcc.length());
		}
		if (styleAcc.length() > 0) {
			try {
				CSSParser parser = new CSSParser();
				StyleResource global = (StyleResource) epub.getResourceByName("OPS/global.css");
				parser.readStylesheet(new StringReader(styleAcc.toString()), global.getStylesheet().getCSS());
			} catch (Exception e) {
				// e.printStackTrace();
				e.printStackTrace(log);
			}
			styleAcc.delete(0, styleAcc.length());
		}
	}

	boolean possiblyAddResource(com.adobe.dp.office.word.Element we) {
		if (nextResourceName == null)
			return false;
		Iterator it = we.content();
		while (it.hasNext()) {
			Object child = it.next();
			if (child instanceof DrawingElement) {
				DrawingElement wd = (DrawingElement) child;
				PictureData picture = wd.getPictureData();
				if (picture != null) {
					try {
						getImageResource(picture, nextResourceName, nextResourceMediaType);
						nextResourceName = null;
						nextResourceMediaType = null;
						return true;
					} catch (Exception e) {
						e.printStackTrace(log);
					}
				}
			} else if (child instanceof com.adobe.dp.office.word.Element) {
				com.adobe.dp.office.word.Element ce = (com.adobe.dp.office.word.Element) child;
				if (possiblyAddResource(ce))
					return true;
			}
		}
		return false;
	}

	ParagraphProperties getNonMagicParagraphProperties(com.adobe.dp.office.word.Element we) {
		if (we instanceof ParagraphElement) {
			ParagraphProperties pp = ((ParagraphElement) we).getParagraphProperties();
			if (getMagicStyleName(pp) == null)
				return pp;
		}
		return null;
	}

	boolean getNeigborCode(ParagraphProperties p1, ParagraphProperties p2) {
		if (p1 == null)
			return p2 == null;
		if (p2 == null)
			return false;
		return p1.sameStyle(p2);
	}

	boolean appendConvertedElement(com.adobe.dp.office.word.Element we, com.adobe.dp.office.word.Element prev,
			com.adobe.dp.office.word.Element next, float emScale, int depth, InlineRule tableCellProps) {
		Element conv = null;
		boolean addToParent = true;
		boolean resetSpaceProcessing = false;
		boolean ensureContent = false;
		if (we instanceof ParagraphElement) {
			ParagraphElement wp = (ParagraphElement) we;
			ParagraphProperties pp = wp.getParagraphProperties();
			String className = null;
			String elementName = null;
			String epubStyle = getMagicStyleName(pp);
			ensureContent = true;
			if (epubStyle != null) {
				if (epubStyle.startsWith("*command")) {
					if (possiblyAddResource(wp))
						return true;
					return processEPUBCommand(we.getTextContent(), depth);
				}
				if (epubStyle.startsWith("*style")) {
					styleAcc.append(we.getTextContent() + "\n");
					return true;
				}
				if (epubStyle.startsWith("*inject")) {
					injectAcc.append(we.getTextContent() + "\n");
					return true;
				}
				if (epubStyle.equals("*metadata")) {
					if (!hasEpubMetadata) {
						includeWordMetadata = false;
						hasEpubMetadata = true;
					}
					processEPUBMetadata(we.getTextContent());
					return true;
				}
				flushMagic();
				if (epubStyle.startsWith("-")) {
					int index = epubStyle.indexOf('.');
					if (index < 0) {
						elementName = epubStyle.substring(1);
						epubStyle = "";
					} else {
						elementName = epubStyle.substring(1, index);
						epubStyle = epubStyle.substring(index);
					}
				} else
					elementName = "p";
				if (epubStyle.startsWith("."))
					className = epubStyle.substring(1);
				if (conv == null && elementName != null) {
					conv = chapter.createElement(elementName);
					if (className != null)
						conv.setClassName(className);
					resetSpaceProcessing = true;
				} else {
					treatAsSpace();
				}
			} else {
				flushMagic();
				ParagraphProperties prevpp = getNonMagicParagraphProperties(prev);
				ParagraphProperties nextpp = getNonMagicParagraphProperties(next);
				boolean nc1 = getNeigborCode(prevpp, pp);
				boolean nc2 = getNeigborCode(pp, nextpp);
				StylingResult result = styleConverter.styleElement(pp, listElements.contains(we), emScale, nc1, nc2);

				NestingItem containerItem = getOPSContainer(wp, !nc1);
				if (!nc1 && result.containerRule != null)
					containerItem.topMargin = (CSSLength) result.containerRule.get("margin-top");

				// don't finalize container until last element of the same style
				if (!nc2) {
					if (containerItem.topMargin != null) {
						if (result.containerRule == null)
							result.containerRule = new InlineRule();
						result.containerRule.set("margin-top", containerItem.topMargin);
					}
					if (containerItem.opsElement != null) {
						containerItem.opsElement.setClassName(result.containerClassName);
						containerItem.opsElement.setDesiredCascadeResult(result.containerRule);
					}
				}
				elementName = result.elementName;
				if (elementName == null)
					elementName = "p";
				className = result.elementClassName;
				conv = chapter.createElement(elementName);
				if (className != null) {
					conv.setClassName(className);
					conv.setDesiredCascadeResult(result.elementRule);
				}
				resetSpaceProcessing = true;
				NumberingLabel label = pp.getNumberingLabel();
				if (label != null) {
					if (!styleConverter.convertLabelToProperty(label, null)) {
						// add label as a first child
						StylingResult labelRes = styleConverter.getLabelRule(result.elementRule, label, emScale
								* emScaleMultiplier(conv));
						Element labelElement = chapter.createElement("span");
						labelElement.setClassName(labelRes.elementClassName);
						labelElement.setDesiredCascadeResult(labelRes.elementRule);
						labelElement.add(label.getText() + " ");
						conv.add(labelElement);
					}
				}
			}
		} else {
			flushMagic();
			if (we instanceof RunElement) {
				RunElement wr = (RunElement) we;
				RunProperties rp = wr.getRunProperties();
				String epubStyle = getMagicStyleName(rp);
				String className = null;
				String elementName;
				if (epubStyle != null) {
					if (epubStyle.startsWith("*command")) {
						return processEPUBCommand(we.getTextContent(), depth);
					}
					if (epubStyle.startsWith("-")) {
						int index = epubStyle.indexOf('.');
						if (index < 0) {
							elementName = epubStyle.substring(1);
							epubStyle = "";
						} else {
							elementName = epubStyle.substring(1, index);
							epubStyle = epubStyle.substring(index);
						}
					} else
						elementName = "span";
					if (epubStyle.startsWith("."))
						className = epubStyle.substring(1);
					if (elementName == null)
						elementName = "span";
					if (elementName != null) {
						conv = chapter.createElement(elementName);
						if (className != null)
							conv.setClassName(className);
					}
				} else {
					StylingResult result = styleConverter.styleElement(rp, false, emScale, false, false);
					elementName = result.elementName;
					className = result.elementClassName;
					if (elementName == null && className != null)
						elementName = "span";
					if (elementName != null) {
						conv = chapter.createElement(elementName);
						if (className != null)
							conv.setClassName(className);
					}
					conv.setDesiredCascadeResult(result.elementRule);
				}
			} else if (we instanceof com.adobe.dp.office.word.HyperlinkElement) {
				com.adobe.dp.office.word.HyperlinkElement wa = (com.adobe.dp.office.word.HyperlinkElement) we;
				HyperlinkElement a = chapter.createHyperlinkElement("a");
				String href = wa.getHRef();
				if (href != null)
					a.setExternalHRef(href);
				conv = a;
			} else if (we instanceof FootnoteReferenceElement) {
				FootnoteReferenceElement wf = (FootnoteReferenceElement) we;
				String fid = wf.getID();
				if (fid != null) {
					XRef xref = (XRef) footnoteMap.get(fid);
					if (xref != null) {
						HyperlinkElement a = chapter.createHyperlinkElement("a");
						a.setClassName("footnote-ref");
						a.setXRef(xref);
						a.add("[" + fid + "]");
						conv = a;
					}
				}
				resetSpaceProcessing = true;
			} else if (we instanceof FootnoteElement) {
				FootnoteElement wf = (FootnoteElement) we;
				String fid = wf.getID();
				if (fid != null) {
					conv = chapter.createElement("div");
					footnoteMap.put(fid, conv.getSelfRef());
					conv.setClassName("footnote");
					Element ft = chapter.createElement("h6");
					ft.setClassName("footnote-title");
					conv.add(ft);
					ft.add(fid);
				}
				resetSpaceProcessing = true;
			} else if (we instanceof LastRenderedPageBreakElement) {
				if (useWordPageBreaks) {
					XRef xref = createOPSContainerXRefIfPossible();
					if (xref != null) {
						conv = chapter.createElement("span");
						xref = conv.getSelfRef();
					}
					epub.getTOC().addPage(null, xref);
				}
			} else if (we instanceof TableElement) {
				TableElement wt = (TableElement) we;
				TableProperties tp = wt.getTableProperties();
				conv = chapter.createElement("table");
				StylingResult result = styleConverter.convertTableStylingRule(tp, emScale);
				conv.setDesiredCascadeResult(result.elementRule);
				conv.setClassName("table");
				tableCellProps = result.tableCellRule;
				resetSpaceProcessing = true;
			} else if (we instanceof TableRowElement) {
				conv = chapter.createElement("tr");
				resetSpaceProcessing = true;
			} else if (we instanceof TableCellElement) {
				TableCellElement wtc = (TableCellElement) we;
				TableCellProperties tcp = wtc.getTableCellProperties();
				StylingResult result = styleConverter.convertTableCellStylingRule(tcp, emScale, tableCellProps);
				com.adobe.dp.epub.ops.TableCellElement tce = chapter.createTableCellElement("td", null, result.cols != null ? result.cols.intValue() : 1, 1);
				conv = tce;
				conv.setDesiredCascadeResult(result.elementRule);
				conv.setClassName("tc");
				resetSpaceProcessing = true;
			} else if (we instanceof TextElement) {
				Element parent = getCurrentOPSContainer();
				parent.add(processSpaces(((TextElement) we).getText()));
				return true;
			} else if (we instanceof TabElement) {
				Element parent = getCurrentOPSContainer();
				parent.add(processSpaces("\t"));
				return true;
			} else if (we instanceof BRElement) {
				conv = chapter.createElement("br");
				resetSpaceProcessing = true;
			} else if (we instanceof DrawingElement) {
				DrawingElement wd = (DrawingElement) we;
				PictureData picture = wd.getPictureData();
				if (picture != null) {
					try {
						Resource imageResource = getImageResource(picture, null, null);
						if (imageResource != null) {
							ImageElement img = chapter.createImageElement("img");
							img.setImageResource(imageResource);
							conv = img;
							if (picture.getWidth() > 0 && picture.getHeight() > 0) {
								double widthPt = picture.getWidth();
								setImageWidth(img, "img", widthPt, emScale * emScaleMultiplier(conv));
							}
						}
					} catch (Exception e) {
						// e.printStackTrace();
						e.printStackTrace(log);
					}
				}
				resetSpaceProcessing = true;
			} else if (we instanceof PictElement) {
				Iterator it = we.content();
				VMLGroupElement group = null;
				while (it.hasNext()) {
					Object child = it.next();
					if (child instanceof VMLGroupElement) {
						group = (VMLGroupElement) child;
						break;
					}
				}
				if (group != null) {
					Hashtable style = group.getStyle();
					if (style != null) {
						String widthStr = (String) style.get("width");
						if (widthStr != null) {
							float widthPt = VMLPathConverter.readCSSLength(widthStr, 0);
							boolean embed = false;
							VMLConverter vmlConv = new VMLConverter(this, embed);
							Element parent = getCurrentOPSContainer();
							if (embed) {
								SVGElement svg = chapter.createSVGElement("svg");
								vmlConv.convertVML(resource, svg, group);
								parent.add(svg);
								setImageWidth(svg, "svg", widthPt, emScale);
							} else {
								String name = epub.makeUniqueResourceName(mediaFolder + "vml-embed.svg");
								OPSResource svgResource = epub.createOPSResource(name, "image/svg+xml");
								OPSDocument svgDoc = svgResource.getDocument();
								SVGElement svg = (SVGElement) svgDoc.getBody();
								vmlConv.convertVML(svgResource, svg, group);
								ImageElement img = chapter.createImageElement("img");
								parent.add(img);
								img.setImageResource(svgResource);
								setImageWidth(img, "img", widthPt, emScale);
							}
						}
					}
				}
				return true;
			} else if (we instanceof TXBXContentElement) {
				conv = chapter.createElement("body");
				conv.setClassName("embed");
				resetSpaceProcessing = true;
			} else if (we instanceof SmartTagElement) {
				// pure container
			} else {
				// unknown element
				return true;
			}
		}
		if (conv != null) {
			if (nextPageName != null) {
				epub.getTOC().addPage(nextPageName, conv.getSelfRef());
				nextPageName = null;
			}
			if (addToParent) {
				Element parent = getCurrentOPSContainer();
				parent.add(conv);
			}
			emScale *= emScaleMultiplier(conv);
		}
		if (resetSpaceProcessing)
			resetSpaceProcessing();
		int cdepth = 0;
		if (conv != null)
			cdepth = pushOPSContainer(conv);
		addChildren(we, null, emScale, depth + 1, tableCellProps);
		if (conv != null)
			restoreOPSContainer(cdepth);
		if (ensureContent && conv != null && !conv.content().hasNext())
			conv.add("\u00A0"); // nbsp
		if (resetSpaceProcessing)
			resetSpaceProcessing();
		return true;
	}

	private float emScaleMultiplier(Element e) {
		Object fontSize = e.getCascadedProperty("font-size");
		if (fontSize != null && fontSize instanceof CSSLength) {
			CSSLength fs = (CSSLength) fontSize;
			if (fs.getUnit().equals("em")) {
				double scale = fs.getValue();
				if (scale > 0)
					return (float) scale;
			}
		}
		return 1;
	}

	private com.adobe.dp.office.word.Element addChildren(com.adobe.dp.office.word.Element we,
			com.adobe.dp.office.word.Element skipToChild, float emScale, int depth, InlineRule tableCellProps) {
		Iterator children = we.content();
		if (skipToChild != null) {
			while (children.hasNext()) {
				if (children.next() == skipToChild)
					break;
			}
		}
		com.adobe.dp.office.word.Element prev = null;
		com.adobe.dp.office.word.Element curr = children.hasNext() ? (com.adobe.dp.office.word.Element) children.next()
				: null;
		com.adobe.dp.office.word.Element next = null;
		while (curr != null) {
			if (children.hasNext())
				next = (com.adobe.dp.office.word.Element) children.next();
			else
				next = null;
			if (!appendConvertedElement(curr, prev, next, emScale, depth, tableCellProps)) {
				flushMagic();
				return curr;
			}
			prev = curr;
			curr = next;
		}
		flushMagic();
		return null;
	}

	void findLists(ContainerElement ce) {
		Iterator it = ce.content();
		while (it.hasNext()) {
			Object n = it.next();
			if (n instanceof ParagraphElement) {
				ParagraphElement pe = (ParagraphElement) n;
				ParagraphProperties pp = pe.getParagraphProperties();
				if (pp != null) {
					Style style = pp.getParagraphStyle();
					if (style != null && !style.getStyleId().startsWith("Heading")
							&& !style.getStyleId().startsWith("heading")) {
						if (pp.getNumberingLabel() != null)
							listElements.add(pe);
					}
				}
			} else if (n instanceof ContainerElement) {
				findLists((ContainerElement) n);
			}
		}
	}

	void convert(BodyElement wbody, OPSResource ops, boolean addToSpine) {
		StyleResource global = (StyleResource) epub.getResourceByName("OPS/global.css");
		resource = ops;
		com.adobe.dp.office.word.Element child = null;
		do {
			chapter = resource.getDocument();
			chapter.addStyleResource(global);
			if (addToSpine)
				epub.addToSpine(resource);
			Element body = chapter.getBody();
			body.setClassName("primary");
			int depth = pushOPSContainer(body);
			child = addChildren(wbody, child, 1, 1, null);
			restoreOPSContainer(depth);
		} while (child != null);
	}

	void setWordResources(ContainerSource source) {
		wordResources = source;
	}

}
