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

package com.adobe.dp.epub.otf;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import java.util.Vector;

import com.adobe.dp.css.BaseRule;
import com.adobe.dp.css.CSSName;
import com.adobe.dp.css.CSSNumber;
import com.adobe.dp.css.CSSQuotedString;
import com.adobe.dp.css.CSSValue;
import com.adobe.dp.css.CSSValueList;
import com.adobe.dp.css.CascadeResult;
import com.adobe.dp.css.FontFaceRule;
import com.adobe.dp.css.InlineRule;
import com.adobe.dp.epub.io.BufferedDataSource;
import com.adobe.dp.epub.opf.FontResource;
import com.adobe.dp.epub.opf.Publication;
import com.adobe.dp.epub.opf.StyleResource;
import com.adobe.dp.epub.ops.Element;
import com.adobe.dp.otf.FontInputStream;
import com.adobe.dp.otf.FontLocator;
import com.adobe.dp.otf.FontProperties;
import com.adobe.dp.otf.FontPropertyConstants;
import com.adobe.dp.otf.OpenTypeFont;

public class FontSubsetter implements FontEmbeddingReport {

	class SubsetterEntry {
		OpenTypeFont font;

		boolean used;
	}

	class FontEntry {
		SubsetterEntry subsetter;

		String familyName;

		int weight = FontPropertyConstants.WEIGHT_NORMAL;

		int style = FontPropertyConstants.STYLE_REGULAR;

		FontEntry cloneEntry() {
			FontEntry entry = new FontEntry();
			entry.familyName = familyName;
			entry.weight = weight;
			entry.style = style;
			return entry;
		}

		public int hashCode() {
			return familyName.hashCode() + weight + style;
		}

		public boolean equals(Object other) {
			if (other.getClass() != getClass())
				return false;
			FontEntry o = (FontEntry) other;
			return o.familyName.equals(familyName) && o.weight == weight && o.style == style;
		}

	}

	static class FontListEntry {

		SubsetterEntry[] subsetterList;

		String[] familyName;

		int weight = FontPropertyConstants.WEIGHT_NORMAL;

		int style = FontPropertyConstants.STYLE_REGULAR;

		FontListEntry cloneEntry() {
			FontListEntry entry = new FontListEntry();
			entry.familyName = familyName;
			entry.weight = weight;
			entry.style = style;
			return entry;
		}

		public int hashCode() {
			return familyName.hashCode() + weight + style;
		}

		public boolean equals(Object other) {
			if (other.getClass() != getClass())
				return false;
			FontListEntry o = (FontListEntry) other;
			return o.familyName.equals(familyName) && o.weight == weight && o.style == style;
		}
	}

	Publication epub;

	Stack entryStack = new Stack();

	FontListEntry currentEntry;

	StyleResource styleResource;

	Vector styles;

	Hashtable subsetters = new Hashtable();

	Hashtable subsetterLists = new Hashtable();

	FontLocator fontLocator;

	Set missingFonts = new TreeSet();

	Set prohibitedFonts = new TreeSet();

	long totalPlay;

	public FontSubsetter(Publication epub, StyleResource styleResource, FontLocator locator) {
		this.styleResource = styleResource;
		this.fontLocator = locator;
	}

	public Iterator missingFonts() {
		return missingFonts.iterator();
	}

	public Iterator prohibitedFonts() {
		return prohibitedFonts.iterator();
	}

	public Iterator usedFonts() {
		Iterator keys = subsetters.keySet().iterator();
		Set usedFonts = new TreeSet();
		while (keys.hasNext()) {
			FontEntry entry = (FontEntry) keys.next();
			if (entry.subsetter.used) {
				FontProperties prop = new FontProperties(entry.familyName, entry.weight, entry.style);
				usedFonts.add(prop);
			}
		}
		return usedFonts.iterator();
	}

	public void setStyles(Vector styles) {
		this.styles = styles;
	}

	private String[] parseFamily(Object family) {
		if (family instanceof CSSValueList) {
			CSSValueList list = (CSSValueList) family;
			int len = list.getSeparator() == ',' ? list.length() : 1;
			String[] result = new String[len];
			for (int i = 0; i < len; i++) {
				CSSValue v = list.getSeparator() == ',' ? list.item(i) : list;
				result[i] = v.toString();
			}
			return result;
		} else if ((family instanceof CSSQuotedString) || (family instanceof CSSName)) {
			String[] result = { family.toString() };
			return result;
		}
		String[] result = { "serif" };
		return result;
	}

	private int parseWeight(Object weight) {
		if (weight instanceof CSSNumber) {
			try {
				return ((CSSNumber) weight).getNumber().intValue();
			} catch (Exception e) {
			}
		} else if (weight.toString().toLowerCase().equals("bold")) {
			return FontProperties.WEIGHT_BOLD;
		}
		return FontProperties.WEIGHT_NORMAL;
	}

	private int parseStyle(Object style) {
		style = style.toString().toLowerCase();
		if (style.equals("italic")) {
			return FontProperties.STYLE_ITALIC;
		}
		if (style.equals("oblique")) {
			return FontProperties.STYLE_OBLIQUE;
		}
		return FontProperties.STYLE_REGULAR;
	}

	private void processCascadeResult(CascadeResult cascade) {
		// TODO: other media?
		if (cascade != null) {
			InlineRule rule = cascade.getProperties().getPropertySet();
			processRule(rule);
		}
	}

	private void processRule(BaseRule rule) {
		if (rule == null)
			return;
		Object family = rule.get("font-family");
		if (family != null)
			currentEntry.familyName = parseFamily(family);
		Object weight = rule.get("font-weight");
		if (weight != null)
			currentEntry.weight = parseWeight(weight);
		Object style = rule.get("font-style");
		if (style != null)
			currentEntry.style = parseStyle(style);
	}

	private void processBuiltInStyles(String name) {
		if (name.equals("h1") || name.equals("h2") || name.equals("h3") || name.equals("h4") || name.equals("h5")
				|| name.equals("h6") || name.equals("b") || name.equals("strong"))
			currentEntry.weight = FontProperties.WEIGHT_BOLD;
		else if (name.equals("i") || name.equals("em"))
			currentEntry.style = FontProperties.STYLE_ITALIC;
	}

	public void push(Element e) {
		if (currentEntry == null) {
			currentEntry = new FontListEntry();
		} else {
			entryStack.push(currentEntry);
			currentEntry = currentEntry.cloneEntry();
		}
		// built-in stylesheet
		processBuiltInStyles(e.getElementName());
		// document stylesheets
		processCascadeResult(e.getCascadeResult());
		// style attribute: highest specificity
		processRule(e.getStyle());
		if (currentEntry.familyName != null) {
			currentEntry.subsetterList = (SubsetterEntry[]) subsetterLists.get(currentEntry);
			if (currentEntry.subsetterList == null) {
				Vector subsetterList = new Vector();
				for (int i = 0; i < currentEntry.familyName.length; i++) {
					String family = currentEntry.familyName[i];
					if (family.equals("serif") || family.equals("sans-serif") || family.equals("monospace"))
						continue; // built-in
					FontEntry entry = new FontEntry();
					entry.familyName = family;
					entry.style = currentEntry.style;
					entry.weight = currentEntry.weight;
					SubsetterEntry subsetter = (SubsetterEntry) subsetters.get(entry);
					if (subsetter == null) {
						try {
							FontProperties prop = new FontProperties(entry.familyName, entry.weight, entry.style);
							FontInputStream stream = fontLocator.locateFont(prop);
							if (stream != null) {
								OpenTypeFont font = new OpenTypeFont(stream);
								if (font.canEmbedForReading() && font.canSubset()) {
									subsetter = new SubsetterEntry();
									subsetter.font = font;
									subsetters.put(entry, subsetter);
									entry.subsetter = subsetter;
								} else {
									prohibitedFonts.add(prop);
								}
							} else {
								missingFonts.add(prop);
							}
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}
					if (subsetter != null)
						subsetterList.add(subsetter);
				}
				currentEntry.subsetterList = new SubsetterEntry[subsetterList.size()];
				subsetterList.copyInto(currentEntry.subsetterList);
				subsetterLists.put(currentEntry, currentEntry.subsetterList);
			}
		}
	}

	public void pop(Element e) {
		if (entryStack.isEmpty())
			currentEntry = null;
		else
			currentEntry = (FontListEntry) entryStack.pop();
	}

	public void play(String text) {
		if (currentEntry != null && currentEntry.subsetterList != null) {
			long t0 = System.currentTimeMillis();
			int subsetterListLen = currentEntry.subsetterList.length;
			int stringLength = text.length();
			for (int i = 0; i < stringLength; i++) {
				char c = text.charAt(i);
				for (int j = 0; j < subsetterListLen; j++) {
					SubsetterEntry subsetter = currentEntry.subsetterList[j];
					if (subsetter.font != null) {
						if (subsetter.font.play(c)) {
							subsetter.used = true;
							break;
						}
					}
				}
			}
			long t1 = System.currentTimeMillis();
			totalPlay += (t1 - t0);
		}
	}

	public void addFonts(Publication epub) {
		// System.out.println("\t\tplaying text for subsetting: " + totalPlay
		// /1000.0 + " seconds");
		// long t0 = System.currentTimeMillis();
		Enumeration list = subsetters.keys();
		while (list.hasMoreElements()) {
			FontEntry entry = (FontEntry) list.nextElement();
			if (!entry.subsetter.used)
				continue;
			BufferedDataSource bds = new BufferedDataSource();
			try {
				OpenTypeFont font = entry.subsetter.font;
				String resName = entry.familyName.replaceAll(" ", "-") + "-" + entry.weight;
				switch (entry.style) {
				case FontPropertyConstants.STYLE_ITALIC:
					resName += "-Italic";
					break;
				case FontPropertyConstants.STYLE_OBLIQUE:
					resName += "-Oblique";
					break;
				}
				String primaryUUID = epub.getPrimaryIdentifier();
				if (primaryUUID != null && primaryUUID.startsWith("urn:uuid:")) {
					resName = resName + "-" + primaryUUID.substring(9);
				}
				bds.getOutputStream().write(font.getSubsettedFont());
				String folder = epub.getContentFolder() == null ? "" : epub.getContentFolder() + "/";
				FontResource fontResource = epub.createFontResource(folder + "fonts/" + resName + ".otf", bds);
				FontFaceRule face = styleResource.getStylesheet().createFontFace(fontResource);
				face.set("font-family", new CSSQuotedString(entry.familyName));
				switch (entry.weight) {
				case FontPropertyConstants.WEIGHT_NORMAL:
					face.set("font-weight", new CSSName("normal"));
					break;
				case FontPropertyConstants.WEIGHT_BOLD:
					face.set("font-weight", new CSSName("bold"));
					break;
				default:
					face.set("font-weight", new CSSNumber(new Integer(entry.weight)));
					break;
				}
				switch (entry.style) {
				case FontPropertyConstants.STYLE_ITALIC:
					face.set("font-style", new CSSName("italic"));
					break;
				case FontPropertyConstants.STYLE_OBLIQUE:
					face.set("font-style", new CSSName("oblique"));
					break;
				default:
					face.set("font-style", new CSSName("normal"));
					break;
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		// long t1 = System.currentTimeMillis();
		// System.out.println("\t\twriting font subset: " + (t1 - t0) /1000.0 +
		// " seconds");
	}
}
