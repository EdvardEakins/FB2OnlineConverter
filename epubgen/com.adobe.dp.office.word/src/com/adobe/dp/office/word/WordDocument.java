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

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

public class WordDocument {

	Hashtable stylesById;

	BodyElement body;
	
	BodyElement footnotes;

	Style docDefaultParagraphStyle;

	Style docDefaultRunStyle;

	Style defaultParagraphStyle;

	Style defaultRunStyle;

	Vector metadata = new Vector();
	
	// Integer(abstractNumId) -> AbstractNumberingDefinition
	Hashtable abstractNumberingDefinitions = new Hashtable();
	
	// Integer(ilvl) -> NumberingDefinitionInstance
	Hashtable numberingDefinitions = new Hashtable();
	
	static Hashtable mediaTypeBySuffix;
	
	static {
		mediaTypeBySuffix = new Hashtable();
		mediaTypeBySuffix.put("jpeg", "image/jpeg");
		mediaTypeBySuffix.put("wmf", "image/x-wmf");
		mediaTypeBySuffix.put("png", "image/png");
		mediaTypeBySuffix.put("gif", "image/gif");
	}

	WordDocument() {
	}

	public WordDocument(File file) throws IOException {
		WordDocumentParser parser = new WordDocumentParser(file);
		parser.doc = this;
		parser.parseInternal();
	}

	public String getResourceMediaType(String name) {
		int index = name.lastIndexOf('.');
		if (index > 0) {
			String suffix = name.substring(index + 1);
			String type = (String) mediaTypeBySuffix.get(suffix);
			if (type != null)
				return type;
		}
		return "application/octet-stream";
	}

	public NumberingDefinitionInstance getNumberingDefinition( Integer numId ) {
		return (NumberingDefinitionInstance)numberingDefinitions.get(numId);
	}
	
	public BodyElement getBody() {
		return body;
	}

	public BodyElement getFootnotes() {
		return footnotes;
	}
	
	public Iterator metadata() {
		return metadata.iterator();
	}

	public Style getStyleById(String styleId) {
		return (Style) stylesById.get(styleId);
	}

	public Style getDocumentDefaultParagraphStyle() {
		return docDefaultParagraphStyle;
	}

	public Style getDocumentDefaultRunStyle() {
		return docDefaultRunStyle;
	}
	
	public Style getDefaultParagraphStyle() {
		return defaultParagraphStyle;
	}

	public Style getDefaultRunStyle() {
		return defaultRunStyle;
	}
}
