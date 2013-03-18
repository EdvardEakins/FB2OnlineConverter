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

import java.io.IOException;
import java.util.Hashtable;

import com.adobe.dp.css.CSSStylesheet;
import com.adobe.dp.css.CSSURL;
import com.adobe.dp.otf.ByteArrayFontInputStream;
import com.adobe.dp.otf.FontInputStream;
import com.adobe.dp.otf.FontLocator;
import com.adobe.dp.otf.FontProperties;

public class EmbeddedFontLocator extends FontLocator {

	Hashtable fontSrcs;

	CSSStylesheet stylesheet;

	FontLocator chained;

	public EmbeddedFontLocator(CSSStylesheet stylesheet, FontLocator chained) {
		this.stylesheet = stylesheet;
		this.chained = chained;
	}

	public FontInputStream locateFont(FontProperties key) throws IOException {
		CSSURL src = (CSSURL) fontSrcs.get(key);
		try {
			if (src != null) {
				return new ByteArrayFontInputStream(src.getData());
			}
		} catch (IOException e) {
		}
		return chained.locateFont(key);
	}

	public boolean hasFont(FontProperties key) {
		CSSURL src = (CSSURL) fontSrcs.get(key);
		try {
			if (src != null && src.getData() != null)
				return true;
		} catch (IOException e) {
		}
		return chained.hasFont(key);
	}

}
