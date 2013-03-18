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

package com.adobe.dp.office.conv;

import java.io.PrintWriter;
import java.util.Hashtable;
import java.util.Iterator;

import com.adobe.dp.epub.opf.OPSResource;
import com.adobe.dp.epub.opf.Publication;
import com.adobe.dp.epub.opf.StyleResource;
import com.adobe.dp.epub.ops.OPSDocument;
import com.adobe.dp.epub.ops.SVGElement;
import com.adobe.dp.office.types.RGBColor;
import com.adobe.dp.office.vml.VMLCoordPair;
import com.adobe.dp.office.vml.VMLElement;
import com.adobe.dp.office.vml.VMLGroupElement;
import com.adobe.dp.office.vml.VMLLineElement;
import com.adobe.dp.office.vml.VMLOvalElement;
import com.adobe.dp.office.vml.VMLPathConverter;
import com.adobe.dp.office.vml.VMLRectElement;
import com.adobe.dp.office.vml.VMLShadow;
import com.adobe.dp.office.vml.VMLShapeElement;
import com.adobe.dp.office.word.TXBXContentElement;

public class VMLConverter {

	private Publication epub;

	private OPSDocument chapter;
	
	private OPSResource resource;
	
	private WordMLConverter wordConverter;

	private StyleConverter styleConverter;

	private boolean embedded;

	PrintWriter log;
	
	VMLConverter(WordMLConverter wordConverter, boolean embedded) {
		this.wordConverter = wordConverter;
		this.log = wordConverter.log;
		this.embedded = embedded;
		if (!embedded) {
			epub = wordConverter.getPublication();
			styleConverter = new StyleConverter(true);
		}
	}

	void convertVML(OPSResource resource, SVGElement svg, VMLGroupElement group) {
		Hashtable style = group.getStyle();
		if (style == null)
			return;
		this.resource = resource;
		this.chapter = resource.getDocument();
		String widthStr = (String) style.get("width");
		String heightStr = (String) style.get("height");
		VMLCoordPair origin = group.getOrigin();
		VMLCoordPair size = group.getSize();

		float widthPt = VMLPathConverter.readCSSLength(widthStr, 100);
		float heightPt = VMLPathConverter.readCSSLength(heightStr, 100);

		if (!embedded) {
			StyleResource global = (StyleResource) epub.getResourceByName("OPS/global.css");
			chapter.addStyleResource(global);
			svg.setAttribute("width", Float.toString(widthPt));
			svg.setAttribute("height", Float.toString(heightPt));
		}

		float scaleX = size.x / widthPt;
		float scaleY = size.y / heightPt;
		svg.setAttribute("viewBox", origin.x + " " + origin.y + " " + size.x + " " + size.y);
		convertVMLChildren(svg, group, scaleX, scaleY);
	}

	private void convertVMLChildren(SVGElement svg, VMLElement vml, float scaleX, float scaleY) {
		Iterator it = vml.content();
		while (it.hasNext()) {
			Object child = it.next();
			if (child instanceof VMLElement) {
				convertVMLChild(chapter, svg, (VMLElement) child, scaleX, scaleY);
			}
		}
	}

	private void convertVMLChild(OPSDocument chapter, SVGElement parentSVG, VMLElement vml, float scaleX, float scaleY) {
		try {
			SVGElement childSVG = null;
			Hashtable style = vml.getStyle();
			if (style == null)
				return;
			String rotationStr = (String) style.get("rotation");
			float rotation = 0;
			if (rotationStr != null) {
				try {
					if (rotationStr.endsWith("fd")) {
						rotation = Float.parseFloat(rotationStr.substring(0, rotationStr.length() - 2)) / 0x10000;
					} else {
						rotation = Float.parseFloat(rotationStr);
					}
				} catch (Exception e) {
					e.printStackTrace(log);
				}
			}
			float top = VMLElement.getNumberValue(style, "top", 0);
			float left = VMLElement.getNumberValue(style, "left", 0);
			float width = VMLElement.getNumberValue(style, "width", 0);
			float height = VMLElement.getNumberValue(style, "height", 0);
			float cx = left + width / 2;
			float cy = top + height / 2;
			String flip = (String) style.get("flip");
			float[] textbox = null;
			if (vml instanceof VMLGroupElement) {
				// not supported
			} else if (vml instanceof VMLShapeElement) {
				childSVG = chapter.createSVGElement("path");
				VMLPathConverter conv = new VMLPathConverter((VMLShapeElement) vml);
				conv.setOuterSize(width, height);
				conv.setScale(scaleX, scaleY);
				conv.readFormulas();
				String path = conv.getSVGPath();
				childSVG.setAttribute("d", path);
				textbox = conv.getTextBox();
			} else if (vml instanceof VMLRectElement) {
				if (width > 0 && height > 0) {
					childSVG = chapter.createSVGElement("rect");
					childSVG.setAttribute("x", Float.toString(-width / 2));
					childSVG.setAttribute("y", Float.toString(-height / 2));
					childSVG.setAttribute("width", Float.toString(width));
					childSVG.setAttribute("height", Float.toString(height));
				}
			} else if (vml instanceof VMLOvalElement) {
				if (width > 0 && height > 0) {
					childSVG = chapter.createSVGElement("ellipse");
					childSVG.setAttribute("rx", Float.toString(width / 2));
					childSVG.setAttribute("ry", Float.toString(height / 2));
				}
			} else if (vml instanceof VMLLineElement) {
				VMLCoordPair from = ((VMLLineElement) vml).getFrom();
				VMLCoordPair to = ((VMLLineElement) vml).getTo();
				if (from != null && to != null) {
					childSVG = chapter.createSVGElement("line");
					childSVG.setAttribute("x1", Float.toString(from.x));
					childSVG.setAttribute("y1", Float.toString(from.y));
					childSVG.setAttribute("x2", Float.toString(to.x));
					childSVG.setAttribute("y2", Float.toString(to.y));
				}
			}
			if (childSVG != null) {
				StringBuffer transform = new StringBuffer();
				boolean flipX = flip != null && flip.indexOf('x') >= 0;
				boolean flipY = flip != null && flip.indexOf('y') >= 0;
				if (cx != 0 || cy != 0)
					transform.append("translate(" + cx + "," + cy + ")");
				if (rotation != 0)
					transform.append("rotate(" + rotation + ")");
				if (flipX || flipY)
					transform.append("scale(" + (flipX ? -1 : 1) + " " + (flipY ? -1 : 1) + ")");
				VMLShadow shadow = vml.getShadow();
				if (transform.length() > 0)
					childSVG.setAttribute("transform", transform.toString());
				RGBColor fill = vml.getFill();
				childSVG.setAttribute("fill", (fill != null ? fill.toCSSValue().toCSSString() : "none"));
				RGBColor stroke = vml.getStroke();
				if (stroke != null) {
					childSVG.setAttribute("stroke", stroke.toCSSValue().toCSSString());
					String sws = vml.getStrokeWeight();
					if (sws != null) {
						float sw = VMLPathConverter.readCSSLength(sws, 0);
						if (sw > 0)
							childSVG.setAttribute("stroke-width", Float.toString(scaleX * sw));
					}
				}
				if (shadow != null) {
					SVGElement svgShadow = (SVGElement)childSVG.cloneElementShallow();
					String shadowOffset = "translate(" + scaleX * shadow.getOffsetX() + "," + scaleY
							* shadow.getOffsetY() + ")";
					svgShadow.setAttribute("transform", shadowOffset + transform);
					svgShadow.setAttribute("fill", shadow.getColor().toCSSValue());
					if( stroke != null )
						svgShadow.setAttribute("stroke", shadow.getColor().toCSSValue());						
					if (shadow.getOpacity() != 1)
						svgShadow.setAttribute("opacity", Float.toString(shadow.getOpacity()));
					parentSVG.add(svgShadow);
				}
				float opacity = vml.getOpacity();
				if (opacity != 1)
					childSVG.setAttribute("opacity", Float.toString(opacity));
				parentSVG.add(childSVG);
				TXBXContentElement textboxContent = vml.getTextBoxContentElement();
				if (textboxContent != null) {
					if (textbox == null)
						textbox = vml.getTextBox();
					if (textbox != null) {
						SVGElement foreignObject = chapter.createSVGElement("foreignObject");
						float scaleAdj = Math.round(100 * scaleY) / 100.0f;
						foreignObject.setAttribute("transform", transform + "scale(" + scaleAdj + " " + scaleAdj + ")");
						foreignObject.setAttribute("x", Float.toString(textbox[0] / scaleY));
						foreignObject.setAttribute("y", Float.toString(textbox[1] / scaleY));
						foreignObject.setAttribute("width", Float.toString((textbox[2] - textbox[0]) / scaleY));
						foreignObject.setAttribute("height", Float.toString((textbox[3] - textbox[1]) / scaleY));
						parentSVG.add(foreignObject);
						WordMLConverter wordConv;
						if (embedded)
							wordConv = new WordMLConverter(wordConverter, resource);
						else
							wordConv = new WordMLConverter(wordConverter, styleConverter);
						int depth = wordConv.pushOPSContainer(foreignObject);
						wordConv.appendConvertedElement(textboxContent, null, null, 1, 1, null);
						wordConv.restoreOPSContainer(depth);
					}
				}
			}
		} catch (Exception e) {
			// VML is very incomplete, don't fail the whole document
			e.printStackTrace(log);
		}
	}
}
