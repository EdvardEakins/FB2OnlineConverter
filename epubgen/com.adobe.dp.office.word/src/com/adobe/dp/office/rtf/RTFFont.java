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

package com.adobe.dp.office.rtf;

import java.util.Vector;

import com.adobe.dp.css.CSSName;
import com.adobe.dp.css.CSSQuotedString;
import com.adobe.dp.css.CSSValue;
import com.adobe.dp.css.CSSValueList;

public class RTFFont {

	public static final int ROMAN = 0;

	public static final int SWISS = 1;

	public static final int MODERN = 2;

	public static final int SCRIPT = 3;

	public static final int DECOR = 4;

	public static final int TECH = 5;

	int type;

	String encoding;

	String name;

	public String getEncoding() {
		return encoding;
	}

	public String getName() {
		return name;
	}

	public int getType() {
		return type;
	}

	public CSSValue toCSSValue() {
		Vector list = new Vector();
		if (name != null && name.length() > 0) {
			if (name.indexOf(" ") >= 0) {
				list.add(new CSSQuotedString(name));
			} else {
				list.add(new CSSName(name));
			}
		}
		switch (type) {
		case ROMAN:
			list.add(new CSSName("serif"));
			break;
		case SWISS:
			list.add(new CSSName("sans-serif"));
			break;
		case MODERN:
		case TECH:
			list.add(new CSSName("monospace"));
			break;
		case SCRIPT:
			list.add(new CSSName("cursive"));
			list.add(new CSSName("serif"));
			break;
		case DECOR:
			list.add(new CSSName("fantasy"));
			list.add(new CSSName("serif"));
			break;
		}
		CSSValue[] arr = new CSSValue[list.size()];
		list.copyInto(arr);
		return new CSSValueList(' ', arr);
	}
}
