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

package com.adobe.dp.epub.opf;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import com.adobe.dp.css.CSSParser;
import com.adobe.dp.css.CSSStylesheet;
import com.adobe.dp.epub.io.DataSource;
import com.adobe.dp.epub.style.EPUBCSSURLFactory;
import com.adobe.dp.epub.style.Stylesheet;

public class StyleResource extends Resource {

	Stylesheet stylesheet;
	
	StyleResource(Publication epub, String name) {
		super(epub, name, "text/css", null);
	}
	
	public Stylesheet getStylesheet() {
		if( stylesheet == null )
			stylesheet = new Stylesheet(this);
		return stylesheet;
	}
	
	public void serialize(OutputStream out) throws IOException {
		PrintWriter pout = new PrintWriter(new OutputStreamWriter(out, "UTF-8"));
		getStylesheet().serialize(pout);
		pout.close();
	}
	
	public void load(DataSource data) throws IOException {
		CSSParser parser = new CSSParser();
		parser.setCSSURLFactory(new EPUBCSSURLFactory(this));
		CSSStylesheet css = parser.readStylesheet(data.getInputStream());
		stylesheet = new Stylesheet(this, css);
	}
	
	public void setCSS(CSSStylesheet css) {
		stylesheet = new Stylesheet(this, css);
	}
	
}
