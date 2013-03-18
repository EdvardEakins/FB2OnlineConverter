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

package com.adobe.dp.office.metafile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import com.adobe.dp.office.conv.FileResourceWriter;
import com.adobe.dp.office.conv.GDISVGSurface;

public class Test {

	class TestGDISurface extends GDISurface {

		public void setMapMode(int mode) {
			System.out.println("SetMapMode " + Integer.toHexString(mode));
		}

		public void setBkColor(int rgb) {
			System.out.println("SetBkColor " + Integer.toHexString(rgb));
		}

		public void setTextColor(int rgb) {
			System.out.println("SetTextColor " + Integer.toHexString(rgb));
		}

		public void setROP2(int mode) {
			System.out.println("SetROP2 " + Integer.toHexString(mode));
		}

		public void setPolyFillMode(int mode) {
			System.out.println("SetPolyFillMode " + Integer.toHexString(mode));
		}

		public void setBkMode(int mode) {
			System.out.println("SetBkMode " + Integer.toHexString(mode));
		}

		public void setTextAlign(int mode) {
			System.out.println("SetTextAlign " + Integer.toHexString(mode));
		}

		public void selectObject(GDIObject obj) {
			super.selectObject(obj);
			System.out.println("SelectObject " + obj);
		}

		public void deleteObject(GDIObject obj) {
			super.deleteObject(obj);
			System.out.println("DeleteObject " + obj);
		}

		public void setWindowExt(int x, int y) {
			super.setWindowExt(x, y);
			System.out.println("SetWindowExt " + x + " " + y);
		}

		public void setWindowOrg(int x, int y) {
			super.setWindowOrg(x, y);
			System.out.println("SetWindowOrg " + x + " " + y);
		}

		public void setViewportExt(int x, int y) {
			super.setViewportExt(x, y);
			System.out.println("SetViewportExt " + x + " " + y);
		}

		public void setViewportOrg(int x, int y) {
			super.setViewportOrg(x, y);
			System.out.println("SetViewportOrg " + x + " " + y);
		}

		public void moveTo(int x, int y) {
			System.out.println("MoveTo " + x + " " + y);
		}

		public void lineTo(int x, int y) {
			System.out.println("LineTo " + x + " " + y);
		}

		public void rectangle(int x1, int y1, int x2, int y2) {
			System.out.println("Rectangle " + x1 + " " + y1 + " " + x2 + " " + y2);
		}

		public void ellipse(int x1, int y1, int x2, int y2) {
			System.out.println("Ellipse " + x1 + " " + y1 + " " + x2 + " " + y2);
		}

		public void polygon(int[] points, int offset, int len) {
			System.out.println("Polygon " + len / 2 + " points");
		}

		public void polyline(int[] points, int offset, int len) {
			System.out.println("Polyline " + len / 2 + " points");
		}

		public void extTextOut(int x, int y, String text, int flags, int[] clipRect, int[] adj) {
			System.out.print("ExtTextOut " + x + " " + y + " '" + text + "' " + Integer.toHexString(flags));
			if (clipRect != null)
				System.out.print(" [clip " + clipRect[0] + " " + clipRect[1] + " " + clipRect[2] + " "
						+ clipRect[3] + "]");
			System.out.println();
		}

		public GDIPen createPenIndirect(int style, int width, int rgb) {
			GDIPen pen = super.createPenIndirect(style, width, rgb);
			System.out.println("CreatePenIndirect " + style + " " + width + " " + Integer.toHexString(rgb)
					+ " " + pen);
			return pen;
		}

		public GDIPen extCreatePen(int extStyle, int width, int rgb) {
			GDIPen pen = super.extCreatePen(extStyle, width, rgb);
			System.out.println("ExtCreatePen " + Integer.toHexString(extStyle) + " " + width + " "
					+ Integer.toHexString(rgb) + " " + pen);
			return pen;
		}

		public GDIBrush createBrushIndirect(int style, int rgb, int hatch) {
			GDIBrush brush = super.createBrushIndirect(style, rgb, hatch);
			System.out.println("CreateBrushIndirect " + style + " " + Integer.toHexString(rgb) + " " + hatch
					+ " " + brush);
			return brush;
		}

		public GDIFont createFontIndirect(int fontHeight, int width, int esc, int orientation, int weight,
				String name, boolean italic, boolean underline, boolean strikeout, int charset, int quality,
				int pitchAndFamily) {
			GDIFont font = super.createFontIndirect(fontHeight, width, esc, orientation, weight, name, italic,
					underline, strikeout, charset, quality, pitchAndFamily);
			System.out.println("CreateFontIndirect '" + name + "' sz=" + fontHeight + " w=" + weight + " i="
					+ italic + " u=" + underline + " s=" + strikeout + " ch=" + charset + " q=" + quality
					+ " p=" + pitchAndFamily + " " + font);
			return font;
		}

		public void restoreDC() {
			super.restoreDC();
			System.out.println("RestoreDC");
		}

		public void saveDC() {
			super.saveDC();
			System.out.println("SaveDC");
		}

		public void stretchDIB(GDIBitmap bitmap, int destX, int destY, int destWidth, int destHeight, int srcX,
				int srcY, int srcWidth, int srcHeight) {
			System.out.println("StretchDIB " + destX + " " + destY + " " + destWidth + " " + destHeight + " "
					+ srcX + " " + srcY + " " + srcWidth + " " + srcHeight + " " + bitmap);
		}

		public void setMiterLimit(int miterLimit) {
			System.out.println("SetMiterLimit " + miterLimit);
		}

		public void commentMetafile(int version, byte[] data, int offset, int len) {
			System.out.println("GdiComment Metafile " + version);
		}

		public void commentBeginGroup(int left, int top, int right, int bottom, String desc) {
			System.out.println("GdiComment BeginGroup[" + left + " " + top + " " + right + " " + bottom
					+ "]: '" + desc + "'");
		}

		public void commentEndGroup() {
			System.out.println("GdiComment EndGroup");
		}
		
		public void commentEMFPlus( byte[] data, int offset, int len) {
			System.out.println("GdiComment EMF+" );
		}
		
		public void commentGDIC( int type, byte[] data, int offset, int len) {
			System.out.println("GdiComment GDIC " + type);
		}
		
		public void comment(byte[] data, int len, int offset) {
			String s = new String(data, len, offset);
			System.out.println("GdiComment '" + s + "'");
		}

	};
	
	public static void main(String[] args) {
		try {
			FileInputStream fin = new FileInputStream(args[0]);

			FileResourceWriter rw = new FileResourceWriter(new File("C:\\private\\wmf\\out"));
			GDISVGSurface svg = new GDISVGSurface(rw);

			//WMFParser r = new WMFParser(fin, myHandler);
			WMFParser r = new WMFParser(fin, svg);
			//EMFParser r = new EMFParser(fin, myHandler);
			//EMFParser r = new EMFParser(fin, svg);
			while (r.readNext()) {
				// nothing
			}

			System.out.print(svg.getSVG());

			Writer out = new OutputStreamWriter(new FileOutputStream("C:\\private\\wmf\\out\\aaa.svg"), "UTF-8");
			out.write(svg.getSVG());
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
