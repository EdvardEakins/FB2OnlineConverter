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
import java.io.OutputStream;
import java.util.Stack;
import java.util.Vector;

import com.adobe.dp.epub.io.DataSource;
import com.adobe.dp.epub.ops.OPSDocument;

public class OPSResource extends Resource {

	
	OPSDocument document;

	OPSResource(Publication epub, String name) {
		super(epub, name, "application/xhtml+xml", null);
	}

	OPSResource(Publication epub, String name, String mediaType) {
		super(epub, name, mediaType, null);
	}

	public OPSDocument getDocument() {
		if (document == null)
			document = new OPSDocument(this);
		return document;
	}

	OPSResource[] splitLargeChapter(Publication pub, int sizeToSplit) {
		if (!mediaType.equals("application/xhtml+xml"))
			return null;
		int targetSize = sizeToSplit;
		Vector res = new Vector();
		res.add(this);
		OPSDocument doc = document;
		while (true) {
			String name = pub.makeUniqueResourceName(getName());
			OPSResource r = pub.createOPSResource(name);
			if (!doc.peelOffBack(r.getDocument(), targetSize)) {
				pub.removeResource(r);
				break;
			}
			doc = r.getDocument();
			res.add(r);
		}
		OPSResource[] result = new OPSResource[res.size()];
		res.copyInto(result);
		return result;
	}

	public void generateTOCFromHeadings(Stack headings, int depth) {
		if (mediaType.equals("application/xhtml+xml"))
			getDocument().getBody().generateTOCFromHeadings(headings, depth);
	}

	public void serialize(OutputStream out) throws IOException {
		getDocument().serialize(out);
	}

	public void load(DataSource data) throws Exception {
		document = new OPSDocument(this);
		document.load(data);
	}
}
