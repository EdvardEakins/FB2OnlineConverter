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

import com.adobe.dp.office.conv.PNGWriter;

public class GDIBitmap {

	int biWidth;

	int biHeight;

	int biBitsPixel;

	int biCompression;

	byte[] bits;

	int[] colors;

	public GDIBitmap(int biWidth, int biHeight, int biBitsPixel, int biCompression, byte[] bits, int[] colors) {
		this.biWidth = biWidth;
		this.biHeight = biHeight;
		this.biBitsPixel = biBitsPixel;
		this.biCompression = biCompression;
		this.bits = bits;
		this.colors = colors;
	}

	public int getWidth() {
		return biWidth;
	}

	public int getHeight() {
		return biHeight;
	}

	public int getBitsPerPixel() {
		return biBitsPixel;
	}

	public String toString() {
		return "[GDIBitmap " + biWidth + " " + biHeight + " " + biBitsPixel + "]";
	}

	public void saveAsPNG(PNGWriter writer) {
		if (biCompression != 0)
			throw new RuntimeException("not supported yet: comp=" + biCompression);
		if (biBitsPixel == 24 || biBitsPixel == 8) {
			int byteWidth = biWidth * 3;
			int stride = 4 * ((biWidth * biBitsPixel + 31) / 32);
			byte[] scanline = new byte[byteWidth];
			int height;
			int offset;
			int increment;
			if (biHeight > 0) {
				offset = stride * (biHeight - 1);
				increment = -stride;
				height = biHeight;
			} else {
				offset = 0;
				increment = stride;
				height = -biHeight;
			}
			for (int j = 0; j < height; j++) {
				if (biBitsPixel == 24) {
					for (int i = 0; i < byteWidth; i += 3) {
						scanline[i] = bits[offset + i + 2]; // R
						scanline[i + 1] = bits[offset + i + 1]; // G
						scanline[i + 2] = bits[offset + i]; // B
					}
				} else {
					int si = 0;
					for (int i = 0; i < biWidth; i++) {
						int color = colors[bits[offset + i] & 0xFF];
						scanline[si++] = (byte) (color >> 16); // R
						scanline[si++] = (byte) (color >> 8); // G
						scanline[si++] = (byte) color; // B
					}
				}
				writer.writeScanline(scanline, 0, byteWidth);
				offset += increment;
			}
			return;
		}
		if (biBitsPixel == 1) {
			int[] clrs = { 0, 0xFFFFFF };
			if (colors != null)
				clrs = colors;
			int byteWidth = biWidth * 3;
			int stride = 4 * ((biWidth + 31) / 32);
			byte[] scanline = new byte[byteWidth];
			int height;
			int offset;
			int increment;
			if (biHeight > 0) {
				offset = stride * (biHeight - 1);
				increment = -stride;
				height = biHeight;
			} else {
				offset = 0;
				increment = stride;
				height = -biHeight;
			}
			for (int j = 0; j < height; j++) {
				int si = 0;
				for (int i = 0; i < biWidth; i++) {
					int color = clrs[(bits[offset + i / 8] >> (7 - (i & 7))) & 1];
					scanline[si++] = (byte) (color >> 16); // R
					scanline[si++] = (byte) (color >> 8); // G
					scanline[si++] = (byte) color; // B
				}
				writer.writeScanline(scanline, 0, byteWidth);
				offset += increment;
			}
			return;
		}
		throw new RuntimeException("not supported yet: bpp=" + biBitsPixel);
	}
}
