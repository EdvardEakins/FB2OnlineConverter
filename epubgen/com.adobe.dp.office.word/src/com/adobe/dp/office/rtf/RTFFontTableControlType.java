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

import java.util.HashSet;

public class RTFFontTableControlType extends RTFControlType {

	static HashSet encSuffix = new HashSet();

	static {
		encSuffix.add("CE");
		encSuffix.add("Western");
		encSuffix.add("Cyr");
		encSuffix.add("Greek");
		encSuffix.add("Tur");
		encSuffix.add("Baltic");
		encSuffix.add("CE");
	}

	public RTFFontTableControlType(String name) {
		super(name);
	}

	static String stripEncSuffix(String name) {
		int sp = name.lastIndexOf(" ");
		if (sp >= 0) {
			String word = name.substring(sp + 1);
			if (encSuffix.contains(word) || (word.startsWith("(") && word.endsWith(")")))
				return name.substring(0, sp);
		}
		return name;
	}

	private void processFont(RTFGroup group, RTFDocumentParser parser) {
		Object[] content = group.content;
		int len = content.length;
		int index = -1;
		int type = RTFFont.ROMAN;
		String encoding = null;
		StringBuffer name = new StringBuffer();
		for (int i = 0; i < len; i++) {
			Object e = content[i];
			if (e instanceof RTFControl) {
				RTFControl f = (RTFControl) e;
				String fname = f.getName();
				if (fname.equals("f")) {
					index = f.getParam();
				} else if (fname.equals("froman")) {
					type = RTFFont.ROMAN;
				} else if (fname.equals("fswiss")) {
					type = RTFFont.SWISS;
				} else if (fname.equals("fmodern")) {
					type = RTFFont.MODERN;
				} else if (fname.equals("fscript")) {
					type = RTFFont.SCRIPT;
				} else if (fname.equals("ftech")) {
					type = RTFFont.TECH;
				} else if (fname.equals("cpg")) {
					encoding = RTFEncodingControlType.getEncoding(f.getParam());
				} else if (fname.equals("fcharset")) {
					encoding = RTFEncodingControlType.getCharsetEncoding(f.getParam());
				}
			} else if (e instanceof String) {
				name.append(e);
			}
		}
		if (index >= 0 && name != null) {
			int ne = name.indexOf(";");
			if (ne >= 0) {
				RTFFont font = new RTFFont();
				font.type = type;
				font.encoding = encoding;
				font.name = stripEncSuffix(name.substring(0, ne));
				System.out.println("Font name: " + font.name);
				parser.addFont(index, font);
			}
		}
	}

	public boolean parseTimeGroupExec(RTFGroup group, RTFDocumentParser parser) {
		Object[] content = group.content;
		int len = content.length;
		for (int i = 0; i < len; i++) {
			Object e = content[i];
			if (e instanceof RTFGroup)
				processFont((RTFGroup) e, parser);
		}
		return true;
	}

}
