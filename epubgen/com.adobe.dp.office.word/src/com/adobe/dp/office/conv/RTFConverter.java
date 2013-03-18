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

import com.adobe.dp.css.*;
import com.adobe.dp.epub.io.BufferedDataSource;
import com.adobe.dp.epub.io.DataSource;
import com.adobe.dp.epub.io.StringDataSource;
import com.adobe.dp.epub.opf.OPSResource;
import com.adobe.dp.epub.opf.Publication;
import com.adobe.dp.epub.opf.Resource;
import com.adobe.dp.epub.opf.StyleResource;
import com.adobe.dp.epub.ops.Element;
import com.adobe.dp.epub.ops.HTMLElement;
import com.adobe.dp.epub.ops.ImageElement;
import com.adobe.dp.epub.ops.OPSDocument;
import com.adobe.dp.epub.style.Stylesheet;
import com.adobe.dp.office.metafile.WMFParser;
import com.adobe.dp.office.rtf.*;
import com.adobe.dp.otf.FontLocator;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

public class RTFConverter {

	Stylesheet stylesheet;

	RTFDocument doc;

	Publication epub;

	OPSDocument chapter;

	Element section;

	Element paragraph;

	Element textAppendPoint;

	HashSet usedClassNames = new HashSet();

	Hashtable styleMap = new Hashtable();

	String bulletText;

	String bulletClass;

	StyleResource css;

	PrintWriter log = new PrintWriter(new OutputStreamWriter(System.out));

	int count = 1;

	static String mediaFolder = "OPS/images/";

	class WMFResourceWriter implements ResourceWriter {

		public StreamAndName createResource(String base, String suffix, boolean doNotCompress) throws IOException {
			String name = mediaFolder + "wmf-" + base + suffix;
			name = epub.makeUniqueResourceName(name);
			BufferedDataSource dataSource = new BufferedDataSource();
			epub.createBitmapImageResource(name, "image/png", dataSource);
			return new StreamAndName(name.substring(mediaFolder.length()), dataSource.getOutputStream());
		}

	}

	public RTFConverter(RTFDocument doc, Publication epub) {
		this.doc = doc;
		this.epub = epub;
	}

	private void transferProperties(SelectorRule rule, RTFStyle style, boolean block) {
		Iterator props = style.properties();
		boolean adjFontSize = false;
		float defaultFontSize = 22;
		float fontSize = defaultFontSize;
		boolean underline = false;
		boolean strike = false;
		int before = 0;
		int after = 0;
		int left = 0;
		int right = 0;
		int lineSpacing = 0;
		while (props.hasNext()) {
			String prop = (String) props.next();
			Object val = style.get(prop);
			if (prop.equals("q_")) {
				if (val.equals("l"))
					rule.set("text-align", new CSSName("left"));
				else if (val.equals("r"))
					rule.set("text-align", new CSSName("right"));
				else if (val.equals("c"))
					rule.set("text-align", new CSSName("center"));
				else if (val.equals("j"))
					rule.set("text-align", new CSSName("justify"));
			} else if (prop.equals("f")) {
				RTFFont font = doc.getFont(((Number) val).intValue());
				if (font != null)
					rule.set("font-family", font.toCSSValue());
			} else if (prop.equals("fs")) {
				fontSize = ((Number) val).intValue();
			} else if (prop.equals("sb")) {
				before = ((Number) val).intValue();
			} else if (prop.equals("sa")) {
				after = ((Number) val).intValue();
			} else if (prop.equals("li")) {
				left = ((Number) val).intValue();
			} else if (prop.equals("ri")) {
				right = ((Number) val).intValue();
			} else if (prop.equals("sl")) {
				lineSpacing = ((Number) val).intValue();
			} else if (prop.equals("fi")) {
				rule.set("text-indent", new CSSLength(((Number) val).intValue() / 220.0, "em"));
			} else if (prop.equals("i")) {
				rule.set("font-style", new CSSName(val.equals(Boolean.TRUE) ? "italic" : "normal"));
			} else if (prop.equals("b")) {
				rule.set("font-weight", new CSSName(val.equals(Boolean.TRUE) ? "bold" : "normal"));
			} else if (prop.equals("cf")) {
				RTFColor color = doc.getColor(((Integer) val).intValue());
				if (color != null)
					rule.set("color", color.toCSSValue());
			} else if (prop.equals("sub")) {
				if (val.equals(Boolean.TRUE)) {
					rule.set("vertical-align", new CSSName("sub"));
					adjFontSize = true;
				}
			} else if (prop.equals("super")) {
				if (val.equals(Boolean.TRUE)) {
					rule.set("vertical-align", new CSSName("super"));
					adjFontSize = true;
				}
			} else if (prop.equals("ul")) {
				if (!val.equals(Boolean.FALSE)) {
					underline = true;
				}
			} else if (prop.equals("strike")) {
				if (!val.equals(Boolean.FALSE)) {
					strike = true;
				}
			} else if (prop.equals("cf")) {
				RTFColor color = doc.getColor(((Integer) val).intValue());
				if (color != null)
					rule.set("color", color.toCSSValue());
			} else if (prop.equals("webhidden")) {
				rule.set("visibility", new CSSName(val.equals(Boolean.TRUE) ? "hidden" : "visible"));
			}
		}
		if (adjFontSize)
			fontSize *= 0.67f;
		if (fontSize != defaultFontSize)
			rule.set("font-size", new CSSLength((fontSize / defaultFontSize), "em"));
		if (underline || strike) {
			if (underline && strike) {
				CSSValue[] td = { new CSSName("line-through"), new CSSName("underline") };
				rule.set("text-decoration", new CSSValueList(',', td));
			} else if (underline)
				rule.set("text-decoration", new CSSName("underline"));
			else
				rule.set("text-decoration", new CSSName("line-through"));
		}
		if (block) {
			CSSValue[] margin = { new CSSLength(before / 20.0, "pt"), new CSSLength(right / 20.0, "pt"),
					new CSSLength(after / 20.0, "pt"), new CSSLength(left / 20.0, "pt") };
			rule.set("margin", new CSSValueList(' ', margin));
			if (lineSpacing != 0) {
				if (lineSpacing < 0)
					lineSpacing = -lineSpacing;
				rule.set("line-height", new CSSLength(lineSpacing / (defaultFontSize * 10), "em"));
			}
		}
	}

	private String getClassName(RTFStyle[] styles, String prefix, Set propSet) {
		RTFStyle style = RTFStyle.collapse(styles, propSet);
		if (style.isEmpty())
			return prefix + "0";
		String className = (String) styleMap.get(style);
		if (className == null) {
			className = prefix + (count++);
			Selector selector = stylesheet.getSimpleSelector(null, className);
			SelectorRule rule = stylesheet.getRuleForSelector(selector, true);
			transferProperties(rule, style, prefix.equals("p"));
			style.lock();
			styleMap.put(style, className);
		}
		return className;
	}

	private String getParagraphClassName(RTFStyle paragraphStyle, RTFStyle characterStyle) {
		RTFStyle[] styles = { characterStyle, paragraphStyle };
		return getClassName(styles, "p", RTFControlType.paragraphProps);
	}

	private String getCharacterClassName(RTFStyle paragraphStyle, RTFStyle characterStyle) {
		RTFStyle[] styles = { characterStyle, paragraphStyle };
		return getClassName(styles, "c", RTFControlType.characterProps);
	}

	private void ensureParagraph(RTFStyle paragraphStyle, RTFStyle characterStyle) {
		if (paragraph == null) {
			paragraph = chapter.createElement("p");
			paragraph.setClassName(getParagraphClassName(paragraphStyle, characterStyle));
			section.add(paragraph);
			textAppendPoint = null;
		}
	}

	private Element getTextAppendPoint(RTFStyle paragraphStyle, RTFStyle characterStyle) {
		ensureParagraph(paragraphStyle, characterStyle);
		if (textAppendPoint == null) {
			if (characterStyle != null && !characterStyle.isEmpty()) {
				textAppendPoint = chapter.createElement("span");
				textAppendPoint.setClassName(getCharacterClassName(paragraphStyle, characterStyle));
				paragraph.add(textAppendPoint);
			} else {
				textAppendPoint = paragraph;
			}
		}
		return textAppendPoint;
	}

	private void addChildren(RTFGroup g, RTFStyle paragraphStyle, RTFStyle characterStyle) {
		RTFControl ctrl;
		Object[] content = g.getContent();
		for (int i = 0; i < content.length; i++) {
			Object c = content[i];
			if (c instanceof RTFControl) {
				ctrl = (RTFControl) c;
				RTFControlType ct = ctrl.getType();
				String name = ctrl.getName();
				if (name.equals("par")) {
					// new paragraph
					paragraph = null;
					textAppendPoint = null;
				} else if (name.equals("s")) {
					int index = ctrl.getParam();
					paragraphStyle = doc.getParagraphStyle(index);
					textAppendPoint = null;
				} else if (name.equals("cs")) {
					int index = ctrl.getParam();
					characterStyle = cloneStyle(doc.getCharacterStyle(index));
					textAppendPoint = null;
				} else {
					if (ct instanceof RTFFormattingControlType) {
						if (name.equals("pard"))
							paragraphStyle = null;
						if (characterStyle == null)
							characterStyle = new RTFStyle();
						if (characterStyle.isLocked()) {
							characterStyle = characterStyle.cloneStyle();
						}
						ct.formattingExec(ctrl, characterStyle);
						textAppendPoint = null;
					}
				}
			} else if (c instanceof String) {
				getTextAppendPoint(paragraphStyle, characterStyle);
				if (bulletText != null) {
					HTMLElement bulletSpan = chapter.createElement("span");
					bulletSpan.setClassName(bulletClass);
					textAppendPoint.add(bulletSpan);
					bulletSpan.add(bulletText);
					bulletText = null;
				}
				textAppendPoint.add(c);
			} else if (c instanceof RTFGroup) {
				RTFGroup sg = (RTFGroup) c;
				ctrl = sg.getHead();
				if (ctrl != null) {
					String name = ctrl.getName();
					if (name.equals("pict")) {
						processPicture(sg);
						continue;
					}
					if (name.equals("listtext")) {
						extractBulletText(sg);
						continue;
					}
					// System.out.println(name + " " + ctrl.isOptional());
					if (ctrl.isOptional())
						continue;
				}
				textAppendPoint = null;
				addChildren(sg, cloneStyle(paragraphStyle), cloneStyle(characterStyle));
				textAppendPoint = null;
			}
		}
	}

	private void extractBulletText(RTFGroup listtext) {
		StringBuffer sb = new StringBuffer();
		extractText(listtext, sb);
		bulletText = sb.toString();
		bulletClass = "listtext";
		if (bulletText.equals("\u00B7\t")) {
			bulletText = "\u2022\t";
			bulletClass = "bullet";
		} else if (bulletText.equals("o\t")) {
			bulletText = "\u25E6\t";
			bulletClass = "bullet";
		}
	}

	private void extractText(RTFGroup listtext, StringBuffer sb) {
		Object[] content = listtext.getContent();
		for (int i = 0; i < content.length; i++) {
			Object c = content[i];
			if (c instanceof String)
				sb.append(c);
			else if (c instanceof RTFGroup) {
				RTFGroup g = (RTFGroup) c;
				RTFControl ctrl = g.getHead();
				if (ctrl == null || !ctrl.isOptional())
					extractText(g, sb);
			}
		}
	}

	private void processPicture(RTFGroup pict) {
		paragraph = null;
		textAppendPoint = null;
		Object[] content = pict.getContent();
		if (content.length == 0)
			return;
		String contentType = null;
		int width = 0;
		int scalex = 100;
		for (int i = 0; i < content.length; i++) {
			if (content[i] instanceof RTFControl) {
				RTFControl c = (RTFControl) content[i];
				String name = c.getName();
				if (name.equals("emfblip"))
					contentType = "image/x-emf";
				else if (name.equals("pngblip"))
					contentType = "image/png";
				else if (name.equals("jpegblip"))
					contentType = "image/jpeg";
				else if (name.equals("wmetafile"))
					contentType = "image/x-wmf";
				else if (name.equals("picwgoal"))
					width = c.getParam();
				else if (name.equals("picscalex"))
					scalex = c.getParam();
			}
		}
		if (contentType == null)
			return;
		Object last = content[content.length - 1];
		if (!(last instanceof byte[]))
			return;
		byte[] pictBytes = (byte[]) last;
		Resource res;
		if (contentType.equals("image/jpeg") || contentType.equals("image/png")) {
			BufferedDataSource dataSource = new BufferedDataSource();
			try {
				dataSource.getOutputStream().write(pictBytes);
			} catch (IOException e) {
				throw new Error("Unexpected IOException for memory buffer stream: " + e.getMessage());
			}
			String resName = epub.makeUniqueResourceName(mediaFolder
					+ (contentType.equals("image/jpeg") ? "pict.jpeg" : "pict.png"));
			res = epub.createBitmapImageResource(resName, contentType, dataSource);
		} else if (contentType.equals("image/x-wmf")) {
			WMFResourceWriter dw = new WMFResourceWriter();
			GDISVGSurface svg = new GDISVGSurface(dw);
			try {
				WMFParser parser = new WMFParser(new ByteArrayInputStream(pictBytes), svg);
				parser.readAll();
			} catch (IOException e) {
				e.printStackTrace(log);
				return;
			}
			DataSource dataSource = new StringDataSource(svg.getSVG());
			String resName = epub.makeUniqueResourceName(mediaFolder + "pict.svg");
			res = epub.createGenericResource(resName, "image/svg+xml", dataSource);
		} else
			return;
		ImageElement imageElement = chapter.createImageElement("img");
		imageElement.setImageResource(res);
		InlineRule props = new InlineRule();
		if (width > 0)
			props.set("width", new CSSLength(width * scalex / 2000.0, "pt"));
		props.set("max-width", new CSSLength(95, "%"));
		imageElement.setStyle(props);
		section.add(imageElement);
	}

	private static RTFStyle cloneStyle(RTFStyle s) {
		if (s == null)
			return null;
		return s.cloneStyle();
	}

	public void convert() {
		OPSResource ops = epub.createOPSResource("OPS/content.xhtml");
		css = epub.createStyleResource("OPS/style.css");
		stylesheet = css.getStylesheet();
		SelectorRule bulletRule = stylesheet.getRuleForSelector(stylesheet.getSimpleSelector("span", "bullet"), true);
		CSSValue[] names = { new CSSName("sans-serif") };
		bulletRule.set("font-family", new CSSValueList(',', names));
		epub.addToSpine(ops);
		chapter = ops.getDocument();
		chapter.addStyleResource(css);
		section = chapter.getBody();
		addChildren(doc.getRoot(), null, null);
	}

	void embedFonts(FontLocator fontLocator) {
		epub.addFonts(css, fontLocator);
	}

	public void setLog(PrintWriter log) {
		this.log = log;
	}
}
