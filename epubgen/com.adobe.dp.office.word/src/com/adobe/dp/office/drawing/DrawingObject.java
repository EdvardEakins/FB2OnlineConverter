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

package com.adobe.dp.office.drawing;

import org.xml.sax.Attributes;

import com.adobe.dp.office.embedded.EmbeddedObject;

public class DrawingObject implements EmbeddedObject {

	public static final String aNS = "http://schemas.openxmlformats.org/drawingml/2006/main";

	public static final String picNS = "http://schemas.openxmlformats.org/drawingml/2006/picture";

	static final String wpNS = "http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing";

	static final String rNS = "http://schemas.openxmlformats.org/officeDocument/2006/relationships";

	PictureData pictureData = new PictureData();

	public DrawingObject() {
	}

	public PictureData getPictureData() {
		return pictureData;
	}

	public EmbeddedObject newChild(Object context, String ns, String name, Attributes attrs) {
		if (ns.equals(DrawingObject.aNS) && name.equals("blip")) {
			pictureData.setResourceId(attrs.getValue(rNS, "embed"));
		}
		if (ns.equals(wpNS) && name.equals("extent")) {
			String cx = attrs.getValue("cx");
			String cy = attrs.getValue("cy");
			if (cx != null)
				try {
					pictureData.width = Double.parseDouble(cx) * (72.0 / (2.54 * 360000.0));
				} catch (NumberFormatException e) {
					e.printStackTrace();
				}
			if (cy != null)
				try {
					pictureData.height = Double.parseDouble(cy) * (72.0 / (2.54 * 360000.0));
				} catch (NumberFormatException e) {
					e.printStackTrace();
				}
		}
		return this;
	}

	public void finishChild(Object context, EmbeddedObject child) {
	}

	public void finish(Object context) {
	}
}
