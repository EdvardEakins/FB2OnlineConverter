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
package com.adobe.dp.office.vml;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.xml.sax.Attributes;

import com.adobe.dp.office.types.RGBColor;
import com.adobe.dp.office.word.ContainerElement;
import com.adobe.dp.office.word.TXBXContentElement;

public class VMLElement extends ContainerElement {

	Hashtable style;

	RGBColor fill;

	RGBColor stroke;

	VMLShadow shadow;

	String strokeWeight;

	float opacity = 1;

	String startArrow;

	String endArrow;

	VMLElement(Attributes attr) {
		this.style = parseStyle(attr.getValue("style"));
		String f = attr.getValue("filled");
		if ((f == null || f.startsWith("t")) && isDrawable()) {
			this.fill = parseColor(attr.getValue("fillcolor"));
			if (this.fill == null)
				this.fill = new RGBColor(0xFFFFFF); // default value: white
		}
		f = attr.getValue("stroked");
		if ((f == null || f.startsWith("t")) && isDrawable()) {
			this.stroke = parseColor(attr.getValue("strokecolor"));
			if (this.stroke == null)
				this.stroke = new RGBColor(0); // default value: black
			this.strokeWeight = attr.getValue("strokeweight");
			if (this.strokeWeight == null)
				this.strokeWeight = "0.75pt"; // default value
		}
		opacity = parseVMLFloat(attr.getValue("opacity"), 1);
	}

	public float getOpacity() {
		return opacity;
	}
	
	public VMLShadow getShadow() {
		return shadow;
	}
	
	protected boolean isDrawable() {
		return true;
	}

	public Hashtable getStyle() {
		return style;
	}

	public RGBColor getFill() {
		return fill;
	}

	public RGBColor getStroke() {
		return stroke;
	}

	public String getStrokeWeight() {
		return strokeWeight;
	}

	static float parseVMLFloat(String f, float def) {
		if (f != null)
			if (f.endsWith("f")) {
				return Integer.parseInt(f.substring(0, f.length() - 1)) / 65536.0f;
			} else {
				return Float.parseFloat(f);
			}
		return def;
	}

	static Hashtable parseStyle(String style) {
		if (style == null)
			return null;
		Hashtable result = new Hashtable();
		StringTokenizer tok = new StringTokenizer(style, ";");
		while (tok.hasMoreTokens()) {
			String t = tok.nextToken();
			int i = t.indexOf(':');
			if (i > 0) {
				String prop = t.substring(0, i).trim();
				String val = t.substring(i + 1).trim();
				result.put(prop, val);
			}
		}
		return result;
	}

	static RGBColor parseColor(String color) {
		if (color == null || !color.startsWith("#") || color.length() < 7)
			return null;
		try {
			int ival = Integer.parseInt(color.substring(1, 7), 16);
			return new RGBColor(ival);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public TXBXContentElement getTextBoxContentElement() {
		Iterator it = content();
		while (it.hasNext()) {
			Object child = it.next();
			if (child instanceof VMLTextboxElement) {
				Iterator cit = ((VMLTextboxElement) child).content();
				while (cit.hasNext()) {
					child = cit.next();
					if (child instanceof TXBXContentElement)
						return (TXBXContentElement) child;
				}
			}
		}
		return null;
	}

	public float[] getTextBox() {
		return null;
	}

	public static float getNumberValue(Hashtable style, String propName, float def) {
		Object propVal = style.get(propName);
		if (propVal != null) {
			try {
				return Float.parseFloat(propVal.toString());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return def;
	}

}
