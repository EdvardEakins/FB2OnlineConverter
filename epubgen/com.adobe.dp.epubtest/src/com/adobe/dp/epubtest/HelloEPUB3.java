package com.adobe.dp.epubtest;

import com.adobe.dp.css.*;
import com.adobe.dp.epub.io.DataSource;
import com.adobe.dp.epub.io.OCFContainerWriter;
import com.adobe.dp.epub.io.ResourceDataSource;
import com.adobe.dp.epub.ncx.TOCEntry;
import com.adobe.dp.epub.opf.*;
import com.adobe.dp.epub.ops.*;
import com.adobe.dp.epub.style.Stylesheet;
import com.adobe.dp.otf.DefaultFontLocator;
import com.adobe.dp.otf.FontLocator;

import java.io.FileOutputStream;

/**
 * Advanced epubgen example. Using bitmap images, font embedding, links and
 * inline SVG.
 */
public class HelloEPUB3 {

	public static void main(String[] args) {

		try {

			// create new EPUB document
			Publication epub = new Publication();

			// set up title and author
			epub.addDCMetadata("title", "My Third EPUB");
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
			h1Rule.set("font-family", new CSSQuotedString("Adobe Garamond Pro"));

			// style p element
			Selector pSelector = stylesheet.getSimpleSelector("p", null);
			SelectorRule pRule = stylesheet.getRuleForSelector(pSelector, true);
			pRule.set("margin", new CSSLength(0, "px"));
			pRule.set("text-indent", new CSSLength(1, "em"));
			pRule.set("text-align", new CSSName("justify"));
			h1Rule.set("font-family", new CSSQuotedString("Liberation Serif"));

			// style bitmap class (JPEG image)
			Selector bitmapSelector = stylesheet.getSimpleSelector(null,
					"bitmap");
			SelectorRule bitmapRule = stylesheet.getRuleForSelector(
					bitmapSelector, true);
			bitmapRule.set("width", new CSSLength(80, "%"));
			bitmapRule.set("max-width", new CSSLength(553, "px"));

			// style container class (container for JPEG image)
			Selector containerSelector = stylesheet.getSimpleSelector("p",
					"container");
			SelectorRule containerRule = stylesheet.getRuleForSelector(
					containerSelector, true);
			containerRule.set("text-align", new CSSName("center"));
			containerRule.set("text-indent", new CSSLength(0, "px"));
			CSSValue[] padval = { new CSSLength(0.5, "em"), new CSSLength(0, "px") };
			containerRule.set("padding", new CSSValueList(' ', padval));

			// style svgimage class (embedded SVG)
			Selector svgimageSelector = stylesheet.getSimpleSelector(null,
					"svgimage");
			SelectorRule svgimageRule = stylesheet.getRuleForSelector(svgimageSelector, true);
			svgimageRule.set("width", new CSSLength(80, "%"));
			CSSValue[] padval1 = { new CSSLength(0.5, "em"), new CSSLength(10, "%") };
			svgimageRule.set("padding", new CSSValueList(' ', padval1));

			// style label1 class (text in embedded SVG)
			Selector label1Selector = stylesheet.getSimpleSelector(null,
					"label1");
			SelectorRule label1Rule = stylesheet.getRuleForSelector(label1Selector, true);
			label1Rule.set("font-size", new CSSLength(36, "px"));
			label1Rule.set("font-family", new CSSQuotedString("Liberation Serif"));

			// style label2 class (text in embedded SVG)
			Selector label2Selector = stylesheet.getSimpleSelector(null,
					"label2");
			SelectorRule label2Rule = stylesheet.getRuleForSelector(label2Selector, true);
			label2Rule.set("font-size", new CSSLength(48, "px"));
			label2Rule.set("font-family", new CSSQuotedString("Comic Sans MS"));

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

			// add SVG graphics area
			SVGElement svg = chapter1Doc.createSVGElement("svg");
			svg.setAttribute("viewBox", "0 0 400 200");
			svg.setClassName("svgimage");
			body1.add(svg);

			// draw background
			SVGElement bg = chapter1Doc.createSVGElement("rect");
			bg.setAttribute("x", "1");
			bg.setAttribute("y", "1");
			bg.setAttribute("width", "398");
			bg.setAttribute("height", "198");
			bg.setAttribute("stroke", "black");
			bg.setAttribute("stroke-width", "2");
			bg.setAttribute("fill", "#FFF8EE");
			svg.add(bg);

			// draw a shape
			SVGElement path = chapter1Doc.createSVGElement("path");
			path.setAttribute("fill", "none");
			path.setAttribute("stroke", "black");
			path.setAttribute("stroke-width", "4");
			String resistorPath = "M90 120h50l10 20l20-40l20 40l20-40l20 40l20-40l20 40l20-40l20 40l10-20h50";
			path.setAttribute("d", resistorPath);
			svg.add(path);

			// draw a label
			SVGElement label1 = chapter1Doc.createSVGElement("text");
			label1.setClassName("label1");
			label1.setAttribute("x", "150");
			label1.setAttribute("y", "60");
			label1.add("R = 30\u03A9");
			svg.add(label1);

			// draw rotated label
			SVGElement label2 = chapter1Doc.createSVGElement("text");
			label2.setClassName("label2");
			label2.setAttribute("transform", "translate(40 110)rotate(-75)");
			SVGElement t1 = chapter1Doc.createSVGElement("tspan");
			t1.setAttribute("fill", "red");
			t1.add("S");
			label2.add(t1);
			SVGElement t2 = chapter1Doc.createSVGElement("tspan");
			t2.setAttribute("fill", "green");
			t2.add("V");
			label2.add(t2);
			SVGElement t3 = chapter1Doc.createSVGElement("tspan");
			t3.setAttribute("fill", "blue");
			t3.add("G");
			label2.add(t3);
			svg.add(label2);

			// create a small paragraph to use as a link target
			Element target = chapter1Doc.createElement("p");
			target.add("Link in the second chapter points here.");
			body1.add(target);

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

			// add a bitmap resource
			DataSource dataSource = new ResourceDataSource(HelloEPUB3.class,
					"cassini.jpg");
			BitmapImageResource imageResource = epub.createBitmapImageResource(
					"OPS/images/cassini.jpg", "image/jpeg", dataSource);

			// add a bitmap image
			Element container = chapter2Doc.createElement("p");
			container.setClassName("container");
			body2.add(container);
			ImageElement bitmap = chapter2Doc.createImageElement("img");
			bitmap.setClassName("bitmap");
			bitmap.setImageResource(imageResource);
			container.add(bitmap);

			// and another paragraph
			Element paragraph3 = chapter2Doc.createElement("p");
			StringBuffer sb3 = new StringBuffer();
			for (int i = 1; i <= 6; i++)
				sb3.append("This is sentence " + i
						+ " of the second chapter's second paragraph. ");
			paragraph3.add(sb3.toString());
			body2.add(paragraph3);

			// add a link to the target paragraph in the first chapter
			HyperlinkElement a = chapter2Doc.createHyperlinkElement("a");
			a.setXRef(target.getSelfRef());
			a.add("Here is a link.");
			paragraph3.add(a);

			// embed fonts
			// NB: on non-Windows platforms you need to supply your own
			// FontLocator implementation or place fonts in ~/.epubfonts
			FontLocator fontLocator = DefaultFontLocator.getInstance("/usr/share/fonts/truetype");
			// epub.useAdobeFontMangling();
			epub.cascadeStyles();
			epub.addFonts(style, fontLocator);

			// save EPUB to an OCF container
			OCFContainerWriter writer = new OCFContainerWriter(
					new FileOutputStream("hello.epub"));
			epub.serialize(writer);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
