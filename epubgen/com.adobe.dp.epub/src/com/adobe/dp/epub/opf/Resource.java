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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.adobe.dp.epub.io.DataSource;

/**
 * This class represents an arbitrary resource in a Publication. Typically one
 * of this class subclasses is used.
 */
public class Resource {

	String id;

	String name;

	String mediaType;

	DataSource source;

	Publication epub;

	Resource(Publication epub, String name, String type, DataSource source) {
		this.epub = epub;
		this.mediaType = type;
		this.name = name;
		this.source = source;
	}

	/**
	 * Get resource name, including complete path from the Publication container
	 * root
	 * 
	 * @return resource name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Resource MIME type, e.g. "image/jpeg"
	 * 
	 * @return MIME type
	 */
	public String getMediaType() {
		return mediaType;
	}

	/**
	 * Make a relative URL that points to another resource in the same
	 * Publication
	 * 
	 * @param target
	 *            target Resource
	 * @param fragment
	 *            fragment identifier, null if none
	 * @return relative URL
	 */
	public String makeReference(String targetName, String fragment) {
		StringBuffer ref = new StringBuffer();
		int index = 0;
		while (true) {
			int m = name.indexOf('/', index);
			int t = targetName.indexOf('/', index);
			if (m < 0)
				break;
			if (m != t)
				break;
			String mn = name.substring(index, m);
			String tn = targetName.substring(index, t);
			if (!mn.equals(tn))
				break;
			index = m + 1;
		}
		String tail = targetName.substring(index);
		while (true) {
			int m = name.indexOf('/', index);
			if (m < 0)
				break;
			ref.append("../");
			index = m + 1;
		}
		ref.append(tail);
		if (fragment != null) {
			ref.append("#");
			ref.append(fragment);
		}
		return ref.toString();
	}

	/**
	 * Serialize this resource into OutputStream
	 * 
	 * @param out
	 *            OutputStream
	 * @throws IOException
	 *             if I/O error occurs while writing
	 */
	public void serialize(OutputStream out) throws IOException {
		try {
			byte[] buffer = new byte[4096];
			int len;
			InputStream in = source.getInputStream();
			while ((len = in.read(buffer)) > 0) {
				out.write(buffer, 0, len);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		out.close();
	}

	/**
	 * Indicates if this resource can be compressed. Most resources can and
	 * should be compressed when packaged into OCF container. However, some
	 * files, such as bitmap images or video file may be already compressed and
	 * should not be compressed again.
	 * 
	 * @return true if this resource should be compressed, false otherwise
	 */
	public boolean canCompress() {
		return true;
	}

	public ResourceRef getResourceRef() {
		return epub.getResourceRef(name);
	}

	public Publication getPublication() {
		return epub;
	}
}
