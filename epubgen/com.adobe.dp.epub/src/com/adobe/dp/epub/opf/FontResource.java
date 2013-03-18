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

package com.adobe.dp.epub.opf;

import com.adobe.dp.epub.io.DataSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.Deflater;

public class FontResource extends Resource {

	FontResource(Publication owner, String name, DataSource source) {
		super(owner, name, "application/vnd.ms-opentype", source);
	}

	static class DeflaterInputStreamImpl extends InputStream {

		InputStream in;

		Deflater def;

		DeflaterInputStreamImpl(InputStream in, Deflater def) {
			this.in = in;
			this.def = def;
		}

		public void close() throws IOException {
			def.end();
		}

		public int read() throws IOException {
			byte[] b = new byte[1];
			if (read(b) == 1)
				return b[0] & 0xFF;
			return -1;
		}

		public int read(byte[] b, int off, int len) throws IOException {
			int total = 0;
			while (len > 0) {
				int got = def.deflate(b, off, len);
				if (got <= 0) {
					if (in != null && def.needsInput()) {
						byte[] buffer = new byte[4096];
						int rlen = in.read(buffer);
						if (rlen <= 0) {
							def.finish();
							in.close();
							in = null;
						} else {
							def.setInput(buffer, 0, rlen);
						}
					} else {
						break;
					}
				} else {
					total += got;
					off += got;
					len -= got;
				}
			}
			return total;
		}

	}

    public void serialize(OutputStream out) throws IOException {
        byte[] buf = new byte[4096];
        InputStream in = source.getInputStream();
        int len;
        while ((len = in.read(buf)) >= 0) {
            out.write(buf, 0, len);
        }
        out.close();
    }
	/**
	 * Serializes this embedded font. Implements the Obfuscation Algorithm
	 * either from
	 * http://www.openebook.org/doc_library/informationaldocs/FontManglingSpec
	 * .html or from
	 * http://www.adobe.com/devnet/digitalpublishing/pdfs/content_protection.pdf
	 * depending on the type of font mangling used
	 */
//	public void serialize(OutputStream out) throws IOException {
//
//		int headerLen;
//		byte[] mask;
//
//		if (epub.useIDPFFontMangling) {
//			headerLen = 1040;
//			mask = epub.makeIDPFXORMask();
//		} else {
//			headerLen = 1024;
//			mask = epub.makeAdobeXORMask();
//		}
//
//		Deflater def = null;
//		try {
//			byte[] buffer = new byte[4096];
//			int len;
//			InputStream in = source.getInputStream();
//			if (mask != null) {
//				// encryption assumes compression
//				def = new Deflater(9, true);
//				in = new DeflaterInputStreamImpl(in, def);
//			}
//			boolean first = true;
//			while ((len = in.read(buffer)) > 0) {
//				if (first && mask != null) {
//					first = false;
//					for (int i = 0; i < headerLen; i++) {
//						buffer[i] = (byte) (buffer[i] ^ mask[i % mask.length]);
//					}
//				}
//				out.write(buffer, 0, len);
//			}
//		} catch (IOException e) {
//			e.printStackTrace();
//		} finally {
//			// It is important to do this to free non-Java-heap memory
//			// see Java bug 4797189 for details
//			if (def != null) {
//				def.end();
//				def = null;
//			}
//		}
//		out.close();
//	}

	/**
	 * Return false to inhibit compressing
	 */
	public boolean canCompress() {
		return true;
	}
}