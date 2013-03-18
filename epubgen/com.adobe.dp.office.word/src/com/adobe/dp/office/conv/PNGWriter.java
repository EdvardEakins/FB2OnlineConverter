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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.CRC32;
import java.util.zip.DeflaterOutputStream;

public class PNGWriter {

	final static private byte[] sig = { (byte) 137, 80, 78, 71, 13, 10, 26, 10 };

	final static private byte[] idat = { 'I', 'D', 'A', 'T' };

	final static private byte[] end = { 0, 0, 0, 0, 'I', 'E', 'N', 'D', (byte) 0xAE, 0x42, 0x60, (byte) 0x82 };

	int width;

	int height;

	int byteWidth;

	boolean alpha;

	byte[] prevLine;

	byte[] filterBuf;

	ByteArrayOutputStream compressedImage = new ByteArrayOutputStream();

	DeflaterOutputStream deflater = new DeflaterOutputStream(compressedImage);

	OutputStream out;

	public PNGWriter(OutputStream out, int width, int height, boolean alpha) throws IOException {
		this.out = out;
		this.width = width;
		this.height = height;
		this.alpha = alpha;
		out.write(sig);
		byte[] len = { 0, 0, 0, 13 };
		out.write(len);
		byte[] ihdr = { 'I', 'H', 'D', 'R', (byte) (width >> 24), (byte) (width >> 16), (byte) (width >> 8),
				(byte) width, (byte) (height >> 24), (byte) (height >> 16), (byte) (height >> 8), (byte) height,
				8 /* bit depth */, (byte) (alpha ? 6 /* RGBA */: 2 /* RGB */), 0 /* flate */, 0 /* filter */, 0 };
		out.write(ihdr);
		CRC32 crc32 = new CRC32();
		crc32.update(ihdr);
		long crc = crc32.getValue();
		byte[] checksum = { (byte) (crc >> 24), (byte) (crc >> 16), (byte) (crc >> 8), (byte) crc };
		out.write(checksum);
		byteWidth = width * (alpha ? 4 : 3);
		filterBuf = new byte[byteWidth + 1];
	}

	public void writeScanline(byte[] pixels, int offset, int len) {
		if (len != byteWidth)
			throw new IllegalArgumentException("len");
		try {
			if (prevLine == null) {
				filterBuf[0] = 0; // None
				System.arraycopy(pixels, offset, filterBuf, 1, byteWidth);
				prevLine = new byte[byteWidth];
			} else {
				filterBuf[0] = 2; // Up
				for( int i = 0 ; i < byteWidth ; i++ )
					filterBuf[i+1] = (byte)(pixels[offset+i] - prevLine[i]);
			}
			deflater.write(filterBuf);
			System.arraycopy(pixels, offset, prevLine, 0, byteWidth);
		} catch (IOException e) {
			throw new Error("IOException while writing to memory-based stream");
		}
	}

	public void close() throws IOException {
		deflater.close();
		byte[] bytes = compressedImage.toByteArray();
		int csz = bytes.length;
		byte[] len = { (byte) (csz >> 24), (byte) (csz >> 16), (byte) (csz >> 8), (byte) csz };
		out.write(len);
		out.write(idat);
		out.write(bytes);
		CRC32 crc32 = new CRC32();
		crc32.update(idat);
		crc32.update(bytes);
		long crc = crc32.getValue();
		byte[] checksum = { (byte) (crc >> 24), (byte) (crc >> 16), (byte) (crc >> 8), (byte) crc };
		out.write(checksum);
		out.write(end);
		out.close();
	}
}
