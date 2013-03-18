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

import com.adobe.dp.office.types.RGBColor;

public class VMLShadow {

	RGBColor color;
	float offsetX = 5;
	float offsetY = 5;
	float opacity;
	
	VMLShadow( Attributes attr ) {
		color = VMLElement.parseColor(attr.getValue("color"));
		if( color == null )
			color = new RGBColor(0);
		String offset = attr.getValue("offset");
		if( offset != null ) {
			StringTokenizer tok = new StringTokenizer(offset, ",");
			switch( tok.countTokens() ) {
			case 2:
				offsetX = VMLPathConverter.readCSSLength(tok.nextToken(), 5);
				offsetY = VMLPathConverter.readCSSLength(tok.nextToken(), 5);
				break;
			case 1:
				offsetX = VMLPathConverter.readCSSLength(tok.nextToken(), 5);
				offsetY = offsetX;
				break;
			}
		}
		opacity = VMLElement.parseVMLFloat(attr.getValue("opacity"), 1);		
	}

	public RGBColor getColor() {
		return color;
	}

	public float getOffsetX() {
		return offsetX;
	}

	public float getOffsetY() {
		return offsetY;
	}

	public float getOpacity() {
		return opacity;
	}
	
	
}
