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
import com.adobe.dp.epub.conv.Version;
import com.adobe.dp.epub.io.ContainerSource;
import com.adobe.dp.epub.opf.NCXResource;
import com.adobe.dp.epub.opf.OPSResource;
import com.adobe.dp.epub.opf.Publication;
import com.adobe.dp.epub.opf.StyleResource;
import com.adobe.dp.epub.otf.FontEmbeddingReport;
import com.adobe.dp.epub.style.Stylesheet;
import com.adobe.dp.office.word.*;
import com.adobe.dp.otf.DefaultFontLocator;
import com.adobe.dp.otf.FontLocator;
import com.adobe.dp.xml.util.StringUtil;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;

public class DOCXConverter {

	WordDocument doc;

	Publication epub;

	NCXResource toc;

	StyleResource global;

	StyleConverter styleConverter;

	// maps Footnote IDs to Footnote XRef
	Hashtable footnoteMap = new Hashtable();

	Hashtable convResources = new Hashtable();

	ContainerSource wordResources;

	FontLocator fontLocator;

	double defaultFontSize;

	PrintWriter log = new PrintWriter(new OutputStreamWriter(System.out));

	boolean useWordPageBreaks;

	String lang;

	public DOCXConverter(WordDocument doc, Publication epub) {
		this.doc = doc;
		this.epub = epub;
        this.fontLocator = DefaultFontLocator.getInstance(DefaultFontLocator.BUILT_IN_DIRS);

		global = epub.createStyleResource("OPS/global.css");
		Stylesheet globalStylesheet = global.getStylesheet();

		styleConverter = new StyleConverter(false);
		toc = epub.getTOC();

		Style rs = doc.getDefaultParagraphStyle();
		if (rs != null) {
			RunProperties rp = rs.getRunProperties();
			if (rp != null)
				lang = (String) rp.get("lang");
		}

		// default font size - have to happen early
		RunProperties rp = doc.getDocumentDefaultRunStyle().getRunProperties();
		if (rp != null) {
			Object sz = rp.get("sz");
			if (sz instanceof Number)
				defaultFontSize = ((Number) sz).doubleValue();
			StylingResult res = styleConverter.styleElement(rp, false, 1, false, false);
			if (res.elementRule != null && !res.elementRule.isEmpty()) {
				SelectorRule bodyRule = globalStylesheet.getRuleForSelector(globalStylesheet.getSimpleSelector("body", null),
						true);
				Iterator it = res.elementRule.properties();
				while (it.hasNext()) {
					String property = (String) it.next();
					bodyRule.set(property, res.elementRule.get(property));
				}
			}
			if (lang == null)
				lang = (String) rp.get("lang");
		}
		if (defaultFontSize < 1)
			defaultFontSize = 20;

		SelectorRule bodyEmbedRule = globalStylesheet.getRuleForSelector(globalStylesheet.getSimpleSelector("body", "embed"),
				true);
		bodyEmbedRule.set("font-size", new CSSLength(defaultFontSize / 2, "px"));

		styleConverter.setDefaultFontSize(defaultFontSize);
		styleConverter.setDocumentDefaultParagraphStyle(doc.getDocumentDefaultParagraphStyle());

		// default table styles
		SelectorRule tableRule = globalStylesheet.getRuleForSelector(globalStylesheet.getSimpleSelector("table", null), true);
		tableRule.set("border-collapse", new CSSName("collapse"));
		tableRule.set("border-spacing", new CSSLength(0, "px"));

		// default paragraph styles
		// unlike XHTML, Word's default spacing/margings are zero
		SelectorRule pRule = globalStylesheet.getRuleForSelector(globalStylesheet.getSimpleSelector("p", null), true);
		pRule.set("margin-top", new CSSLength(0, "px"));
		pRule.set("margin-bottom", new CSSLength(0, "px"));
		SelectorRule ulRule = globalStylesheet.getRuleForSelector(globalStylesheet.getSimpleSelector("ul", null), true);
		// Word puts margins on li, not ul elements
		ulRule.set("padding-left", new CSSLength(0, "px")); // most CSS engines
															// have default
		// padding on ul element
		ulRule.set("margin", new CSSLength(0, "px")); // left margin override
														// needed for older
		// Digital Editions
		SelectorRule nestedLiRule = globalStylesheet.getRuleForSelector(globalStylesheet.getSimpleSelector("li", "nested"),
				true);
		nestedLiRule.set("display", new CSSName("block"));
	}

	public void setFontLocator(FontLocator fontLocator) {
		this.fontLocator = fontLocator;
	}

	public void setLog(PrintWriter log) {
		this.log = log;
	}

	public void convert() {

		OPSResource footnotes = null;
		if (doc.getFootnotes() != null) {
			// process footnotes first to build footnote map
			BodyElement fbody = doc.getFootnotes();
			footnotes = epub.createOPSResource("OPS/footnotes.xhtml");
			WordMLConverter footnoteConv = new WordMLConverter(doc, epub, styleConverter, log);
			footnoteConv.setFootnoteMap(footnoteMap);
			footnoteConv.setWordResources(wordResources);
			footnoteConv.convert(fbody, footnotes, false);
			if (footnoteMap.size() > 0) {
				Stylesheet ss = global.getStylesheet();
				Selector selector = ss.getSimpleSelector(null, "footnote-ref");
				SelectorRule rule = ss.getRuleForSelector(selector, true);
				rule.set("font-size", new CSSLength(0.7, "em"));
				rule.set("vertical-align", new CSSName("super"));
				rule.set("line-height", new CSSNumber(0.2));
				selector = ss.getSimpleSelector(null, "footnote-title");
				rule = ss.getRuleForSelector(selector, true);
				rule.set("margin", new CSSLength(0, "px"));
				CSSValue[] padvals = { new CSSLength(1, "em"), new CSSLength(0, "px"), new CSSLength(0.5, "em"),
						new CSSLength(2, "em") };
				rule.set("padding", new CSSValueList(' ', padvals));
			} else {
				epub.removeResource(footnotes);
			}
		}

		BodyElement body = doc.getBody();
		WordMLConverter bodyConv = new WordMLConverter(doc, epub, styleConverter, log);
		bodyConv.setFootnoteMap(footnoteMap);
		bodyConv.setWordResources(wordResources);
		bodyConv.findLists(body);
		OPSResource ops = epub.createOPSResource("OPS/document.xhtml");
		if (useWordPageBreaks) {
			epub.getTOC().addPage(null, ops.getDocument().getRootXRef());
			bodyConv.useWordPageBreaks();
		}
		bodyConv.convert(body, ops, true);

		if (footnotes != null)
			epub.addToSpine(footnotes);

		if (bodyConv.includeWordMetadata) {
			// add EPUB metadata from Word metadata, do it in the end, so that
			// metadata from commands comes first
			Iterator metadata = doc.metadata();
			while (metadata.hasNext()) {
				MetadataItem item = (MetadataItem) metadata.next();
				epub.addMetadata(item.getNS(), item.getName(), item.getValue());
				if (item.getNS().equals("http://purl.org/dc/terms/") && item.getName().equals("modified")) {
					epub.addDCMetadata("date", item.getValue());
				}
			}
		}

		if (lang != null && epub.getDCMetadata("language") == null) {
			epub.addDCMetadata("language", lang);
		}

		epub.addMetadata(null, "DOCX2EPUB.version", Version.VERSION);
		epub.addMetadata(null, "DOCX2EPUB.conversionDate", StringUtil.dateToW3CDTF(new Date()));

		epub.generateTOCFromHeadings(5);
		epub.generateStyles(global);
		epub.splitLargeChapters();
		epub.cascadeStyles();

		log.flush();
	}

	public void useWordPageBreaks() {
		useWordPageBreaks = true;
	}

	public FontEmbeddingReport embedFonts() {
        return epub.addFonts(global, fontLocator);
	}

	public void setWordResources(ContainerSource source) {
		wordResources = source;
	}

}
