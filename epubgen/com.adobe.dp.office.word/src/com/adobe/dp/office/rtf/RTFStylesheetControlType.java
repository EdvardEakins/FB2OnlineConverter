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

public class RTFStylesheetControlType extends RTFControlType {

	RTFStylesheetControlType(String name) {
		super(name);
	}

	public void processStyle(RTFGroup group, RTFDocumentParser parser) {
		Object[] content = group.content;
		int len = content.length;
		int index = -1;
		int charIndex = -1;
		int parentIndex = 222;
		RTFStyle style = new RTFStyle();
		for (int i = 0; i < len; i++) {
			Object e = content[i];
			if (e instanceof RTFControl) {
				RTFControl f = (RTFControl) e;
				RTFControlType t = f.getType();
				if (!t.formattingExec(f, style)) {
					String fname = f.getName();
					if (fname.equals("s")) {
						index = f.getParam();
					} else if (fname.equals("cs")) {
						charIndex = f.getParam();
					} else if (fname.equals("sbasedon")) {
						parentIndex = f.getParam();
					}
				}
			}
		}
		if (index >= 0) {
			if (parentIndex != 222)
				style.parent = parser.getParagraphStyle(parentIndex);
			parser.addParagraphStyle(index, style);
		} else if (charIndex >= 0) {
			if (parentIndex != 222)
				style.parent = parser.getCharacterStyle(parentIndex);
			parser.addCharacterStyle(charIndex, style);
		}
	}

	public boolean parseTimeGroupExec(RTFGroup group, RTFDocumentParser parser) {
		Object[] content = group.content;
		int len = content.length;
		for (int i = 0; i < len; i++) {
			Object e = content[i];
			if (e instanceof RTFGroup)
				processStyle((RTFGroup) e, parser);
		}
		return true;
	}
}
