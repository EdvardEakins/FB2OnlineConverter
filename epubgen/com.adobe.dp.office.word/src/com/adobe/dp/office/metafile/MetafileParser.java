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
import java.util.Vector;

public abstract class MetafileParser {

	private InputStream in;

	private int remainsInRecord;

	GDISurface handler;

	Vector objects = new Vector();

	protected MetafileParser(InputStream in, GDISurface handler) throws IOException {
		this.in = in;
		this.handler = handler;
	}

	protected int storeObject(Object obj, int index) {
		int len = objects.size();
		for (int i = index; i < len; i++) {
			if (objects.get(i) == null) {
				objects.set(i, obj);
				return i;
			}
		}
		while (objects.size() < index)
			objects.add(null);
		objects.add(obj);
		return len;
	}

	protected void setRemainsBytes(int r) {
		remainsInRecord = r;
	}

	protected void setRemainsShorts(int r) {
		remainsInRecord = 2 * r;
	}

	protected int remainsBytes() {
		return remainsInRecord;
	}

	protected int remainsShorts() {
		return remainsInRecord / 2;
	}

	protected int remainsInts() {
		return remainsInRecord / 4;
	}

	protected void skipBytes(int n) throws IOException {
		if (in.skip(n) != n)
			throw new EOFException();
		remainsInRecord -= n;
	}

	protected void skipShorts(int n) throws IOException {
		int byteCount = 2 * n;
		if (in.skip(byteCount) != byteCount)
			throw new EOFException();
		remainsInRecord -= byteCount;
	}

	protected void skipInts(int n) throws IOException {
		int byteCount = 4 * n;
		if (in.skip(byteCount) != byteCount)
			throw new EOFException();
		remainsInRecord -= byteCount;
	}

	protected short readShort() throws IOException {
		int b1 = in.read();
		if (b1 < 0)
			throw new EOFException();
		int b2 = in.read();
		if (b2 < 0)
			throw new EOFException();
		remainsInRecord -= 2;
		return (short) ((b2 << 8) | (b1 & 0xFF));
	}

	protected int readInt() throws IOException {
		int b1 = in.read();
		if (b1 < 0)
			throw new EOFException();
		int b2 = in.read();
		if (b2 < 0)
			throw new EOFException();
		int b3 = in.read();
		if (b3 < 0)
			throw new EOFException();
		int b4 = in.read();
		if (b4 < 0)
			throw new EOFException();
		remainsInRecord -= 4;
		return ((b4 & 0xFF) << 24) | ((b3 & 0xFF) << 16) | ((b2 & 0xFF) << 8) | (b1 & 0xFF);
	}

	protected void readBytes(byte[] arr, int offset, int len) throws IOException {
		while (offset < len) {
			int r = in.read(arr, offset, len - offset);
			if (r <= 0)
				throw new IOException("not enough data");
			offset += r;
		}
		remainsInRecord -= len;
	}

	protected void readBytes(byte[] arr) throws IOException {
		readBytes(arr, 0, arr.length);
	}

	protected int readRGB() throws IOException {
		int bgr = readInt();
		int red = bgr & 0xFF;
		int green = (bgr >> 8) & 0xFF;
		int blue = (bgr >> 16) & 0xFF;
		return (red << 16) | (green << 8) | blue;
	}

	protected void finishRecord() throws IOException {
		if (remainsInRecord > 0) {
			in.skip(remainsInRecord);
			remainsInRecord = 0;
		}
	}

	protected GDIBitmap readDIB() throws IOException {
		return readDIB(0);
	}

	protected GDIBitmap readDIB(int offsetToBytes) throws IOException {
		int biSize = readInt();
		if (biSize != 40)
			throw new IOException("Problem with BITMAPINFOHEADER");
		int biWidth = readInt();
		int biHeight = readInt();
		short biPlanes = readShort();
		if (biPlanes != 1)
			throw new IOException("Problem with BITMAPINFOHEADER");
		short biBitsPixel = readShort();
		int biCompression = readInt();
		int biSizeImage = readInt();
		if( biSizeImage == 0 ) {
			int stride = 4*((biBitsPixel*biWidth+31)/32);
			biSizeImage = stride*biHeight;
		}
		readInt();
		readInt();
		int biClrUsed = readInt();
		readInt();
		offsetToBytes -= 40;
		int[] colors = null;
		if( biClrUsed > 0 ) {
			colors = new int[biClrUsed];
			for( int i = 0 ; i < biClrUsed ; i++ ) {
				offsetToBytes -= 4;
				colors[i] = readRGB();
			}
		}
		byte[] bits = new byte[biSizeImage];
		if( offsetToBytes > 0 )
			skipBytes(offsetToBytes);
		readBytes(bits);
		return new GDIBitmap(biWidth, biHeight, biBitsPixel, biCompression, bits, colors);
	}

	abstract public boolean readNext() throws IOException;

	protected void close() throws IOException {
		in.close();
	}
}
