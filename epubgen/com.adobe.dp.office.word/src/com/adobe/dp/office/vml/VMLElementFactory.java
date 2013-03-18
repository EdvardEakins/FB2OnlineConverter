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

import java.util.Hashtable;

import org.xml.sax.Attributes;

import com.adobe.dp.office.word.Element;

public class VMLElementFactory {

	public static Element createVMLElement(VMLElement parent, Hashtable vmldefs, String localName, Attributes attr) {
		if (localName.equals("group"))
			return new VMLGroupElement(attr);
		if (localName.equals("line"))
			return new VMLLineElement(attr);
		if (localName.equals("oval"))
			return new VMLOvalElement(attr);
		if (localName.equals("rect"))
			return new VMLRectElement(attr);
		if (localName.equals("shape")) {
			VMLShapeElement e = new VMLShapeElement(attr);
			String ref = attr.getValue("type");
			if( ref != null && ref.startsWith("#")) {
				Object def = vmldefs.get(ref.substring(1));
				if( def instanceof VMLShapeTypeElement ) {
					e.type = (VMLShapeTypeElement)def;
				}
			}
			return e;
		}
		if (localName.equals("shapetype")) {
			VMLShapeTypeElement e = new VMLShapeTypeElement(attr);
			if( e.id != null )
				vmldefs.put(e.id, e);
			return e;
		}
		if (localName.equals("formulas"))
			return new VMLFormulasElement(attr);
		if (localName.equals("f"))
			return new VMLFElement(attr);
		if (localName.equals("textbox"))
			return new VMLTextboxElement(attr);
		if (localName.equals("path")) {
			if( parent instanceof VMLShapeTypeElement ) {
				VMLShapeTypeElement ste = (VMLShapeTypeElement)parent;
				VMLCoordPair limo = VMLCoordPair.parse(attr.getValue("limo"));
				if( limo != null )
					ste.limo = limo;
				String strokeok = attr.getValue("strokeok");
				if( strokeok != null && !strokeok.toLowerCase().startsWith("t"))
					ste.strokeok = false;
				String fillok = attr.getValue("fillok");
				if( fillok != null && !fillok.toLowerCase().startsWith("t"))
					ste.fillok = false;
				String textbox = attr.getValue("textboxrect");
				if( textbox != null )
					ste.setTextBox(textbox);
			}
		}
		else if (localName.equals("stroke")) {
			if( parent instanceof VMLElement ) {
				VMLElement e = (VMLElement)parent;
				e.endArrow = attr.getValue("endarrow");
				e.startArrow = attr.getValue("startarrow");
			}
		}
		else if (localName.equals("fill")) {
			if( parent instanceof VMLElement ) {
				//VMLElement e = (VMLElement)parent;
			}
		}
		else if (localName.equals("shadow")) {
			if( parent instanceof VMLElement ) {
				VMLElement e = (VMLElement)parent;
				e.shadow = new VMLShadow(attr);
			}
		}
		return null;
	}
}
