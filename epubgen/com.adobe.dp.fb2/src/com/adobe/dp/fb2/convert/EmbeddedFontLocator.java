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
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import com.adobe.dp.css.*;
import com.adobe.dp.otf.ByteArrayFontInputStream;
import com.adobe.dp.otf.FontInputStream;
import com.adobe.dp.otf.FontLocator;
import com.adobe.dp.otf.FontProperties;
import org.omg.CosNaming._BindingIteratorImplBase;

public class EmbeddedFontLocator extends FontLocator {

	Map<FontProperties, CSSURL> fontSrcs = new HashMap<FontProperties, CSSURL>();

	CSSStylesheet stylesheet;

	FontLocator chained;

	public EmbeddedFontLocator(CSSStylesheet stylesheet, FontLocator chained) {
		this.stylesheet = stylesheet;
		this.chained = chained;
        Iterator cssStatements = stylesheet.statements();
        while (cssStatements.hasNext()) {
            Object o = cssStatements.next();
            if (o instanceof FontFaceRule) {
                FontFaceRule fontFaceRule = (FontFaceRule) o;
                CSSValue fontSrc = fontFaceRule.get("src");
                if (fontSrc instanceof CSSURL) {
                    CSSValue fontFamily = fontFaceRule.get("font-family");
                    CSSValue fontStyle = fontFaceRule.get("font-style");
                    CSSValue fontWeight = fontFaceRule.get("font-weight");

                    if (fontFamily != null) {
                        int count = CSSValueList.valueCount(fontFamily, ',');
                        for (int i = 0; i < count; i++) {
                            String family = getStringValue(CSSValueList.valueAt(fontFamily, i, ','));
                            FontProperties fp = new FontProperties(family, getStringValue(fontWeight), getStringValue(fontStyle));
                            fontSrcs.put(fp, (CSSURL) fontSrc);
                        }

                    }
                }


            }
        }
    }

    private String getStringValue(Object val) {
        if (val instanceof CSSQuotedString) {
            return ((CSSQuotedString) val).getText();
        }
        return val.toString();
    }

	public FontInputStream locateFont(FontProperties key) throws IOException {
		CSSURL src = fontSrcs.get(key);
		try {
			if (src != null) {
				return new ByteArrayFontInputStream(src.getData());
			}
		} catch (IOException ignored) {
		}
		return chained.locateFont(key);
	}

	public boolean hasFont(FontProperties key) {
		CSSURL src = fontSrcs.get(key);
		try {
			if (src != null && src.getData() != null)
				return true;
		} catch (IOException ignored) {
		}
		return chained.hasFont(key);
	}

}
