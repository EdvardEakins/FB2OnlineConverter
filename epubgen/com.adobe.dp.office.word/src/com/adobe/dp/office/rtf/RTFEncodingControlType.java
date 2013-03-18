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

public class RTFEncodingControlType extends RTFControlType {

	private String encoding;

	public RTFEncodingControlType(String name, String encoding) {
		super(name);
		this.encoding = encoding;
	}

	public static String getEncoding(int pg) {
		String encoding;
		switch (pg) {
		case 1033 : // US English
		case 4105 : // Canadian English
		case 1024 : // default
			return "Cp1252";
		default:
			encoding = "Cp" + pg;
			break;
		}
		return encoding;
	}

	public static String getCharsetEncoding(int cs) {
		String encoding;
		switch (cs) {
		case 161:
			encoding = "Cp1253"; // Greek
			break;
		case 162:
			encoding = "Cp1254"; // Turkish
			break;
		case 163:
			encoding = "Cp1258"; // Vietnamese
			break;
		case 177:
			encoding = "Cp1255"; // Hebrew
			break;
		case 178:
			encoding = "Cp1256"; // Arabic
			break;
		case 186:
			encoding = "Cp1257"; // Baltic
			break;
		case 204:
			encoding = "Cp1251"; // Russian
			break;
		case 222:
			encoding = "Cp874"; // Thai
			break;
		case 238:
			encoding = "Cp1250"; // Eastern European
			break;
		case 254:
			encoding = "Cp437"; // PC 437
			break;
		default:
			// unknown
			encoding = null;
			break;
		}
		return encoding;
	}

	public boolean parseTimeExec(RTFControl ctrl, RTFDocumentParser parser) {
		String encoding;
		if (this.encoding != null) {
			encoding = this.encoding;
		} else {
			encoding = getEncoding(ctrl.getParam());
		}
		parser.setEncoding(encoding);
		return true;
	}

}
