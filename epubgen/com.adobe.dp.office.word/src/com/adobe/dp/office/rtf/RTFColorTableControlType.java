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

import java.util.Vector;

public class RTFColorTableControlType extends RTFControlType {

	public RTFColorTableControlType(String name) {
		super(name);
	}

	public boolean parseTimeGroupExec(RTFGroup group, RTFDocumentParser parser) {
		Vector colors = new Vector();
		RTFColor color = new RTFColor();
		for( int i = 0 ; i < group.content.length ; i++ ) {
			Object c = group.content[i];
			if( c instanceof String && ((String)c).indexOf(";") >= 0 ) {
				colors.add(color);
				color = new RTFColor();
			} else if( c instanceof RTFControl ) {
				RTFControl cc = (RTFControl)c;
				String name = cc.getName();
				int param = cc.getParam();
				if( name.equals("red") ) {
					color.red = param;
				} else if( name.equals("green") ) {
					color.green = param;					
				} else if( name.equals("blue") ) {
					color.blue = param;					
				}
			}
		}
		RTFColor[] colorTable = new RTFColor[colors.size()];
		colors.copyInto(colorTable);
		parser.setColorTable(colorTable);
		return true;
	}

}
