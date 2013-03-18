package com.adobe.dp.epubtest;

import java.io.FileOutputStream;

import com.adobe.dp.css.CSSLength;
import com.adobe.dp.css.CSSName;
import com.adobe.dp.css.CSSValue;
import com.adobe.dp.css.CSSValueList;
import com.adobe.dp.css.Selector;
import com.adobe.dp.css.SelectorRule;
import com.adobe.dp.epub.io.OCFContainerWriter;
import com.adobe.dp.epub.ncx.TOCEntry;
import com.adobe.dp.epub.opf.NCXResource;
import com.adobe.dp.epub.opf.OPSResource;
import com.adobe.dp.epub.opf.Publication;
import com.adobe.dp.epub.opf.StyleResource;
import com.adobe.dp.epub.ops.Element;
import com.adobe.dp.epub.ops.OPSDocument;
import com.adobe.dp.epub.style.Stylesheet;

/**
 * Intermediate epubgen example. Using stylesheet and multiple chapters.
 */
public class HelloEPUB2 {

	public static void main(String[] args) {

		try {

			// create new EPUB document
			Publication epub = new Publication();

			// set up title and author
			epub.addDCMetadata("title", "My Second EPUB");
			epub.addDCMetadata("creator", System.getProperty("user.name"));
			epub.addDCMetadata("language", "en");

			// prepare table of contents
			NCXResource toc = epub.getTOC();
			TOCEntry rootTOCEntry = toc.getRootTOCEntry();

			// create a stylesheet
			StyleResource style = epub.createStyleResource("OPS/styles.css");
			Stylesheet stylesheet = style.getStylesheet();

			// style h1 element
			Selector h1Selector = stylesheet.getSimpleSelector("h1", null);
			SelectorRule h1Rule = stylesheet.getRuleForSelector(h1Selector,
					true);
			h1Rule.set("color", new CSSName("gray"));
			CSSValue[] border = { new CSSLength(2, "px"), new CSSName("solid"),
					new CSSName("gray") };
			h1Rule.set("border-bottom", new CSSValueList(' ', border));
			h1Rule.set("text-align", new CSSName("right"));
			CSSValue[] margin = { new CSSLength(2, "em"),
					new CSSLength(8, "px"), new CSSLength(1, "em"),
					new CSSLength(0, "px") };
			h1Rule.set("margin", new CSSValueList(' ', margin));

			// style p element
			Selector pSelector = stylesheet.getSimpleSelector("p", null);
			SelectorRule pRule = stylesheet.getRuleForSelector(pSelector, true);
			pRule.set("margin", new CSSLength(0, "px"));
			pRule.set("text-indent", new CSSLength(1, "em"));
			pRule.set("text-align", new CSSName("justify"));

			// create first chapter resource
			OPSResource chapter1 = epub.createOPSResource("OPS/chapter1.html");
			epub.addToSpine(chapter1);

			// get chapter document
			OPSDocument chapter1Doc = chapter1.getDocument();

			// link our stylesheet
			chapter1Doc.addStyleResource(style);

			// add chapter to the table of contents
			TOCEntry chapter1TOCEntry = toc.createTOCEntry("Chapter 1",
					chapter1Doc.getRootXRef());
			rootTOCEntry.add(chapter1TOCEntry);

			// chapter XHTML body element
			Element body1 = chapter1Doc.getBody();

			// add a header
			Element header1 = chapter1Doc.createElement("h1");
			header1.add("One");
			body1.add(header1);

			// add a paragraph
			Element paragraph1 = chapter1Doc.createElement("p");
			StringBuffer sb1 = new StringBuffer();
			for (int i = 1; i <= 6; i++)
				sb1.append("This is sentence " + i
						+ " of the first chapter's first paragraph. ");
			paragraph1.add(sb1.toString());
			body1.add(paragraph1);

			// create second chapter resource
			OPSResource chapter2 = epub.createOPSResource("OPS/chapter2.html");
			epub.addToSpine(chapter2);

			// get chapter document
			OPSDocument chapter2Doc = chapter2.getDocument();

			// link our stylesheet
			chapter2Doc.addStyleResource(style);

			// add chapter to the table of contents
			TOCEntry chapter2TOCEntry = toc.createTOCEntry("Chapter 2",
					chapter2Doc.getRootXRef());
			rootTOCEntry.add(chapter2TOCEntry);

			// chapter XHTML body element
			Element body2 = chapter2Doc.getBody();

			// add a header
			Element header2 = chapter1Doc.createElement("h1");
			header2.add("Two");
			body2.add(header2);

			// add a paragraph
			Element paragraph2 = chapter2Doc.createElement("p");
			StringBuffer sb2 = new StringBuffer();
			for (int i = 1; i <= 6; i++)
				sb2.append("This is sentence " + i
						+ " of the second chapter's first paragraph. ");
			paragraph2.add(sb2.toString());
			body2.add(paragraph2);

			// and another one
			Element paragraph3 = chapter2Doc.createElement("p");
			StringBuffer sb3 = new StringBuffer();
			for (int i = 1; i <= 6; i++)
				sb3.append("This is sentence " + i
						+ " of the second chapter's second paragraph. ");
			paragraph3.add(sb3.toString());
			body2.add(paragraph3);

			// save EPUB to an OCF container
			OCFContainerWriter writer = new OCFContainerWriter(
					new FileOutputStream("hello.epub"));
			epub.serialize(writer);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
