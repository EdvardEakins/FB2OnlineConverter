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

// WMF doc: http://wvware.sourceforge.net/caolan/support.html
// wxwidgets

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public class WMFParser extends MetafileParser {

	public static final int META_MAGIC = 0x9AC6CDD7;

	public static final int META_MAGIC1 = 0x00090001;

	public static final int META_MAGIC2 = 0x00090002;

	private static final int META_SAVEDC = 0x1E;

	// private static final int META_REALIZEPALETTE = 0x35;

	// private static final int META_SETPALENTRIES = 0x37;

	// private static final int META_CREATEPALETTE = 0xf7;

	private static final int META_SETBKMODE = 0x102;

	private static final int META_SETMAPMODE = 0x103;

	private static final int META_SETROP2 = 0x104;

	private static final int META_SETRELABS = 0x105;

	private static final int META_SETPOLYFILLMODE = 0x106;

	// private static final int META_SETSTRETCHBLTMODE = 0x107;

	// private static final int META_SETTEXTCHAREXTRA = 0x108;

	private static final int META_RESTOREDC = 0x127;

	// private static final int META_INVERTREGION = 0x12A;

	// private static final int META_PAINTREGION = 0x12B;

	// private static final int META_SELECTCLIPREGION = 0x12C;

	private static final int META_SELECTOBJECT = 0x12D;

	private static final int META_SETTEXTALIGN = 0x12E;

	// private static final int META_RESIZEPALETTE = 0x139;

	// private static final int META_DIBCREATEPATTERNBRUSH = 0x142;

	// private static final int META_SETLAYOUT = 0x149;

	private static final int META_DELETEOBJECT = 0x1F0;

	// private static final int META_CREATEPATTERNBRUSH = 0x1F9;

	private static final int META_SETBKCOLOR = 0x201;

	private static final int META_SETTEXTCOLOR = 0x209;

	// private static final int META_SETTEXTJUSTIFICATION = 0x20A;

	private static final int META_SETWINDOWORG = 0x20B;

	private static final int META_SETWINDOWEXT = 0x20C;

	// private static final int META_SETVIEWPORTORG = 0x20D;

	// private static final int META_SETVIEWPORTEXT = 0x20E;

	// private static final int META_OFFSETWINDOWORG = 0x20F;

	// private static final int META_OFFSETVIEWPORTORG = 0x211;

	private static final int META_LINETO = 0x213;

	private static final int META_MOVETO = 0x214;

	// private static final int META_OFFSETCLIPRGN = 0x220;

	// private static final int META_FILLREGION = 0x228;

	// private static final int META_SETMAPPERFLAGS = 0x231;

	// private static final int META_SELECTPALETTE = 0x234;

	private static final int META_CREATEPENINDIRECT = 0x2FA;

	private static final int META_CREATEFONTINDIRECT = 0x2FB;

	private static final int META_CREATEBRUSHINDIRECT = 0x2FC;

	private static final int META_POLYGON = 0x324;

	private static final int META_POLYLINE = 0x325;

	// private static final int META_SCALEWINDOWEXT = 0x410;

	// private static final int META_SCALEVIEWPORTEXT = 0x412;

	// private static final int META_EXCLUDECLIPRECT = 0x415;

	// private static final int META_INTERSECTCLIPRECT = 0x416;

	private static final int META_ELLIPSE = 0x418;

	// private static final int META_FLOODFILL = 0x419;

	private static final int META_RECTANGLE = 0x41B;

	// private static final int META_SETPIXEL = 0x41F;

	// private static final int META_FRAMEREGION = 0x429;

	// private static final int META_ANIMATEPALETTE = 0x436;

	// private static final int META_TEXTOUT = 0x521;

	private static final int META_POLYPOLYGON = 0x538;

	// private static final int META_EXTFLOODFILL = 0x548;

	// private static final int META_ROUNDRECT = 0x61C;

	// private static final int META_PATBLT = 0x61D;

	// private static final int META_ESCAPE = 0x626;

	// private static final int META_CREATEREGION = 0x6FF;

	// private static final int META_ARC = 0x817;

	// private static final int META_PIE = 0x81A;

	// private static final int META_CHORD = 0x830;

	// private static final int META_BITBLT = 0x922;

	// private static final int META_DIBBITBLT = 0x940;

	private static final int META_EXTTEXTOUT = 0xA32;

	//private static final int META_STRETCHBLT = 0xB23;

	private static final int META_DIBSTRETCHBLT = 0xB41;

	//private static final int META_SETDIBTODEV = 0xD33;

	private static final int META_STRETCHDIB = 0xF43;

	public WMFParser(InputStream in, GDISurface handler) throws IOException {
		super(in, handler);
		readFileHeader();
	}

	private int readRecordHeader() throws IOException {
		int remainsInRecord = readInt(); // size in shorts
		int opcode = readShort() & 0xFFFF;
		setRemainsShorts(remainsInRecord - 3);
		return opcode;
	}

	private void readFileHeader() throws IOException {
		int magic = readInt();
		if (magic == META_MAGIC) {
			readShort();
			short left = readShort();
			short top = readShort();
			short right = readShort();
			short bottom = readShort();
			handler.setBounds(left, top, right, bottom);
			skipShorts(13);
		} else if (magic == META_MAGIC1 || magic == META_MAGIC2) {
			skipShorts(7);
		} else
			throw new IOException("Invalid file format");
	}

	public void readAll() throws IOException {
		while (readNext()) {
		}
		close();
	}

	public boolean readNext() throws IOException {
		try {
			int opcode = readRecordHeader();
			switch (opcode) {
			case 0: // end
				return false;
			case META_SAVEDC: {
				handler.saveDC();
				break;
			}
			case META_RESTOREDC: {
				handler.restoreDC();
				break;
			}
			case META_STRETCHDIB: {
				skipShorts(3);
				int srcHeight = readShort();
				int srcWidth = readShort();
				int srcY = readShort();
				int srcX = readShort();
				int destHeight = readShort();
				int destWidth = readShort();
				int destY = readShort();
				int destX = readShort();
				GDIBitmap bitmap = readDIB();
				handler.stretchDIB(bitmap, destX, destY, destWidth, destHeight, srcX, srcY, srcWidth, srcHeight);
				break;
			}
			case META_DIBSTRETCHBLT: {
				readInt(); // rop; not supported
				int srcY = readShort();
				int srcX = readShort();
				int srcHeight = readShort();
				int srcWidth = readShort();
				int destHeight = readShort();
				int destWidth = readShort();
				int destY = readShort();
				int destX = readShort();
				GDIBitmap bitmap = readDIB();
				handler.stretchDIB(bitmap, destX, destY, destWidth, destHeight, srcX, srcY, srcWidth, srcHeight);
				break;
			}
			case META_SETMAPMODE: {
				if (remainsBytes() <= 0)
					throw new IOException("Problem in SETMAPMODE");
				int mode = readShort();
				handler.setMapMode(mode);
				break;
			}
			case META_SETROP2: {
				if (remainsBytes() <= 0)
					throw new IOException("Problem in SETROP2");
				int mode = readShort();
				handler.setROP2(mode);
				break;
			}
			case META_SETPOLYFILLMODE: {
				if (remainsBytes() <= 0)
					throw new IOException("Problem in SETPOLYFILLMODE");
				short mode = readShort();
				handler.setPolyFillMode(mode);
				break;
			}
			case META_SETBKMODE: {
				if (remainsBytes() <= 0)
					throw new IOException("Problem in SETBKMODE");
				short mode = readShort();
				handler.setBkMode(mode);
				break;
			}
			case META_SETTEXTALIGN: {
				if (remainsBytes() <= 0)
					throw new IOException("Problem in SETTEXTALIGN");
				short mode = readShort();
				handler.setTextAlign(mode);
				break;
			}
			case META_SELECTOBJECT: {
				if (remainsBytes() <= 0)
					throw new IOException("Problem in SELECTOBJECT");
				short handle = readShort();
				GDIObject gdi = (GDIObject) objects.get(handle);
				handler.selectObject(gdi);
				break;
			}
			case META_DELETEOBJECT: {
				if (remainsBytes() <= 0)
					throw new IOException("Problem in DELETEOBJECT");
				short handle = readShort();
				GDIObject gdi = (GDIObject) objects.get(handle);
				handler.deleteObject(gdi);
				gdi.dispose();
				objects.set(handle, null);
				break;
			}
			case META_SETWINDOWEXT: {
				if (remainsShorts() < 2)
					throw new IOException("Problem in SETWINDOWEXT");
				short h = readShort();
				short w = readShort();
				handler.setWindowExt(w, h);
				break;
			}
			case META_SETWINDOWORG: {
				if (remainsShorts() < 2)
					throw new IOException("Problem in SETWINDOWORG");
				short y = readShort();
				short x = readShort();
				handler.setWindowOrg(x, y);
				break;
			}
			case META_SETBKCOLOR: {
				if (remainsShorts() < 2)
					throw new IOException("Problem in SETBKCOLOR");
				handler.setBkColor(readRGB());
				break;
			}
			case META_SETTEXTCOLOR: {
				if (remainsShorts() < 2)
					throw new IOException("Problem in SETTEXTCOLOR");
				handler.setTextColor(readRGB());
				break;
			}
			case META_CREATEPENINDIRECT: {
				if (remainsShorts() < 5)
					throw new IOException("Problem in CREATEPENINDIRECT");
				short style = readShort();
				short widthX = readShort();
				short widthY = readShort();
				int width = (widthX > widthY ? widthX : widthY);
				int rgb = readRGB();
				Object pen = handler.createPenIndirect(style, width, rgb);
				storeObject(pen, 0);
				break;
			}
			case META_CREATEBRUSHINDIRECT: {
				if (remainsShorts() < 4)
					throw new IOException("Problem in CREATEBRUSHINDIRECT");
				short style = readShort();
				int rgb = readRGB();
				short hatch = readShort();
				Object brush = handler.createBrushIndirect(style, rgb, hatch);
				storeObject(brush, 0);
				break;

			}
			case META_CREATEFONTINDIRECT: {
				if (remainsShorts() < 10)
					throw new IOException("Problem in CREATEFONTINDIRECT");
				int fontHeight = readShort();
				int width = readShort(); // width
				int esc = readShort(); // esc
				int orientation = readShort(); // orientation
				int weight = readShort(); // weight
				int dl = 2 * remainsShorts();
				byte[] data = new byte[dl];
				readBytes(data);
				int l = 8;
				while (l < dl && data[l] != 0)
					l++;
				String name = new String(data, 8, l - 8);
				boolean italic = data[0] != 0;
				boolean underline = data[1] != 0;
				boolean strikeout = data[2] != 0;
				int charset = data[3] & 0xFF;
				int quality = data[6] & 0xFF;
				int pitchAndFamily = data[7] & 0xFF;
				Object font = handler.createFontIndirect(fontHeight, width, esc, orientation, weight, name, italic,
						underline, strikeout, charset, quality, pitchAndFamily);
				storeObject(font, 0);
				break;
			}
			case META_EXTTEXTOUT: {
				if (remainsShorts() < 4)
					throw new IOException("Problem in EXTTEXTOUT");
				short y = readShort();
				short x = readShort();
				short count = readShort();
				short flags = readShort();
				int[] clipRect = null;
				int[] adj = null;
				if ((flags & 4) != 0) {
					// ETO_CLIPPED
					clipRect = new int[4];
					clipRect[0] = readShort(); // x1
					clipRect[1] = readShort(); // y1
					clipRect[2] = readShort(); // x2
					clipRect[3] = readShort(); // y2
				}
				byte[] data = new byte[2 * remainsShorts()];
				readBytes(data);
				String text = new String(data, 0, count);
				handler.extTextOut(x, y, text, flags, clipRect, adj);
				break;
			}
			case META_MOVETO: {
				if (remainsShorts() < 2)
					throw new IOException("Problem in MOVETO");
				short y = readShort();
				short x = readShort();
				handler.moveTo(x, y);
				break;
			}
			case META_LINETO: {
				if (remainsShorts() < 2)
					throw new IOException("Problem in LINETO");
				short y = readShort();
				short x = readShort();
				handler.lineTo(x, y);
				break;
			}
			case META_RECTANGLE: {
				if (remainsShorts() < 4)
					throw new IOException("Problem in RECTANGLE");
				short bottom = readShort();
				short right = readShort();
				short top = readShort();
				short left = readShort();
				handler.rectangle(left, top, right, bottom);
				break;
			}
			case META_ELLIPSE: {
				if (remainsShorts() < 4)
					throw new IOException("Problem in ELLIPSE");
				short bottom = readShort();
				short right = readShort();
				short top = readShort();
				short left = readShort();
				handler.ellipse(left, top, right, bottom);
				break;
			}
			case META_POLYGON: {
				if (remainsShorts() < 2)
					throw new IOException("Problem in POLYGON");
				int count = readShort() * 2;
				int[] points = new int[count];
				for (int i = 0; i < count; i++) {
					points[i] = readShort();
				}
				handler.polygon(points, 0, count);
				break;
			}
			case META_POLYLINE: {
				if (remainsShorts() < 2)
					throw new IOException("Problem in POLYLINE");
				int count = readShort() * 2;
				int[] points = new int[count];
				for (int i = 0; i < count; i++) {
					points[i] = readShort();
				}
				handler.polyline(points, 0, count);
				break;
			}
			case META_POLYPOLYGON: {
				if (remainsShorts() < 2)
					throw new IOException("Problem in POLYPOLYLINE");
				int polyCount = readShort();
				int[] pointCounts = new int[polyCount];
				int total = 0;
				for (int i = 0; i < polyCount; i++) {
					int n = readShort();
					total += n;
					pointCounts[i] = n;
				}
				total *= 2;
				int[] points = new int[total];
				for (int i = 0; i < total; i++) {
					points[i] = readShort();
				}
				handler.polyPolygon(pointCounts, points);
				break;
			}
			case META_SETRELABS: // undocumented API
			default:
				System.out.println("UNKNOWN " + Integer.toHexString(opcode) + " " + remainsShorts());
				break;
			}
			finishRecord();
			return true;
		} catch (EOFException e) {
			return false;
		}
	}

}
