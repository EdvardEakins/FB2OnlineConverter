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
import java.io.StringWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public abstract class BaseRule {

	TreeMap properties;

	BaseRule() {
		properties = new TreeMap();
	}

	BaseRule(TreeMap props) {
		this.properties = props;
	}

	protected BaseRule(BaseRule other) {
		properties = new TreeMap();
		Iterator it = other.properties.entrySet().iterator();
		while( it.hasNext() ) {
			Map.Entry entry = (Map.Entry)it.next();
			// not cloning CSS values
			properties.put(entry.getKey(), entry.getValue());
		}
		
	}
	
	public boolean isEmpty() {
		return properties.isEmpty();
	}

	public CSSValue get(String property) {
		return (CSSValue)properties.get(property);
	}

	public void set(String property, CSSValue value) {
		if (value == null)
			properties.remove(property);
		else
			properties.put(property, value);
	}

	public Iterator properties() {
		return properties.keySet().iterator();
	}

	public abstract void serialize(PrintWriter out);

	public void serializeProperties(PrintWriter out, boolean newlines) {
		Iterator entries = properties.entrySet().iterator();
		while (entries.hasNext()) {
			Map.Entry entry = (Map.Entry) entries.next();
			if (newlines)
				out.print('\t');
			out.print(entry.getKey());
			out.print(": ");
			((CSSValue)entry.getValue()).serialize(out);
			out.print(";");
			if (newlines)
				out.println();
			else
				out.print(' ');
		}
	}

	public String toString() {
		StringWriter out = new StringWriter();
		PrintWriter pw = new PrintWriter(out);
		serialize(pw);
		pw.flush();
		return out.toString();
	}
}
