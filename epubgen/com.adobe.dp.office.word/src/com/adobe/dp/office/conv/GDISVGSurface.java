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

import com.adobe.dp.office.metafile.GDIBitmap;
import com.adobe.dp.office.metafile.GDIBrush;
import com.adobe.dp.office.metafile.GDIFont;
import com.adobe.dp.office.metafile.GDIMatrix;
import com.adobe.dp.office.metafile.GDIObject;
import com.adobe.dp.office.metafile.GDIPen;
import com.adobe.dp.office.metafile.GDISurface;

public class GDISVGSurface extends GDISurface {

	StringBuffer body = new StringBuffer();

	StringBuffer defs = new StringBuffer();

	boolean initialized = false;

	GDIMatrix initialTransform;

	int mappingMode;

	int[] path = new int[10];

	int pathEnd = 0;

	ResourceWriter resourceWriter;

	public GDISVGSurface(ResourceWriter resourceWriter) {
		this.resourceWriter = resourceWriter;
	}

	public void setBounds(int left, int top, int right, int bottom) {
		super.setBounds(left, top, right, bottom);
	}

	private String makeRGB(int rgb) {
		return "#" + Integer.toHexString((rgb & 0xFFFFFF) | 0xF000000).substring(1);
	}

	private void appendFillAndStrokeParam() {
		GDIPen pen = getCurrentPen();
		if (pen != null && pen.getStyle() != GDIPen.PS_NULL) {
			body.append(" stroke=\"");
			body.append(makeRGB(pen.getColor()));
			body.append('"');
			int width = pen.getWidth();
			if (width > 1) {
				body.append(" stroke-width=\"");
				body.append(width);
				body.append('"');
			}
		}
		GDIBrush brush = getCurrentBrush();
		if (brush == null || brush.getStyle() == GDIBrush.BS_NULL) {
			body.append(" fill=\"none\"");
		} else {
			body.append(" fill=\"");
			body.append(makeRGB(brush.getColor()));
			body.append('"');
			if( getPolyFillMode() == ALTERNATE ) {
				body.append(" fill-rule=\"evenodd\"");
			}
		}
	}

	private void captureHeader() {
		if (initialized)
			return;
		initialTransform = getViewportMatrix();
		initialized = true;
	}

	private void ensurePoints(int count) {
		if (path.length < pathEnd + 3 * count) {
			int[] newPath = new int[2 * pathEnd + 3 * count];
			System.arraycopy(path, 0, newPath, 0, pathEnd);
			path = newPath;
		}
	}

	private void endPathIfAny() {
		if (pathEnd == 0)
			return;
		body.append("<path d=\"");
		int i = 0;
		while(i < pathEnd) {
			int command = path[i++];
			body.append((char)command);
			switch(command) {
			case 'M' :
			case 'L' : {
				int x = path[i++];
				int y = path[i++];
				body.append(x);
				if (y >= 0)
					body.append(' ');
				body.append(y);
				break;
			}
			}
		}
		body.append('"');
		appendFillAndStrokeParam();
		body.append("/>\r\n");
		pathEnd = 0;
	}

	public void selectObject(GDIObject obj) {
		captureHeader();
		endPathIfAny();
		super.selectObject(obj);
	}

	public void extTextOut(int x, int y, String text, int flags, int[] clipRect, int[] adj) {
		captureHeader();
		endPathIfAny();
		GDIFont font = getCurrentFont();
		if (font != null) {
			int fontSize = font.getHeight();
			if (fontSize < 0)
				fontSize = -fontSize;
			int textAlign = getTextAlign();
			int color = getTextColor();
			body.append("<text x=\"");
			body.append(x);
			body.append("\" y=\"");
			body.append(y);
			switch( textAlign & 6 ) {
			case 6 : // center
				body.append("\" text-anchor=\"middle");
				break;
			case 4 : // right
				body.append("\" text-anchor=\"end");
				break;
			}
			body.append("\" fill=\"");
			body.append(makeRGB(color));
			body.append("\" font-family=\"'");
			body.append(font.getName());
			body.append("'\" font-size=\"");
			body.append(fontSize);
			body.append('"');
			int weight = font.getWeight();
			if (weight % 100 == 0) {
				body.append(" font-weight=\"");
				body.append(weight);
				body.append('"');
			}
			if (font.isItalic()) {
				body.append(" font-style=\"italic\"");
			}
			if (font.isStrikeout() || font.isUnderline()) {
				body.append(" text-decoration=\"");
				if (font.isUnderline()) {
					body.append("underline");
					if (font.isStrikeout())
						body.append(',');
				}
				if (font.isStrikeout())
					body.append("line-through");
				body.append('"');
			}
			body.append('>');
			body.append(text.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;"));
			body.append("</text>\r\n");
		}
	}

	public void lineTo(int x, int y) {
		captureHeader();
		ensurePoints(1);
		path[pathEnd++] = 'L';
		path[pathEnd++] = x;
		path[pathEnd++] = y;
	}

	public void moveTo(int x, int y) {
		captureHeader();
		ensurePoints(1);
		path[pathEnd++] = 'M';
		path[pathEnd++] = x;
		path[pathEnd++] = y;
	}

	public void closePath() {
		captureHeader();
		ensurePoints(1);
		path[pathEnd++] = 'Z';
	}

	private void playPoly(int[] points, int offset, int len) {
		ensurePoints(len / 2);
		for (int i = 0; i < len; i += 2) {
			path[pathEnd++] = (i == 0 ? 'M' : 'L');
			path[pathEnd++] = points[offset + i];
			path[pathEnd++] = points[offset + i + 1];
		}		
	}
	
	public void polygon(int[] points, int offset, int len) {
		captureHeader();
		endPathIfAny();
		playPoly(points, offset, len);
		closePath();
		endPathIfAny();
	}
	
	public void polyPolygon(int[] lens, int points[]) {
		captureHeader();
		endPathIfAny();
		int offset = 0;
		for (int i = 0; i < lens.length; i++) {
			int len = lens[i] * 2;
			playPoly(points, offset, len);
			closePath();
			offset += len;
		}
		endPathIfAny();
	}

	public void polyline(int[] points, int offset, int len) {
		captureHeader();
		endPathIfAny();
		ensurePoints(len / 2);
		for (int i = 0; i < len; i += 2) {
			path[pathEnd++] = (i == 0 ? 'M' : 'L');
			path[pathEnd++] = points[offset + i];
			path[pathEnd++] = points[offset + i + 1];
		}
		endPathIfAny();
	}

	public void rectangle(int x1, int y1, int x2, int y2) {
		captureHeader();
		endPathIfAny();
		if (x2 < x1) {
			int tmp = x2;
			x2 = x1;
			x1 = tmp;
		}
		if (y2 < y1) {
			int tmp = y2;
			y2 = y1;
			y1 = tmp;
		}
		body.append("<rect x=\"");
		body.append(x1);
		body.append("\" y=\"");
		body.append(y1);
		body.append("\" width=\"");
		body.append(x2 - x1);
		body.append("\" height=\"");
		body.append(y2 - y1);
		body.append('"');
		appendFillAndStrokeParam();
		body.append("/>\r\n");
	}

	public void ellipse(int x1, int y1, int x2, int y2) {
		captureHeader();
		endPathIfAny();
		body.append("<ellipse cx=\"");
		body.append((x1 + x2) / 2.0);
		body.append("\" cy=\"");
		body.append((y1 + y2) / 2.0);
		body.append("\" rx=\"");
		body.append(Math.abs((x2 - x1) / 2.0));
		body.append("\" ry=\"");
		body.append(Math.abs((y2 - y1) / 2.0));
		body.append('"');
		appendFillAndStrokeParam();
		body.append("/>\r\n");
	}

	public void stretchDIB(GDIBitmap bitmap, int destX, int destY, int destWidth, int destHeight, int srcX, int srcY,
			int srcWidth, int srcHeight) {
		if (resourceWriter != null) {
			try {
				StreamAndName image = resourceWriter.createResource("image", ".png", true);
				PNGWriter writer = new PNGWriter(image.stream, bitmap.getWidth(), bitmap.getHeight(), false);
				bitmap.saveAsPNG(writer);
				writer.close();
				boolean flipX = destWidth < 0;
				boolean flipY = destHeight < 0;
				int width = (flipX ? -destWidth : destWidth);
				int height = (flipY ? -destHeight : destHeight);
				body.append("<image transform=\"matrix(");
				body.append(flipX ? -1 : 1);
				body.append(" 0 0 ");
				body.append(flipY ? -1 : 1);
				body.append(" ");
				body.append(destX);
				body.append(" ");
				body.append(destY);
				body.append(")\" width=\"");
				body.append(width);
				body.append("\" height=\"");
				body.append(height);
				body.append("\" xlink:href=\"");
				body.append(image.name);
				body.append("\"/>\r\n");
			} catch (Exception err) {
				err.printStackTrace();
			}
		}
	}

	public String getSVG() {
		captureHeader();
		StringBuffer total = new StringBuffer();
		total.append("<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\"");
		int wb = getBoundsRight() - getBoundsLeft();
		int hb = getBoundsBottom() - getBoundsTop();
		double wv = wb / Math.abs(initialTransform.a);
		double hv = hb / Math.abs(initialTransform.d);
		double xv = -initialTransform.x/initialTransform.a;
		double yv = -initialTransform.y/initialTransform.d;
		boolean flipX = initialTransform.a < 0;
		boolean flipY = initialTransform.d < 0;
		if (wb != 0 && hb != 0) {
			total.append(" viewBox=\"");
			if (flipX)
				total.append('0');
			else {
				total.append(xv);
				xv = 0;
			}
			total.append(' ');
			if (flipY)
				total.append('0');
			else {
				total.append(yv / initialTransform.d);
				yv = 0;
			}
			total.append(' ');
			total.append(wv);
			total.append(' ');
			total.append(hv);
			total.append("\" width=\"");
			total.append(wb);
			total.append("\" height=\"");
			total.append(hb);
			total.append('"');
		}
		total.append(">\r\n");
		if (defs.length() > 0) {
			total.append("<defs>\r\n");
			total.append(defs);
			total.append("</defs>\r\n");
		}
		if (flipX || flipY || xv != 0 || yv != 0) {
			total.append("<g transform=\"matrix(");
			total.append(flipX ? "-1" : "1");
			total.append(" 0 0 ");
			total.append(flipY ? "-1" : "1");
			total.append(' ');
			total.append(xv);
			total.append(' ');
			total.append(yv);
			total.append(")\">\r\n");
			total.append(body);
			total.append("</g>\r\n");
		} else {
			total.append(body);
		}
		total.append("</svg>\r\n");
		return total.toString();
	}

}
