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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public class EMFParser extends MetafileParser {

	final private static int ENHMETA_SIGNATURE = 0x464d4520;

	final private static int EMR_HEADER = 1;

	final private static int EMR_POLYBEZIER = 2;

	final private static int EMR_POLYGON = 3;

	final private static int EMR_POLYLINE = 4;

	final private static int EMR_POLYBEZIERTO = 5;

	final private static int EMR_POLYLINETO = 6;

	final private static int EMR_POLYPOLYLINE = 7;

	final private static int EMR_POLYPOLYGON = 8;

	final private static int EMR_SETWINDOWEXTEX = 9;

	final private static int EMR_SETWINDOWORGEX = 10;

	final private static int EMR_SETVIEWPORTEXTEX = 11;

	final private static int EMR_SETVIEWPORTORGEX = 12;

	final private static int EMR_SETBRUSHORGEX = 13;

	final private static int EMR_EOF = 14;

	final private static int EMR_SETPIXELV = 15;

	final private static int EMR_SETMAPPERFLAGS = 16;

	final private static int EMR_SETMAPMODE = 17;

	final private static int EMR_SETBKMODE = 18;

	final private static int EMR_SETPOLYFILLMODE = 19;

	final private static int EMR_SETROP2 = 20;

	final private static int EMR_SETSTRETCHBLTMODE = 21;

	final private static int EMR_SETTEXTALIGN = 22;

	final private static int EMR_SETCOLORADJUSTMENT = 23;

	final private static int EMR_SETTEXTCOLOR = 24;

	final private static int EMR_SETBKCOLOR = 25;

	final private static int EMR_OFFSETCLIPRGN = 26;

	final private static int EMR_MOVETOEX = 27;

	final private static int EMR_SETMETARGN = 28;

	final private static int EMR_EXCLUDECLIPRECT = 29;

	final private static int EMR_INTERSECTCLIPRECT = 30;

	final private static int EMR_SCALEVIEWPORTEXTEX = 31;

	final private static int EMR_SCALEWINDOWEXTEX = 32;

	final private static int EMR_SAVEDC = 33;

	final private static int EMR_RESTOREDC = 34;

	final private static int EMR_SETWORLDTRANSFORM = 35;

	final private static int EMR_MODIFYWORLDTRANSFORM = 36;

	final private static int EMR_SELECTOBJECT = 37;

	final private static int EMR_CREATEPEN = 38;

	final private static int EMR_CREATEBRUSHINDIRECT = 39;

	final private static int EMR_DELETEOBJECT = 40;

	final private static int EMR_ANGLEARC = 41;

	final private static int EMR_ELLIPSE = 42;

	final private static int EMR_RECTANGLE = 43;

	final private static int EMR_ROUNDRECT = 44;

	final private static int EMR_ARC = 45;

	final private static int EMR_CHORD = 46;

	final private static int EMR_PIE = 47;

	final private static int EMR_SELECTPALETTE = 48;

	final private static int EMR_CREATEPALETTE = 49;

	final private static int EMR_SETPALETTEENTRIES = 50;

	final private static int EMR_RESIZEPALETTE = 51;

	final private static int EMR_REALIZEPALETTE = 52;

	final private static int EMR_EXTFLOODFILL = 53;

	final private static int EMR_LINETO = 54;

	final private static int EMR_ARCTO = 55;

	final private static int EMR_POLYDRAW = 56;

	final private static int EMR_SETARCDIRECTION = 57;

	final private static int EMR_SETMITERLIMIT = 58;

	final private static int EMR_BEGINPATH = 59;

	final private static int EMR_ENDPATH = 60;

	final private static int EMR_CLOSEFIGURE = 61;

	final private static int EMR_FILLPATH = 62;

	final private static int EMR_STROKEANDFILLPATH = 63;

	final private static int EMR_STROKEPATH = 64;

	final private static int EMR_FLATTENPATH = 65;

	final private static int EMR_WIDENPATH = 66;

	final private static int EMR_SELECTCLIPPATH = 67;

	final private static int EMR_ABORTPATH = 68;

	final private static int EMR_GDICOMMENT = 70;

	final private static int EMR_FILLRGN = 71;

	final private static int EMR_FRAMERGN = 72;

	final private static int EMR_INVERTRGN = 73;

	final private static int EMR_PAINTRGN = 74;

	final private static int EMR_EXTSELECTCLIPRGN = 75;

	final private static int EMR_BITBLT = 76;

	final private static int EMR_STRETCHBLT = 77;

	final private static int EMR_MASKBLT = 78;

	final private static int EMR_PLGBLT = 79;

	final private static int EMR_SETDIBITSTODEVICE = 80;

	final private static int EMR_STRETCHDIBITS = 81;

	final private static int EMR_EXTCREATEFONTINDIRECTW = 82;

	final private static int EMR_EXTTEXTOUTA = 83;

	final private static int EMR_EXTTEXTOUTW = 84;

	final private static int EMR_POLYBEZIER16 = 85;

	final private static int EMR_POLYGON16 = 86;

	final private static int EMR_POLYLINE16 = 87;

	final private static int EMR_POLYBEZIERTO16 = 88;

	final private static int EMR_POLYLINETO16 = 89;

	final private static int EMR_POLYPOLYLINE16 = 90;

	final private static int EMR_POLYPOLYGON16 = 91;

	final private static int EMR_POLYDRAW16 = 92;

	final private static int EMR_CREATEMONOBRUSH = 93;

	final private static int EMR_CREATEDIBPATTERNBRUSHPT = 94;

	final private static int EMR_EXTCREATEPEN = 95;

	final private static int EMR_POLYTEXTOUTA = 96;

	final private static int EMR_POLYTEXTOUTW = 97;

	final private static int EMR_SETICMMODE = 98;

	final private static int EMR_CREATECOLORSPACE = 99;

	final private static int EMR_SETCOLORSPACE = 100;

	final private static int EMR_DELETECOLORSPACE = 101;

	final private static int EMR_GLSRECORD = 102;

	final private static int EMR_GLSBOUNDEDRECORD = 103;

	final private static int EMR_PIXELFORMAT = 104;

	final private static int EMR_COLORCORRECTPALETTE = 111;

	final private static int EMR_SETICMPROFILEA = 112;

	final private static int EMR_SETICMPROFILEW = 113;

	final private static int EMR_ALPHABLEND = 114;

	final private static int EMR_SETLAYOUT = 115;

	final private static int EMR_TRANSPARENTBLT = 116;

	final private static int EMR_GRADIENTFILL = 118;

	final private static int EMR_COLORMATCHTOTARGETW = 121;

	final private static int EMR_CREATECOLORSPACEW = 122;

	// GDI comment
	final private static int GDICOMMENT_BEGINGROUP = 0x00000002;

	final private static int GDICOMMENT_ENDGROUP = 0x00000003;

	final private static int GDICOMMENT_UNICODE_STRING = 0x00000040;

	final private static int GDICOMMENT_UNICODE_END = 0x00000080;

	// final private static int GDICOMMENT_MULTIFORMATS = 0x40000004;

	final private static int GDICOMMENT_WINDOWS_METAFILE = 0x80000001;

	final private static int GDICOMMENT_IDENTIFIER = 0x43494447;

	final private static int GDICOMMENT_EMFPLUS = 0x2b464d45;

	public EMFParser(InputStream in, GDISurface handler) throws IOException {
		super(in, handler);
		readFileHeader();
	}

	private void readFileHeader() throws IOException {
		int iType = readInt();
		if (iType != EMR_HEADER)
			throw new IOException("corrupted header");
		int nSize = readInt();
		setRemainsBytes(nSize - 8);
		int boundsLeft = readInt();
		int boundsTop = readInt();
		int boundsRight = readInt();
		int boundsBottom = readInt();
		skipInts(4);
		int dSignature = readInt();
		if (dSignature != ENHMETA_SIGNATURE)
			throw new IOException("corrupted header signature");
		int nVersion = readInt();
		if (nVersion != 0x10000)
			throw new IOException("unsupported version");
		finishRecord();
		handler.setBounds(boundsLeft, boundsTop, boundsRight, boundsBottom);
	}

	private void readGDIComment() throws IOException {
		int len = readInt();
		if (len < 8) {
			byte[] arr = new byte[len];
			readBytes(arr);
			handler.comment(arr, 0, arr.length);
		} else {
			int id = readInt();
			if (id == GDICOMMENT_IDENTIFIER) {
				int commType = readInt();
				switch (commType) {
				case GDICOMMENT_BEGINGROUP: {
					int boundsLeft = readInt();
					int boundsTop = readInt();
					int boundsRight = readInt();
					int boundsBottom = readInt();
					int uChars = readInt();
					StringBuffer desc = new StringBuffer();
					while (uChars > 0) {
						uChars--;
						desc.append((char) readShort());
					}
					handler.commentBeginGroup(boundsLeft, boundsTop, boundsRight, boundsBottom, desc.toString());
					break;
				}
				case GDICOMMENT_ENDGROUP:
					handler.commentEndGroup();
					break;
				case GDICOMMENT_WINDOWS_METAFILE: {
					int version = readInt();
					readInt(); // checksum
					int flags = readInt();
					if (flags != 0)
						throw new IOException("GDICOMMENT_WINDOWS_METAFILE: flags not zero");
					int mfLen = readInt();
					byte[] mfBytes = new byte[mfLen];
					readBytes(mfBytes);
					handler.commentMetafile(version, mfBytes, 0, mfLen);
					break;
				}
				case GDICOMMENT_UNICODE_STRING:
				case GDICOMMENT_UNICODE_END:
				default: {
					byte[] d = new byte[len - 4];
					readBytes(d);
					handler.commentGDIC(commType, d, 0, len);
					break;
				}
				}
			} else if (id == GDICOMMENT_EMFPLUS) {
				byte[] d = new byte[len - 4];
				readBytes(d);
				handler.commentEMFPlus(d, 0, len);
			} else {
				byte[] arr = new byte[len];
				arr[0] = (byte) id;
				arr[1] = (byte) (id >> 8);
				arr[2] = (byte) (id >> 16);
				arr[3] = (byte) (id >> 24);
				readBytes(arr, 4, len - 4);
				handler.comment(arr, 0, arr.length);
			}
		}
	}

	public boolean readNext() throws IOException {
		try {
			int type = readInt();
			int size = readInt();
			setRemainsBytes(size - 8);
			//System.out.println("RECORD " + type + " [" + size + "]");
			switch (type) {
			case EMR_HEADER:
				throw new IOException("unexpected header record");
			case EMR_POLYBEZIER:
			case EMR_POLYGON:
			case EMR_POLYLINE:
			case EMR_POLYBEZIERTO:
			case EMR_POLYLINETO:
			case EMR_POLYPOLYLINE:
			case EMR_POLYPOLYGON:
				break;
			case EMR_SETWINDOWEXTEX: {
				int width = readInt();
				int height = readInt();
				handler.setWindowExt(width, height);
				break;
			}
			case EMR_SETWINDOWORGEX: {
				int x = readInt();
				int y = readInt();
				handler.setWindowOrg(x, y);
				break;
			}
			case EMR_SETVIEWPORTEXTEX: {
				int width = readInt();
				int height = readInt();
				handler.setViewportExt(width, height);
				break;
			}
			case EMR_SETVIEWPORTORGEX: {
				int x = readInt();
				int y = readInt();
				handler.setViewportExt(x, y);
				break;
			}
			case EMR_SETBRUSHORGEX:
				break;
			case EMR_EOF:
				finishRecord();
				return false;
			case EMR_SETPIXELV:
			case EMR_SETMAPPERFLAGS:
				break;
			case EMR_SETMAPMODE: {
				int mode = readInt();
				handler.setMapMode(mode);
				break;
			}
			case EMR_SETBKMODE: {
				int mode = readInt();
				handler.setBkMode(mode);
				break;
			}
			case EMR_SETPOLYFILLMODE:
			case EMR_SETROP2:
			case EMR_SETSTRETCHBLTMODE:
				break;
			case EMR_SETTEXTALIGN: {
				int textAlign = readInt();
				handler.setTextAlign(textAlign);
			}
			case EMR_SETCOLORADJUSTMENT:
			case EMR_SETTEXTCOLOR:
			case EMR_SETBKCOLOR:
			case EMR_OFFSETCLIPRGN:
			case EMR_MOVETOEX:
			case EMR_SETMETARGN:
			case EMR_EXCLUDECLIPRECT:
			case EMR_INTERSECTCLIPRECT:
			case EMR_SCALEVIEWPORTEXTEX:
			case EMR_SCALEWINDOWEXTEX:
				break;
			case EMR_SAVEDC:
				handler.saveDC();
				break;
			case EMR_RESTOREDC:
				handler.restoreDC();
				break;
			case EMR_SETWORLDTRANSFORM:
			case EMR_MODIFYWORLDTRANSFORM:
				break;
			case EMR_SELECTOBJECT: {
				int index = readInt();
				GDIObject gdi = (GDIObject) objects.get(index);
				handler.selectObject(gdi);
				break;
			}
			case EMR_CREATEPEN:
				break;
			case EMR_CREATEBRUSHINDIRECT: {
				int index = readInt();
				int style = readInt();
				int color = readRGB();
				int hatch = readInt();
				GDIBrush brush = handler.createBrushIndirect(style, color, hatch);
				storeObject(brush, index);
				break;
			}
			case EMR_DELETEOBJECT: {
				int index = readInt();
				GDIObject gdi = (GDIObject) objects.get(index);
				objects.set(index, null);
				handler.deleteObject(gdi);
				gdi.dispose();
			}
			case EMR_ANGLEARC:
			case EMR_ELLIPSE:
			case EMR_RECTANGLE:
			case EMR_ROUNDRECT:
			case EMR_ARC:
			case EMR_CHORD:
			case EMR_PIE:
			case EMR_SELECTPALETTE:
			case EMR_CREATEPALETTE:
			case EMR_SETPALETTEENTRIES:
			case EMR_RESIZEPALETTE:
			case EMR_REALIZEPALETTE:
			case EMR_EXTFLOODFILL:
			case EMR_LINETO:
			case EMR_ARCTO:
			case EMR_POLYDRAW:
			case EMR_SETARCDIRECTION:
				break;
			case EMR_SETMITERLIMIT: {
				int miterLimit = readInt();
				handler.setMiterLimit(miterLimit);
			}
			case EMR_BEGINPATH:
			case EMR_ENDPATH:
			case EMR_CLOSEFIGURE:
			case EMR_FILLPATH:
			case EMR_STROKEANDFILLPATH:
			case EMR_STROKEPATH:
			case EMR_FLATTENPATH:
			case EMR_WIDENPATH:
			case EMR_SELECTCLIPPATH:
			case EMR_ABORTPATH:
				break;
			case EMR_GDICOMMENT:
				readGDIComment();
				break;
			case EMR_FILLRGN:
			case EMR_FRAMERGN:
			case EMR_INVERTRGN:
			case EMR_PAINTRGN:
			case EMR_EXTSELECTCLIPRGN:
			case EMR_BITBLT:
			case EMR_STRETCHBLT:
			case EMR_MASKBLT:
			case EMR_PLGBLT:
			case EMR_SETDIBITSTODEVICE:
				break;
			case EMR_STRETCHDIBITS: {
				readInt(); // boundsLeft
				readInt(); // boundsTop
				readInt(); // boundsRight
				readInt(); // boundsBottom
				int xDest = readInt();
				int yDest = readInt();
				int xSrc = readInt();
				int ySrc = readInt();
				int cxSrc = readInt();
				int cySrc = readInt();
				int offBmiSrc = readInt();
				readInt(); // cbBmiSrc
				int offBitsSrc = readInt();
				readInt(); // cbBitsSrc
				readInt(); // iUsageSrc
				readInt(); // dwRop
				int cxDest = readInt();
				int cyDest = readInt();
				skipBytes(offBmiSrc - 80);
				GDIBitmap bitmap = readDIB(offBitsSrc - 80);
				handler.stretchDIB(bitmap, xDest, yDest, cxDest, cyDest, xSrc, ySrc, cxSrc, cySrc);
				break;
			}
			case EMR_EXTCREATEFONTINDIRECTW:
			case EMR_EXTTEXTOUTA:
			case EMR_EXTTEXTOUTW:
			case EMR_POLYBEZIER16:
			case EMR_POLYGON16:
			case EMR_POLYLINE16:
			case EMR_POLYBEZIERTO16:
			case EMR_POLYLINETO16:
				break;
			case EMR_POLYPOLYLINE16:
			case EMR_POLYPOLYGON16: {
				readInt(); // boundsLeft
				readInt(); // boundsTop
				readInt(); // boundsRight
				readInt(); // boundsBottom
				int nPolys = readInt();
				int[] lens = new int[nPolys];
				int total = 0;
				for (int i = 0; i < nPolys; i++) {
					int len = readInt();
					lens[i] = len;
					total += len;
				}
				int totalPts = readInt();
				if (totalPts < total)
					throw new IOException("invalid POLYPOLYXXX record");
				total *= 2;
				int[] points = new int[total];
				for (int i = 0; i < total; i++) {
					points[i] = readShort();
				}
				if (type == EMR_POLYPOLYLINE16)
					handler.polyPolyline(lens, points);
				else
					handler.polyPolygon(lens, points);
				break;
			}
			case EMR_POLYDRAW16:
			case EMR_CREATEMONOBRUSH:
			case EMR_CREATEDIBPATTERNBRUSHPT:
				break;
			case EMR_EXTCREATEPEN: {
				int index = readInt();
				readInt(); // dibOffset
				readInt(); // dibSize
				readInt(); // brushBitsOffset
				readInt(); // brushBitsSize
				int style = readInt();
				int width = readInt();
				readInt(); // brushStyle
				int color = readRGB();
				readInt(); // hatch
				readInt(); // numEntries
				// styleEntry[numEntries];
				GDIPen pen = handler.extCreatePen(style, width, color);
				storeObject(pen, index);
				break;
			}
			case EMR_POLYTEXTOUTA:
			case EMR_POLYTEXTOUTW:
			case EMR_SETICMMODE:
			case EMR_CREATECOLORSPACE:
			case EMR_SETCOLORSPACE:
			case EMR_DELETECOLORSPACE:
			case EMR_GLSRECORD:
			case EMR_GLSBOUNDEDRECORD:
			case EMR_PIXELFORMAT:
			case EMR_COLORCORRECTPALETTE:
			case EMR_SETICMPROFILEA:
			case EMR_SETICMPROFILEW:
			case EMR_ALPHABLEND:
			case EMR_SETLAYOUT:
			case EMR_TRANSPARENTBLT:
			case EMR_GRADIENTFILL:
			case EMR_COLORMATCHTOTARGETW:
			case EMR_CREATECOLORSPACEW:
				break;
			default:
				throw new IOException("unknown command");
			}
			finishRecord();
		} catch (EOFException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public void readAll() throws IOException {
		while (readNext()) {
		}
		close();
	}
	
}
