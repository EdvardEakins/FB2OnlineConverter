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

package com.adobe.dp.fb2.convert;

import com.adobe.dp.css.*;
import com.adobe.dp.epub.conv.Version;
import com.adobe.dp.epub.io.BufferedDataSource;
import com.adobe.dp.epub.ncx.TOCEntry;
import com.adobe.dp.epub.opf.*;
import com.adobe.dp.epub.ops.*;
import com.adobe.dp.epub.style.Stylesheet;
import com.adobe.dp.epub.util.ImageDimensions;
import com.adobe.dp.fb2.*;
import com.adobe.dp.otf.DefaultFontLocator;
import com.adobe.dp.otf.FontLocator;
import com.adobe.dp.otf.FontProperties;
import com.adobe.dp.xml.util.StringUtil;

import java.io.*;
import java.util.*;

public class FB2Converter {

	final static private int RESOURCE_THRESHOLD_MAX = 45000;

	final static private int RESOURCE_THRESHOLD_MIN = 10000;

	private static CSSQuotedString defaultSansSerifFont = new CSSQuotedString("Arial");

	private static CSSQuotedString defaultSerifFont = new CSSQuotedString("Times New Roman");

	private static CSSQuotedString defaultMonospaceFont = new CSSQuotedString("Courier New");

	FB2Document doc;

	Publication epub;

	Stylesheet stylesheet;

	StyleResource styles;

	NCXResource toc;

	int nameCount = 0;

	Hashtable idMap = new Hashtable();

	FB2Document templateDoc;

	CSSStylesheet templateRules;

	FontLocator fontLocator;

	FontLocator defaultFontLocator;

	static CSSStylesheet defaultStylesheet;

	PrintWriter log = new PrintWriter(new OutputStreamWriter(System.out));

	// static FontLocator builtInFontLocator = new BuiltInFontLocator();

	static float[] titleFontSizes = { 2.2f, 1.8f, 1.5f, 1.3f, 1.2f, 1.1f, 1.0f };

	static {
		try {
			InputStream in = FB2Converter.class.getResourceAsStream("stylesheet.css");
			CSSParser parser = new CSSParser();
			defaultStylesheet = parser.readStylesheet(in);
			in.close();
			Iterator errs = parser.errors();
			if (errs != null) {
				while (errs.hasNext()) {
					CSSParsingError err = (CSSParsingError) errs.next();
					System.err.println(err.getLine() + ": " + err.getError());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static class LinkRecord {
		Vector sources = new Vector();

		Element target;
	}

	private LinkRecord getLinkRecord(String id) {
		LinkRecord record = (LinkRecord) idMap.get(id);
		if (record == null) {
			record = new LinkRecord();
			idMap.put(id, record);
		}
		return record;
	}

	private OPSResource newCoverPageResource() {
		OPSResource res = epub.createOPSResource("OPS/cover.xhtml");
		res.getDocument().addStyleResource(styles);
		epub.addToSpine(res);
		return res;
	}

	private OPSResource newOPSResource() {
		String name = "OPS/ch" + (++nameCount) + ".xhtml";
		OPSResource res = epub.createOPSResource(name);
		res.getDocument().addStyleResource(styles);
		epub.addToSpine(res);
		return res;
	}

	private BitmapImageResource getBitmapImageResource(String name) {
		FB2Binary bin = doc.getBinaryResource(name);
		if (bin == null)
			return null;
		String path = "OPS/images/" + name;
		BitmapImageResource resource = (BitmapImageResource) epub.getResourceByName(path);
		if (resource == null) {
			BufferedDataSource data = new BufferedDataSource();
			try {
				data.getOutputStream().write(bin.getData());
			} catch (IOException e) {
				throw new Error("unexpected exception: " + e);
			}
			resource = epub.createBitmapImageResource(path, bin.getMediaType(), data);
		}
		return resource;
	}

	private String trim(String s) {
		StringBuffer sb = new StringBuffer();
		int len = s.length();
		int i = 0;
		while (i < len) {
			char c = s.charAt(i);
			if ((c & 0xFFFF) > ' ')
				break;
			i++;
		}
		boolean hadSpace = false;
		while (i < len) {
			char c = s.charAt(i);
			if ((c & 0xFFFF) > ' ') {
				if (hadSpace) {
					sb.append(' ');
					hadSpace = false;
				}
				sb.append(c);
			} else
				hadSpace = true;
			i++;
		}
		return sb.toString();
	}

	private String getStringValue(Object val) {
		if (val instanceof CSSQuotedString) {
			return ((CSSQuotedString) val).getText();
		}
		return val.toString();
	}

	private boolean isBuiltIn(String name) {
		return name.equals("serif") || name.equals("sans-serif") || name.equals("monospace");
	}

	private void adjustFontList(BaseRule rule) {
		Object fonts = rule.get("font-family");
		if (fonts == null)
			return;
		int count = CSSValueList.valueCount(fonts, ',');
		for (int i = 0; i < count; i++) {
			String family = getStringValue(CSSValueList.valueAt(fonts, i, ','));
			if (isBuiltIn(family))
				continue;
			FontProperties fp = new FontProperties(family, FontProperties.WEIGHT_NORMAL, FontProperties.STYLE_REGULAR);
			if (fontLocator.hasFont(fp))
				return; // found at least one font
		}
		Vector list = new Vector();
		boolean inserted = false;
		for (int i = 0; i < count; i++) {
			Object fn = CSSValueList.valueAt(fonts, i, ',');
			String family = getStringValue(fn);
			if (!inserted) {
				if (family.equals("sans-serif")) {
					list.add(defaultSansSerifFont);
					inserted = true;
				} else if (family.equals("serif")) {
					list.add(defaultSerifFont);
					inserted = true;
				} else if (family.equals("monospace")) {
					list.add(defaultMonospaceFont);
					inserted = true;
				}
			}
			list.add(fn);
		}
		if (!inserted)
			list.add(defaultSerifFont);
		CSSValue[] vals = new CSSValue[list.size()];
		list.copyInto(vals);
		rule.set("font-family", new CSSValueList(',', vals));
	}

	void mergeRuleStyle(BaseRule rule, CSSStylesheet src, Selector ss) {
		SelectorRule docRule = src.getRuleForSelector(ss, false);
		if (docRule != null) {
			Iterator props = docRule.properties();
			while (props.hasNext()) {
				String prop = (String) props.next();
				rule.set(prop, docRule.get(prop));
			}
		}
	}

	private Element convertElement(OPSDocument ops, Element parent, Object fb, TOCEntry entry, int level,
			boolean insideTitle, boolean largeResource) {
		if (fb instanceof FB2Element) {
			FB2Element fbe = (FB2Element) fb;
			FB2Title title = null;
			if (fbe instanceof FB2Section) {
				title = ((FB2Section) fbe).getTitle();
			}
			String className = fbe.getName();
			String name = null;
			CascadeResult cascade = fbe.getCascade();
			InlineRule estyle = null;
			if (cascade != null) {
				estyle = cascade.getProperties().getPropertySet();
				estyle = estyle.cloneObject();
				CSSName ename = (CSSName) estyle.get("-epubgen-name");
				if (ename != null)
					name = ename.toString();
				estyle.set("-epubgen-name", null);
				adjustFontList(estyle);
			}
			if (name == null)
				name = "span";
			if (fbe instanceof FB2Section) {
				level++;
			} else if (fbe instanceof FB2StyledText) {
				className = ((FB2StyledText) fbe).getStyleName();
			}
			Element self;
			if (name.equals("image")) {
				ImageElement img = null;
				FB2Image image = (FB2Image) fbe;
				String resourceName = image.getResourceName();
				String alt = image.getAlt();
				String caption = image.getTitle();
				if (resourceName != null) {
					BitmapImageResource resource = getBitmapImageResource(resourceName);
					if (resource != null) {
						img = ops.createImageElement("img");
						img.setImageResource(resource);
						if (alt != null)
							img.setAltText(alt);
						if (cascade != null) {
							InlineRule style = cascade.getProperties().getPropertySetForPseudoElement("content");
							style = style.cloneObject();
							adjustFontList(style);
							img.setDesiredCascadeResult(style);
						}
					}
				}
				if (img == null)
					return null;
				self = ops.createElement("div");
				self.add(img);
				if (caption != null && caption.length() > 0) {
					InlineRule style = null;
					CSSName tname = null;
					if (cascade != null) {
						style = cascade.getProperties().getPropertySetForPseudoElement("title");
						style = style.cloneObject();
						tname = (CSSName) style.get("-epubgen-name");
						style.set("-epubgen-name", null);
						adjustFontList(style);
					}
					HTMLElement captionElement = ops.createElement(tname == null ? "p" : tname.toString());
					captionElement.setClassName("image-title");
					self.add(captionElement);
					captionElement.add(caption);
					captionElement.setDesiredCascadeResult(style);
				}
			} else if (name.equals("a")) {
				HyperlinkElement a = ops.createHyperlinkElement("a");
				String link = ((FB2Hyperlink) fbe).getLinkedId();
				if (link != null) {
					LinkRecord record = getLinkRecord(link);
					record.sources.add(a);
				}
				self = a;
			} else if (name.equals("td") || name.equals("th")) {
				FB2TableCell fbt = (FB2TableCell) fbe;
				TableCellElement td = ops.createTableCellElement(name, fbt.getAlign(), fbt.getColSpan(), fbt
						.getRowSpan());
				self = td;
			} else {
				self = ops.createElement(name);
			}
			if (largeResource && self instanceof HTMLElement) {
				((HTMLElement) self).setForceChapterBreak(true);
			}
			self.setClassName(className);
			self.setDesiredCascadeResult(estyle);
			parent.add(self);
			if (fbe.getId() != null) {
				LinkRecord record = getLinkRecord(fbe.getId());
				record.target = self;
			}
			if (title != null && entry != null) {
				TOCEntry subentry = toc.createTOCEntry(trim(title.contentAsString()), self.getSelfRef());
				entry.add(subentry);
				entry = subentry;
			}
			Object[] children = fbe.getChildren();
			int size = 0;
			for (int i = 0; i < children.length; i++) {
				Object child = children[i];
				boolean large = false;
				boolean over = false;
				int esize = 0;
				if (largeResource) {
					large = isLargeSection(child);
					if (large)
						size = 0;
					else {
						esize = FB2Element.getUTF16Size(child);
						size += esize;
						over = size > esize && size > RESOURCE_THRESHOLD_MAX;
					}
				}
				Element ce = convertElement(ops, self, child, entry, level, insideTitle, large);
				if (over && ce instanceof HTMLElement) {
					((HTMLElement) ce).setForceChapterBreak(true);
					size = esize;
				}
			}
			return self;
		} else {
			parent.add(fb);
			return null;
		}
	}

	public void setFontLocator(FontLocator fontLocator) {
		defaultFontLocator = fontLocator;
	}

	public void setTemplate(CSSStylesheet stylesheet) throws IOException {
		templateRules = stylesheet;
	}

	public void setTemplate(InputStream templateStream) throws IOException, FB2FormatException {
		BufferedInputStream in = new BufferedInputStream(templateStream);
		byte[] sniff = new byte[4];
		in.mark(4);
		in.read(sniff);
		in.reset();
		CSSParser parser = new CSSParser();
		if ((sniff[0] == 'P' && sniff[1] == 'K' && sniff[2] == 3 && sniff[3] == 4) || sniff[0] == '<'
				|| ((sniff[0] == (byte) 0xef && sniff[1] == (byte) 0xbb && sniff[2] == (byte) 0xbf && sniff[3] == '<'))) {
			// template is FB2 file itself
			templateDoc = new FB2Document(in);
			templateRules = null;
		} else {
			templateDoc = null;
			templateRules = new CSSStylesheet();
			parser.readStylesheet(in, templateRules);
		}
	}

	public void setTemplateFile(String file) {
		try {
			setTemplate(new FileInputStream(file));
		} catch (IOException e) {
			e.printStackTrace(log);
		} catch (FB2FormatException e) {
			e.printStackTrace(log);
		}
	}

	private boolean isLargeSection(Object child) {
		return child instanceof FB2Section && ((FB2Section) child).getUTF16Size() >= RESOURCE_THRESHOLD_MIN;
	}

	private void convertSection(OPSDocument ops, Element body, FB2Section section, TOCEntry entry, int level) {
		int size = section.getUTF16Size();
		FB2Title title = section.getTitle();
		String sectionName = section.getSectionName();
		if (sectionName != null) {
			if (sectionName.equals("footnotes") || sectionName.equals("notes")) {
				entry = null;
				level += 2;
			}
		}
		if (title != null && level > 1) {
			if (entry != null) {
				TOCEntry subentry = toc.createTOCEntry(trim(title.contentAsString()), body.getSelfRef());
				entry.add(subentry);
				entry = subentry;
			}
		}
		CascadeResult cr = section.getCascade();
		if (cr != null) {
			InlineRule style = cr.getProperties().getPropertySet().cloneObject();
			style.set("-epub-name", null);
			adjustFontList(style);
			body.setDesiredCascadeResult(style);
		}
		boolean large = size > RESOURCE_THRESHOLD_MAX;
		Object[] children = section.getChildren();
		for (int i = 0; i < children.length; i++)
			convertElement(ops, body, children[i], entry, level, false, large);
	}

	private boolean isUUID(String id) {
		return id.length() == 36 && id.charAt(8) == '-' && id.charAt(13) == '-' && id.charAt(18) == '-'
				&& id.charAt(23) == '-';
	}

	private void convert() {
		FB2TitleInfo bookInfo = doc.getTitleInfo();
		styles = epub.createStyleResource("OPS/style.css");
		stylesheet = styles.getStylesheet();
		boolean dateAdded = false;
		if (bookInfo != null) {
			String title = bookInfo.getBookTitle();
			epub.addDCMetadata("title", title);
			FB2AuthorInfo[] authors = bookInfo.getAuthors();
			if (authors != null) {
				for (int i = 0; i < authors.length; i++) {
					epub.addDCMetadata("creator", authors[i].toString());
				}
			}
			FB2AuthorInfo[] translators = bookInfo.getTranslators();
			if (translators != null) {
				for (int i = 0; i < translators.length; i++) {
					epub.addMetadata(null, "FB2.book-info.translator", translators[i].toString());
				}
			}
			epub.addDCMetadata("language", bookInfo.getLanguage());
			FB2Section annot = bookInfo.getAnnotation();
			if (annot != null) {
				epub.addDCMetadata("description", annot.contentAsString());
			}
			FB2DateInfo date = bookInfo.getDate();
			if (date != null) {
				String mr = date.getMachineReadable();
				epub.addMetadata(null, "FB2.book-info.date", (mr == null ? date.getHumanReadable() : mr));
				Date d = date.getDate();
				if (d != null) {
					epub.addDCMetadata("date", StringUtil.toShortW3CDTF(d, date.isYearOnly()));
					dateAdded = true;
				}
			}
			FB2GenreInfo[] genres = bookInfo.getGenres();
			if (genres != null) {
				for (int i = 0; i < genres.length; i++)
					epub.addMetadata(null, "FB2.book-info.genre", genres[i].toString());
			}
			FB2SequenceInfo[] sequences = bookInfo.getSequences();
			if (sequences != null) {
				for (int i = 0; i < sequences.length; i++)
					epub.addMetadata(null, "FB2.book-info.sequence", sequences[i].toString());
			}
			String coverpageImage = bookInfo.getCoverpageImage();
			if (coverpageImage != null) {
				FB2Binary binary = doc.getBinaryResource(coverpageImage);
				if (binary != null && binary.getData() != null) {
					int[] dim = ImageDimensions.getImageDimensions(binary.getData());
					if (dim != null) {
						OPSResource coverRes = newCoverPageResource();
						OPSDocument coverDoc = coverRes.getDocument();
						Element body = coverDoc.getBody();
						body.setClassName("cover");
						SelectorRule coverBodyRule = stylesheet.getRuleForSelector(stylesheet.getSimpleSelector("body",
								"cover"), true);
						coverBodyRule.set("oeb-column-number", new CSSNumber(1));
						coverBodyRule.set("margin", new CSSLength(0, "px"));
						coverBodyRule.set("padding", new CSSLength(0, "px"));
						SVGElement svg = coverDoc.createSVGElement("svg");
						svg.setAttribute("viewBox", "0 0 " + dim[0] + " " + dim[1]);
						svg.setClassName("cover-svg");
						body.add(svg);
						SelectorRule svgRule = stylesheet.getRuleForSelector(stylesheet.getSimpleSelector("svg",
								"cover-svg"), true);
						svgRule.set("width", new CSSLength(100, "%"));
						svgRule.set("height", new CSSLength(100, "%"));
						SVGImageElement image = coverDoc.createSVGImageElement("image");
						image.setAttribute("width", Integer.toString(dim[0]));
						image.setAttribute("height", Integer.toString(dim[1]));
						BitmapImageResource resource = getBitmapImageResource(coverpageImage);
						image.setImageResource(resource);
						svg.add(image);
                        epub.setCoverImage(resource);
					}
				}
			}
		}
		if (defaultFontLocator == null)
			defaultFontLocator = DefaultFontLocator.getInstance(DefaultFontLocator.BUILT_IN_DIRS);
		fontLocator = defaultFontLocator;
		CascadeEngine cascadeEngine = new CascadeEngine();
		cascadeEngine.add(defaultStylesheet, null);
		if (templateRules != null) {
			cascadeEngine.add(templateRules, null);
			fontLocator = new EmbeddedFontLocator(templateRules, fontLocator);
		}
		if (templateDoc != null) {
			CSSStylesheet[] stylesheets = templateDoc.getStylesheets();
			if (stylesheets != null) {
				for (int i = 0; i < stylesheets.length; i++) {
					CSSStylesheet stylesheet = stylesheets[i];
					cascadeEngine.add(stylesheet, null);
					fontLocator = new EmbeddedFontLocator(stylesheet, fontLocator);
				}
			}
		}
		CSSStylesheet[] stylesheets = doc.getStylesheets();
		if (stylesheets != null) {
			for (int i = 0; i < stylesheets.length; i++) {
				CSSStylesheet stylesheet = stylesheets[i];
				cascadeEngine.add(stylesheet, null);
				fontLocator = new EmbeddedFontLocator(stylesheet, fontLocator);
			}
		}
		long start = System.currentTimeMillis();
		doc.applyStyles(cascadeEngine);
		long end = System.currentTimeMillis();
		System.out.println( "Applied styles in " + (end - start) + "ms");
		FB2DocumentInfo docInfo = doc.getDocumentInfo();
		String ident = null;
		if (docInfo != null) {
			FB2AuthorInfo[] authors = docInfo.getAuthors();
			if (authors != null) {
				for (int i = 0; i < authors.length; i++) {
					epub.addMetadata(null, "FB2.document-info.author", authors[i].toString());
				}
			}
			epub.addMetadata(null, "FB2.document-info.program-used", docInfo.getProgramUsed());
			String[] urls = docInfo.getSrcUrls();
			if (urls != null) {
				for (int i = 0; i < urls.length; i++)
					epub.addMetadata(null, "FB2.document-info.src-url", urls[i]);
			}
			epub.addMetadata(null, "FB2.document-info.src-ocr", docInfo.getSrcOcr());
			FB2Section history = docInfo.getHistory();
			if (history != null)
				epub.addMetadata(null, "FB2.document-info.history", history.contentAsString());
			ident = docInfo.getId();
			epub.addMetadata(null, "FB2.document-info.id", ident);
			epub.addMetadata(null, "FB2.document-info.version", docInfo.getVersion());
			FB2DateInfo date = bookInfo.getDate();
			if (date != null) {
				String mr = date.getMachineReadable();
				epub.addMetadata(null, "FB2.document-info.date", (mr == null ? date.getHumanReadable() : mr));
			}

		}
		if (ident == null || !isUUID(ident)) {
			epub.generateRandomIdentifier();
			if (ident != null)
				epub.addDCMetadata("identifier", ident);
		} else {
			epub.addDCMetadata("identifier", "urn:uuid:" + ident.toLowerCase());
		}
		FB2PublishInfo publishInfo = doc.getPublishInfo();
		if (publishInfo != null) {
			epub.addDCMetadata("publisher", publishInfo.getPublisher());
			epub.addMetadata(null, "FB2.publish-info.book-name", publishInfo.getBookName());
			epub.addMetadata(null, "FB2.publish-info.city", publishInfo.getCity());
			epub.addMetadata(null, "FB2.publish-info.year", publishInfo.getYear());
			if (!dateAdded) {
				epub.addDCMetadata("date", publishInfo.getYear());
				dateAdded = true;
			}
			String isbn = publishInfo.getISBN();
			if (isbn != null)
				epub.addDCMetadata("identifier", "isbn:" + isbn.toUpperCase());
		}
		FB2Section[] bodySections = doc.getBodySections();
		toc = epub.getTOC();
		TOCEntry entry = toc.getRootTOCEntry();
		for (int i = 0; i < bodySections.length; i++) {
			OPSResource resource = newOPSResource();
			OPSDocument ops = resource.getDocument();
			Element body = ops.getBody();
			convertSection(ops, body, bodySections[i], entry, 1);
		}
		Enumeration keys = idMap.keys();
		while (keys.hasMoreElements()) {
			String id = (String) keys.nextElement();
			LinkRecord record = (LinkRecord) idMap.get(id);
			if (record.target != null) {
				XRef ref = record.target.getSelfRef();
				Enumeration sources = record.sources.elements();
				while (sources.hasMoreElements()) {
					HyperlinkElement a = (HyperlinkElement) sources.nextElement();
					a.setXRef(ref);
				}
			}
		}

		epub.addMetadata(null, "FB2EPUB.version", Version.VERSION);
		epub.addMetadata(null, "FB2EPUB.conversionDate", StringUtil.dateToW3CDTF(new Date()));
		epub.generateStyles(styles);
		// pass some large number, should split along where marked
		epub.splitLargeChapters(2000000);
	}

	public void embedFonts() {
		epub.addFonts(styles, fontLocator);
	}

	public void convert(FB2Document doc, Publication epub) {
		this.doc = doc;
		this.epub = epub;
		convert();
	}

	public void setLog(PrintWriter log) {
		this.log = log;
	}
}
