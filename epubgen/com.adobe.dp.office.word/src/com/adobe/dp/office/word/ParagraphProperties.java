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

package com.adobe.dp.office.word;

import java.util.Iterator;

public class ParagraphProperties extends BaseProperties {

	RunProperties runProperties;

	NumberingProperties numberingProperties;

	Style paragraphStyle;

	NumberingLabel numberingLabel;

	NumberingProperties getNumberingProperties() {
		ParagraphProperties pp = this;
		Style ps = paragraphStyle;
		do {
			if (pp != null && pp.numberingProperties != null)
				return pp.numberingProperties;
			if (ps == null)
				break;
			pp = ps.paragraphProperties;
			ps = ps.getParent();
		} while (pp != null || ps != null);
		return null;
	}

	public NumberingLabel getNumberingLabel() {
		return numberingLabel;
	}

	public RunProperties getRunProperties() {
		return runProperties;
	}

	public Style getParagraphStyle() {
		return paragraphStyle;
	}

	public int hashCode() {
		return super.hashCode() + (runProperties == null ? 0 : runProperties.hashCode())
				+ (numberingProperties == null ? 0 : numberingProperties.hashCode())
				+ (paragraphStyle == null ? 0 : paragraphStyle.hashCode());
	}

	public boolean equals(Object other) {
		if (!super.equals(other))
			return false;
		ParagraphProperties pp = (ParagraphProperties) other;
		if (pp.paragraphStyle != paragraphStyle)
			return false;
		if (numberingProperties == null) {
			if (pp.numberingProperties != null)
				return false;
		} else {
			if (!numberingProperties.equals(pp.numberingProperties))
				return false;
		}
		if (runProperties == null) {
			if (pp.runProperties != null)
				return false;
		} else {
			if (!runProperties.equals(pp.runProperties))
				return false;
		}
		return true;
	}

	public boolean sameStyle(ParagraphProperties other) {
		if (other.paragraphStyle != paragraphStyle)
			return false;
		if (!isEmpty()) {
			Iterator it = properties();
			while (it.hasNext()) {
				String prop = (String) it.next();
				Object v1 = get(prop);
				Object v2 = other.get(prop);
				if (v1 != v2 && (v1 == null || v2 == null || !v1.equals(v2)))
					return false;
			}
		}
		if (!other.isEmpty()) {
			Iterator it = other.properties();
			while (it.hasNext()) {
				String prop = (String) it.next();
				Object v1 = get(prop);
				if (v1 == null)
					return false;
			}
		}
		return true;
	}

	public Object getWithInheritance(String prop) {
		ParagraphProperties p = this;
		Style style = paragraphStyle;
		while (true) {
			Object val = p.get(prop);
			if (val != null)
				return val;
			do {
				if (style == null)
					return null;
				p = style.paragraphProperties;
				style = style.getParent();
			} while (p == null);
		}
	}

}
