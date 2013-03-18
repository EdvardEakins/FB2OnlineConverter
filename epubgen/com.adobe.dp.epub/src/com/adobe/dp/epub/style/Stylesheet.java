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

package com.adobe.dp.epub.style;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Hashtable;
import java.util.Iterator;

import com.adobe.dp.css.CSSParser;
import com.adobe.dp.css.CSSStylesheet;
import com.adobe.dp.css.CSSValue;
import com.adobe.dp.css.CascadeResult;
import com.adobe.dp.css.FontFaceRule;
import com.adobe.dp.css.InlineRule;
import com.adobe.dp.css.Selector;
import com.adobe.dp.css.SelectorRule;
import com.adobe.dp.epub.opf.FontResource;
import com.adobe.dp.epub.opf.StyleResource;

public class Stylesheet {

	StyleResource owner;

	Hashtable classByProps = new Hashtable();

	CSSStylesheet css;

	public Stylesheet(StyleResource owner) {
		this.owner = owner;
		css = new CSSStylesheet();
	}

	public Stylesheet(StyleResource owner, InputStream in) throws IOException {
		this.owner = owner;
		CSSParser parser = new CSSParser();
		this.css = parser.readStylesheet(in);
		initExisting();
	}

	public Stylesheet(StyleResource owner, CSSStylesheet css) {
		this.owner = owner;
		this.css = css;
		initExisting();
	}

	private void initExisting() {
		// TODO: populate classByProps
	}

	public Selector getSimpleSelector(String elementName, String className) {
		return css.getSimpleSelector(elementName, className);
	}

	public SelectorRule getRuleForSelector(Selector selector, boolean create) {
		return css.getRuleForSelector(selector, create);
	}

	public FontFaceRule createFontFace(FontResource fontResource) {
		FontFaceRule fontFace = new FontFaceRule();
		css.add(fontFace);
		fontFace.set("src", new ResourceURL(owner, fontResource.getResourceRef()));
		return fontFace;
	}

	public String makeClass(String className, CascadeResult props) {
		String cls = (String) classByProps.get(props);
		if (cls != null)
			return cls;
		if (className == null)
			className = "z";
		props = props.cloneObject();
		int count = 1;
		cls = className;
		Selector selector;
		while (true) {
			selector = css.getSimpleSelector(null, cls);
			if (css.getRuleForSelector(selector, false) == null)
				break;
			cls = className + (count++);
		}
		classByProps.put(props, cls);
		SelectorRule rule = css.getRuleForSelector(selector, true);
		InlineRule p = props.getProperties().getPropertySet();
		Iterator ps = p.properties();
		while (ps.hasNext()) {
			String pn = (String) ps.next();
			CSSValue pv = p.get(pn);
			rule.set(pn, pv);
		}
		return cls;
	}

	public CSSStylesheet getCSS() {
		return css;
	}

	public void serialize(PrintWriter pout) {
		css.serialize(pout);
	}

	public void addDirectStyles(InputStream in) throws IOException {
		CSSParser parser = new CSSParser();
		parser.readStylesheet(in, css);
		initExisting();
	}
}
