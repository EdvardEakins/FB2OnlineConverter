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

import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;

import com.adobe.dp.css.CSSStylesheet;
import com.adobe.dp.css.CascadeEngine;

public class FB2Document {

	FB2TitleInfo titleInfo;

	FB2TitleInfo srcTitleInfo;

	FB2DocumentInfo documentInfo;

	FB2PublishInfo publishInfo;

	CSSStylesheet[] stylesheets;

	FB2Section[] bodySections;

	Hashtable binaryResources = new Hashtable();

	Hashtable idMap = new Hashtable();

	public static final String fb2NS = "http://www.gribuser.ru/xml/fictionbook/2.0";
	
	public FB2Document(InputStream in) throws IOException, FB2FormatException {
		FB2DocumentParser parser = new FB2DocumentParser(this);
		parser.parse(in);
	}

	public void applyStyles(CascadeEngine cascadeEngine) {
		for( int i = 0 ; i < bodySections.length ; i++ )
			bodySections[i].applyStyles(cascadeEngine);
	}
	
	public FB2Section[] getBodySections() {
		return bodySections;
	}

	public FB2Binary getBinaryResource(String name) {
		return (FB2Binary) binaryResources.get(name);
	}

	public CSSStylesheet[] getStylesheets() {
		return stylesheets;
	}

	public FB2DocumentInfo getDocumentInfo() {
		return documentInfo;
	}

	public FB2PublishInfo getPublishInfo() {
		return publishInfo;
	}

	public FB2TitleInfo getSrcTitleInfo() {
		return srcTitleInfo;
	}

	public FB2TitleInfo getTitleInfo() {
		return titleInfo;
	}

}
