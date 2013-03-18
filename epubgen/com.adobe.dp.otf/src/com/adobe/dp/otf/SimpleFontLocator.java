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

package com.adobe.dp.otf;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;

public abstract class SimpleFontLocator extends FontLocator {

	private Hashtable fontMap;

	protected SimpleFontLocator() {
	}
	
	protected void init() {
		fontMap = new Hashtable();
		collectFonts(fontMap);
	}

	abstract protected Iterator getStreamNames();

	abstract protected FontInputStream getStream(String name)
			throws IOException;

	protected void collectFonts(Hashtable map) {
		Iterator streams = getStreamNames();
		while (streams.hasNext()) {
			String name = (String) streams.next();
			try {
				FontInputStream in = getStream(name);
				if (in == null)
					continue;
                OpenTypeFont font;
                try {
                    font = new OpenTypeFont(in, true);
                } finally {
                    in.close();
                }
                if (!font.canEmbedForReading())
					continue;
				FontProperties key = new FontProperties(font.getFamilyName(),
						font.getWeight(), font.getStyle());
				map.put(key, name);
				System.out.println(name + ": " + key);
			} catch (Exception e) {
				// ignore
				// System.out.println(name + ": " + e);
			}
		}
	}

	FontProperties substitute(FontProperties key) {
		
		/*
		if ( key.getFamilyName().equals("Tahoma")) {
			// substitute Tahoma with Calibri
			key = new FontProperties("Calibri", key.getWeight(), key.getStyle());
		}
		*/
		
		if (key.getStyle() == FontPropertyConstants.STYLE_ITALIC
				&& key.getFamilyName().equals("Tahoma")) {
			// workaround: Tahoma does not have italic, replace with Verdana
			key = new FontProperties("Verdana", key.getWeight(), key.getStyle());
		}
		return key;
	}
	
	public boolean hasFont(FontProperties key) {
		key = substitute(key);
		String fileName = (String) fontMap.get(key);
		if (fileName == null) {
			// try a bit bolder...
			FontProperties key1 = new FontProperties(key.getFamilyName(), key
					.getWeight() + 100, key.getStyle());
			fileName = (String) fontMap.get(key1);
			if (fileName == null) {
				// ...and a bit lighter
				key1 = new FontProperties(key.getFamilyName(),
						key.getWeight() - 100, key.getStyle());
				fileName = (String) fontMap.get(key);
				if (fileName == null)
					return false;
			}
		}
		return true;
	}

	public FontInputStream locateFont(FontProperties key) throws IOException {
		key = substitute(key);
		String fileName = (String) fontMap.get(key);
		if (fileName == null) {
			// try a bit bolder...
			FontProperties key1 = new FontProperties(key.getFamilyName(), key
					.getWeight() + 100, key.getStyle());
			fileName = (String) fontMap.get(key1);
			if (fileName == null) {
				// ...and a bit lighter
				key1 = new FontProperties(key.getFamilyName(),
						key.getWeight() - 100, key.getStyle());
				fileName = (String) fontMap.get(key);
				if (fileName == null)
					return null;
			}
		}
		File file = new File(fileName);
		if (!file.canRead())
			return null;
		return getStream(fileName);
	}

}
