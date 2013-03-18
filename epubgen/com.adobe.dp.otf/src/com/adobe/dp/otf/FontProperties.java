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

package com.adobe.dp.otf;

public class FontProperties implements FontPropertyConstants, Comparable {

	private String familyName;

	private int weight;

	private int style;

	public FontProperties(String familyName, int weight, int style) {
		this.familyName = familyName;
		this.weight = weight;
		this.style = style;
	}

	public int hashCode() {
		return familyName.hashCode() + weight + style;
	}

	public String getFamilyName() {
		return familyName;
	}

	public int getStyle() {
		return style;
	}

	public String getStyleString() {
		if (style == STYLE_ITALIC)
			return "Italic";
		else if (style == STYLE_OBLIQUE)
			return "Oblique";
		return "Regular";
	}

	public int getWeight() {
		return weight;
	}

	public String getWeightString() {
		switch (weight) {
		case 200:
		case 300:
			return "Light";
		case 400:
			return "Normal";
		case 500:
			return "Medium";
		case 600:
			return "Semibold";
		case 700:
		case 800:
			return "Bold";
		case 900:
			return "Black";
		}
		return Integer.toString(weight);
	}

	public boolean equals(Object other) {
		if (other.getClass() != getClass())
			return false;
		FontProperties o = (FontProperties) other;
		return o.familyName.equals(familyName) && o.weight == weight
				&& o.style == style;
	}

	public String toString() {
		String styleStr = getStyleString();
		return familyName + ":" + weight + ":" + styleStr;
	}

	public int compareTo(Object o) {
		FontProperties f = (FontProperties) o;
		int c = getFamilyName().compareTo(f.getFamilyName());
		if (c != 0)
			return c;
		c = getWeight() - f.getWeight();
		if (c != 0)
			return c;
		return getStyle() - f.getStyle();
	}
}
