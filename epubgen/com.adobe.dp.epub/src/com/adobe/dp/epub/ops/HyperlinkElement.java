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

package com.adobe.dp.epub.ops;

import com.adobe.dp.xml.util.SMapImpl;


public class HyperlinkElement extends HTMLElement {

	String href;

	XRef xref;
	
	public HyperlinkElement(OPSDocument document, String name) {
		super(document, name);
	}

	Element cloneElementShallow(OPSDocument newDoc) {
		HyperlinkElement e = new HyperlinkElement(newDoc, getElementName());
		e.className = className;
		e.href = href;
		e.xref = xref;
		return e;
	}
	
	public String getExternalHRef() {
		return href;
	}

	public void setExternalHRef(String href) {
		xref = null;
		this.href = href;
	}

	public XRef getXRef() {
		return xref;
	}

	public void setXRef(XRef xref) {
		href = null;
		this.xref = xref;
	}	
	
	SMapImpl getAttributes() {
		SMapImpl attrs = super.getAttributes();
		String href = this.href;
		if( xref != null )
			href = xref.makeReference(this.document.resource);
		if( href != null )
			attrs.put(null, "href", href);
		return attrs;
	}
}
