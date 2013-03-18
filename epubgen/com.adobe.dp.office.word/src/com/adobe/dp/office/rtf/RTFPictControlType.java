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

import java.io.ByteArrayOutputStream;

public class RTFPictControlType extends RTFControlType {

	public RTFPictControlType(String name) {
		super(name);
	}

	public boolean parseTimeGroupExec(RTFGroup group, RTFDocumentParser parser) {
		Object[] content = group.getContent();
		if( content.length == 0 )
			return true; // ignore
		Object last = content[content.length-1];
		if( !(last instanceof String) )
			return true; // ignore
		content[content.length-1] = decodeHex((String)last);
		return false; // keep
	}

	private byte[] decodeHex(String sb) {
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		int len = sb.length();
		int acc = 0;
		boolean second = false;
		for (int i = 0; i < len; i++) {
			char c = sb.charAt(i);
			if ('0' <= c && c <= '9')
				acc = (acc << 4) | (c - '0');
			else if ('a' <= c && c <= 'f')
				acc = (acc << 4) | (c - ('a' - 10));
			else if ('A' <= c && c <= 'F')
				acc = (acc << 4) | (c - ('A' - 10));
			else
				continue;
			if (second) {
				buf.write((byte) acc);
				acc = 0;
			}
			second = !second;
		}
		return buf.toByteArray();
	}
	
}
