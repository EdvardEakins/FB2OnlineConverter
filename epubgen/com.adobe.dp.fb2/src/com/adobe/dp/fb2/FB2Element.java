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

package com.adobe.dp.fb2;

import com.adobe.dp.css.CascadeEngine;
import com.adobe.dp.css.CascadeResult;
import com.adobe.dp.css.InlineRule;
import com.adobe.dp.xml.util.SMap;

public abstract class FB2Element {

	SMap attrs;

	CascadeResult cascade;

	Object[] children;

	String id;

	int size = -1;

	public abstract String getName();

	public String contentAsString() {
		StringBuffer sb = new StringBuffer();
		contentAsString(sb);
		return sb.toString();
	}

	public Object[] getChildren() {
		return children;
	}

	public int getUTF16Size() {
		if (size < 0)
			calculateSize();
		return size;
	}

	public static int getUTF16Size(Object obj) {
		if (obj instanceof FB2Element) {
			return ((FB2Element) obj).getUTF16Size();
		} else {
			return obj.toString().length();
		}
	}

	private void contentAsString(StringBuffer sb) {
		for (int i = 0; i < children.length; i++) {
			if (children[i] instanceof FB2Element) {
				((FB2Element) children[i]).contentAsString(sb);
				sb.append(" ");
			} else {
				sb.append(children[i]);
			}
		}
	}

	private void calculateSize() {
		size = 0;
		for (int i = 0; i < children.length; i++) {
			size += getUTF16Size(children[i]);
		}
	}

	public InlineRule getStyle() {
		return null;
	}

	public boolean acceptsText() {
		return false;
	}

	public String getId() {
		return id;
	}

	public CascadeResult getCascade() {
		return cascade;
	}

	public String getNamespace() {
		return FB2Document.fb2NS;
	}

	public void applyStyles(CascadeEngine cascadeEngine) {
		cascadeEngine.pushElement(getNamespace(), getName(), attrs);
		cascadeEngine.applyInlineRule(getStyle());
		cascade = cascadeEngine.getCascadeResult();
		for (int i = 0; i < children.length; i++) {
			if (children[i] instanceof FB2Element) {
				((FB2Element) children[i]).applyStyles(cascadeEngine);
			}
		}
		cascadeEngine.popElement();
	}
}
