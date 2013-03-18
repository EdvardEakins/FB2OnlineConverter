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

import java.util.Stack;

public class GDISurface {

	// text alignment
	public static final int TA_TOP        = 0;
	public static final int TA_CENTER     = 6;
	public static final int TA_BOTTOM     = 8;
	public static final int TA_BASELINE   = 24;
	public static final int TA_LEFT       = 0;
	public static final int TA_RIGHT      = 2;
	public static final int TA_RTLREADING = 256;
	public static final int TA_NOUPDATECP = 0;
	public static final int TA_UPDATECP   = 1;
	
	// PolyFill
	public static final int ALTERNATE = 1;
	public static final int WINDING = 2;
	
	class GDIState implements Cloneable {
		GDIBrush currentBrush;

		GDIPen currentPen;

		GDIFont currentFont;

		int windowOrgX;

		int windowOrgY;

		int windowExtX;

		int windowExtY;

		int viewportOrgX;

		int viewportOrgY;

		int viewportExtX;

		int viewportExtY;

		int textColor;

		int mapMode;

		int bkColor;

		int rop2;

		int bkMode;

		int textAlign;

		int polyFillMode = WINDING;

		int miterLimit;

		GDIState cloneState() {
			try {
				return (GDIState) clone();
			} catch (CloneNotSupportedException e) {
				throw new Error("Unexpected exception: CloneNotSupportedException");
			}
		}
	};

	Stack saved = new Stack();

	GDIState state = new GDIState();

	int boundsLeft;

	int boundsTop;

	int boundsRight;

	int boundsBottom;

	// -------------------------- setup ---------------------------------

	public void setBounds(int left, int top, int right, int bottom) {
		boundsLeft = left;
		boundsTop = top;
		boundsRight = right;
		boundsBottom = bottom;
	}

	// --------------------------- GDI functions -------------------------

	public void setWindowOrg(int x, int y) {
		state.windowOrgX = x;
		state.windowOrgY = y;
	}

	public void setWindowExt(int x, int y) {
		state.windowExtX = x;
		state.windowExtY = y;
		if( boundsLeft == boundsRight )
			boundsRight = boundsRight + (x>0?x:-x);
		if( boundsTop == boundsBottom )
			boundsBottom = boundsTop + (y>0?y:-y);
	}

	public void setViewportOrg(int x, int y) {
		state.viewportOrgX = x;
		state.viewportOrgY = y;
	}

	public void setViewportExt(int x, int y) {
		state.viewportExtX = x;
		state.viewportExtY = y;
	}

	public void setTextColor(int rgb) {
		state.textColor = rgb;
	}

	public void setMapMode(int mode) {
		state.mapMode = mode;
	}

	public void setBkColor(int rgb) {
		state.bkColor = rgb;
	}

	public void setROP2(int mode) {
		state.rop2 = mode;
	}

	public void setBkMode(int mode) {
		state.bkMode = mode;
	}

	public void setTextAlign(int mode) {
		state.textAlign = mode;
	}

	public void setMiterLimit(int miterLimit) {
		state.miterLimit = miterLimit;
	}

	public void selectObject(GDIObject obj) {
		if (obj instanceof GDIPen) {
			state.currentPen = (GDIPen) obj;
		} else if (obj instanceof GDIBrush) {
			state.currentBrush = (GDIBrush) obj;
		} else if (obj instanceof GDIFont) {
			state.currentFont = (GDIFont) obj;
		}
	}

	public void deleteObject(GDIObject obj) {
		GDIState s = state;
		int si = saved.size();
		while (true) {
			if (obj == s.currentPen)
				s.currentPen = null;
			else if (obj == s.currentBrush)
				s.currentBrush = null;
			else if (obj == s.currentFont)
				s.currentFont = null;
			si--;
			if (si < 0)
				break;
			s = (GDIState) saved.get(si);
		}
	}

	public void setPolyFillMode(int mode) {
		state.polyFillMode = mode;
	}

	public void saveDC() {
		saved.push(state);
		state = state.cloneState();
	}

	public void restoreDC() {
		state = (GDIState) saved.pop();
	}

	public void moveTo(int x, int y) {
	}

	public void lineTo(int x, int y) {
	}

	public void rectangle(int x1, int y1, int x2, int y2) {
	}

	public void ellipse(int x1, int y1, int x2, int y2) {
	}

	public void polygon(int[] points, int offset, int len) {
	}

	public void polyline(int[] points, int offset, int len) {
	}

	public void polyPolyline(int[] lens, int points[]) {
		int offset = 0;
		for (int i = 0; i < lens.length; i++) {
			int len = lens[i] * 2;
			polyline(points, offset, len);
			offset += len;
		}
	}

	public void polyPolygon(int[] lens, int points[]) {
		int offset = 0;
		for (int i = 0; i < lens.length; i++) {
			int len = lens[i] * 2;
			polygon(points, offset, len);
			offset += len;
		}
	}

	public void extTextOut(int x, int y, String text, int flags, int[] clipRect, int[] adj) {
	}

	public GDIPen createPenIndirect(int style, int width, int rgb) {
		return new GDIPen(style, width, rgb);
	}

	public GDIPen extCreatePen(int extStyle, int width, int rgb) {
		return new GDIPen(GDIPen.PS_SOLID, width, rgb);
	}

	public GDIBrush createBrushIndirect(int style, int rgb, int hatch) {
		return new GDIBrush(style, rgb, hatch);
	}

	public GDIFont createFontIndirect(int fontHeight, int width, int esc, int orientation, int weight, String name,
			boolean italic, boolean underline, boolean strikeout, int charset, int quality, int pitchAndFamily) {
		return new GDIFont(name, fontHeight, weight, italic, underline, strikeout, charset, quality);
	}

	public void stretchDIB(GDIBitmap bitmap, int destX, int destY, int destWidth, int destHeight, int srcX, int srcY,
			int srcWidth, int srcHeight) {
	}

	public void commentMetafile(int version, byte[] data, int offset, int len ) {
	}
	
	public void commentBeginGroup( int left, int top, int right, int bottom, String desc ) {
	}
	
	public void commentEndGroup() {
	}
	
	public void commentEMFPlus(byte[] data, int offset, int len) {
	}
	
	public void commentGDIC(int type, byte[] data, int offset, int len) {
	}
	
	public void comment(byte[] data, int offset, int len) {
	}
	
	// ------------------------- utility functions -------------------------

	public GDIBrush getCurrentBrush() {
		return state.currentBrush;
	}

	public GDIFont getCurrentFont() {
		return state.currentFont;
	}

	public GDIPen getCurrentPen() {
		return state.currentPen;
	}

	public int getBkColor() {
		return state.bkColor;
	}

	public int getBkMode() {
		return state.bkMode;
	}

	public int getMapMode() {
		return state.mapMode;
	}

	public int getPolyFillMode() {
		return state.polyFillMode;
	}

	public int getROP2() {
		return state.rop2;
	}

	public int getTextAlign() {
		return state.textAlign;
	}

	public int getTextColor() {
		return state.textColor;
	}

	public int getWindowExtX() {
		return state.windowExtX;
	}

	public int getWindowExtY() {
		return state.windowExtY;
	}

	public int getWindowOrgX() {
		return state.windowOrgX;
	}

	public int getWindowOrgY() {
		return state.windowOrgY;
	}

	public int getBoundsBottom() {
		return boundsBottom;
	}

	public int getBoundsLeft() {
		return boundsLeft;
	}

	public int getBoundsRight() {
		return boundsRight;
	}

	public int getBoundsTop() {
		return boundsTop;
	}

	public int getViewportExtX() {
		return state.viewportExtX;
	}

	public int getViewportExtY() {
		return state.viewportExtY;
	}

	public int getViewportOrgX() {
		return state.viewportOrgX;
	}

	public int getViewportOrgY() {
		return state.viewportOrgY;
	}

	public int getMiterLimit() {
		return state.miterLimit;
	}

	public GDIMatrix getViewportMatrix() {
		double sx = 1;
		double sy = 1;
		
		// viewport
		int viewX = state.viewportOrgX;
		int viewY = state.viewportOrgY;
		int viewWidth = state.viewportExtX;
		if( viewWidth == 0 )
		{
			viewX = boundsLeft;
			viewWidth = boundsRight - boundsLeft;
		}
		int viewHeight = state.viewportExtY;
		if( viewHeight == 0 )
		{
			viewY = boundsTop;
			viewHeight = boundsBottom - boundsTop;
		}
		
		// device units
		double x = viewX - boundsLeft;
		double y = viewY - boundsTop;
		
		// scaling
		if (state.windowExtX != 0 && state.windowExtY != 0 && viewWidth != 0 && viewHeight != 0) {
			sx = viewWidth/((double) state.windowExtX);
			sy = viewHeight/((double) state.windowExtY);
		}
		
		// logical units
		x -= sx * state.windowOrgX;
		y -= sy * state.windowOrgY;
		return new GDIMatrix(sx, 0, 0, sy, x, y);
	}
}
