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

package com.adobe.dp.epub.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class OCFContainerWriter extends ContainerWriter {

	ZipOutputStream zip;

	class CompressedEntryStream extends OutputStream {

		CompressedEntryStream() {
		}

		public void write(int b) throws IOException {
			zip.write(b);
		}

		public void close() throws IOException {
			zip.closeEntry();
		}

		public void flush() throws IOException {
			zip.flush();
		}

		public void write(byte[] arg0, int arg1, int arg2) throws IOException {
			zip.write(arg0, arg1, arg2);
		}
	}

	class StoredEntryStream extends OutputStream {

		String name;
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		
		StoredEntryStream(String name) {
			this.name = name;
		}

		public void write(int b) throws IOException {
			buffer.write(b);
		}

		public void close() throws IOException {
			byte[] bytes = buffer.toByteArray();
			ZipEntry entry = new ZipEntry(name);
			entry.setMethod(ZipOutputStream.STORED);
			entry.setSize(bytes.length);
			entry.setCompressedSize(bytes.length);
			CRC32 crc = new CRC32();
			crc.update(bytes);
			entry.setCrc(crc.getValue());
			zip.putNextEntry(entry);
			zip.write(bytes);
			zip.closeEntry();
		}

		public void flush() throws IOException {
		}

		public void write(byte[] buf, int arg1, int arg2) throws IOException {
			buffer.write(buf, arg1, arg2);
		}
	}

	public OCFContainerWriter(OutputStream out) throws IOException {
		this(out, "application/epub+zip");
	}

	public OCFContainerWriter(OutputStream out, String mime) throws IOException {
		zip = new ZipOutputStream(out);
		try {
			byte[] bytes = mime.getBytes("UTF-8");
			ZipEntry mimetype = new ZipEntry("mimetype");
			mimetype.setMethod(ZipOutputStream.STORED);
			mimetype.setSize(bytes.length);
			mimetype.setCompressedSize(bytes.length);
			CRC32 crc = new CRC32();
			crc.update(bytes);
			mimetype.setCrc(crc.getValue());
			zip.putNextEntry(mimetype);
			zip.write(bytes);
			zip.closeEntry();
		} catch (UnsupportedEncodingException e) {
			// this should not happen
			e.printStackTrace();
		}
	}

	public OutputStream getOutputStream(String name, boolean eligibleForCompression) throws IOException {
		if( eligibleForCompression ) {
			zip.putNextEntry(new ZipEntry(name));			
			return new CompressedEntryStream();		
		} else {
			return new StoredEntryStream(name);
		}
	}

	public void close() throws IOException {
		zip.close();
	}	
}
