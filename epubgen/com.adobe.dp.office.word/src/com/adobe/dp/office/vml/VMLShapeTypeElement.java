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

import java.util.StringTokenizer;

import org.xml.sax.Attributes;

public class VMLShapeTypeElement extends VMLElement {

	VMLCoordPair size;

	VMLCoordPair origin;

	VMLCoordPair limo;

	String id;

	VMLPathSegment[] path;

	VMLFormulasElement formulas;

	int[] adj;

	Object[] textbox;

	boolean strokeok = true;

	boolean fillok = true;

	VMLShapeTypeElement(Attributes attr) {
		super(attr);
		this.id = attr.getValue("id");
		this.origin = VMLCoordPair.parse(attr.getValue("coordorigin"), 0, 0);
		this.size = VMLCoordPair.parse(attr.getValue("coordsize"), 1000, 1000);
		this.limo = VMLCoordPair.parse(attr.getValue("limo"));
		this.path = VMLPathSegment.parse(attr.getValue("path"));
		this.adj = parseAdj(attr.getValue("adj"));
	}

	void setTextBox(String textbox) {
		StringTokenizer tok = new StringTokenizer(textbox, ", ");
		int n = tok.countTokens();
		if (n != 4)
			return;
		Object[] tb = new Object[4];
		for (int i = 0; i < 4; i++) {
			String str = tok.nextToken();
			if (str.startsWith("@") || str.startsWith("#")) {
				int index = Integer.parseInt(str.substring(1));
				tb[i] = new VMLCallout(str.charAt(0), index);
			} else {
				tb[i] = new Integer(Integer.parseInt(str));
			}
		}
		this.textbox = tb;
	}
	
	static int[] parseAdj(String adjs) {
		if (adjs == null)
			return null;
		StringTokenizer tok = new StringTokenizer(adjs, ", ");
		int n = tok.countTokens();
		int[] adj = new int[n];
		for (int i = 0; i < n; i++) {
			adj[i] = Integer.parseInt(tok.nextToken());
		}
		return adj;
	}

	public VMLFormulasElement getFormulas() {
		return formulas;
	}

	public void setFormulas(VMLFormulasElement formulas) {
		this.formulas = formulas;
	}

}
