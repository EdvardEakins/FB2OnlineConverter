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

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;

public class RTFDocument {

	RTFGroup root;
	Hashtable fonts;
	Hashtable paragraphStyles;
	Hashtable characterStyles;
	RTFColor[] colorTable;
	
	RTFDocument() {
	}

	public RTFDocument(File file) throws IOException {
		RTFDocumentParser parser = new RTFDocumentParser(file);
		parser.doc = this;
		parser.parseInternal();
	}

	public RTFStyle getParagraphStyle(int index) {
		return (RTFStyle) paragraphStyles.get(new Integer(index));
	}
	
	public RTFStyle getCharacterStyle(int index) {
		return (RTFStyle) characterStyles.get(new Integer(index));
	}
	
	public RTFFont getFont(int index) {
		return (RTFFont) fonts.get(new Integer(index));
	}
	
	public RTFColor getColor(int index) {
		if( colorTable == null || index < 0 || index >= colorTable.length )
			return null;
		return colorTable[index];
	}
	
	public RTFGroup getRoot() {
		return root;
	}

	public static void main(String[] args) {
		try {
			new RTFDocument(new File("C:\\Documents and Settings\\psorotok\\My Documents\\SampleDoc.rtf"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
