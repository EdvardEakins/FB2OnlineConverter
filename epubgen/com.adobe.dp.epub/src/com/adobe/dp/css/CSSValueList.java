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

package com.adobe.dp.css;

import java.io.PrintWriter;

public class CSSValueList extends CSSValue {

	char separator;

	CSSValue[] values;

	public CSSValueList(char sep, CSSValue[] vals) {
		this.separator = sep;
		this.values = vals;
	}

	public char getSeparator() {
		return separator;
	}

	public int length() {
		return values.length;
	}

	public CSSValue item(int i) {
		return values[i];
	}

	public void serialize(PrintWriter out) {
		String sep = "";
		for (int i = 0; i < values.length; i++) {
			out.print(sep);
			values[i].serialize(out);
			if (separator == ' ')
				sep = " ";
			else
				sep = separator + " ";
		}
	}

	public boolean equals(Object other) {
		if (this == other)
			return true;
		if (other.getClass() != getClass())
			return false;
		CSSValueList o = (CSSValueList) other;
		if (o.separator != separator || o.values.length != values.length)
			return false;
		for (int i = 0; i < values.length; i++) {
			if (!values[i].equals(o.values[i]))
				return false;
		}
		return true;
	}

	public int hashCode() {
		int code = separator;
		for (int i = 0; i < values.length; i++) {
			code += (i + 1) * values[i].hashCode();
		}
		return code;
	}

	public static int valueCount(Object value, char op) {
		if (value instanceof CSSValueList) {
			CSSValueList vl = (CSSValueList) value;
			if (vl.separator == op)
				return vl.values.length;
		}
		return 1;
	}

	public static Object valueAt(Object value, int index, char op) {
		if (value instanceof CSSValueList) {
			CSSValueList vl = (CSSValueList) value;
			if (vl.separator == op)
				return vl.values[index];
		}
		if (index == 0)
			return value;
		throw new ArrayIndexOutOfBoundsException(index);
	}
}
