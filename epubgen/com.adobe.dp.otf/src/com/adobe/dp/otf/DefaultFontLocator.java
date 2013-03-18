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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

public class DefaultFontLocator extends SimpleFontLocator {

    private static Map<String, FontLocator> instances = new HashMap<String, FontLocator>();


    final String[] dirs;
    public static final String BUILT_IN_DIRS = "C:\\windows\\fonts;/Library/Fonts";

	private DefaultFontLocator(String[] dirs) {
		this.dirs = dirs;
		init();
	}

	protected Iterator getStreamNames() {
		Vector fonts = new Vector();
		String[] fontDirs = dirs;
		for (int i = 0; i < fontDirs.length; i++) {
			File dir = new File(fontDirs[i]);
			if (!dir.isDirectory())
				continue;
			String[] files = dir.list();
			if (files == null)
				continue;
			for (int k = 0; k < files.length; k++) {
				File file = new File(dir, files[k]);
				if (!file.canRead())
					continue;
				fonts.add(file.getAbsolutePath());
			}
		}
		return fonts.iterator();
	}

	protected FontInputStream getStream(String name) throws IOException {
		return new FileFontInputStream(new File(name));
	}

	public static synchronized FontLocator getInstance(String dirs) {
        FontLocator fontLocator = instances.get(dirs);
        if (fontLocator == null) {
            fontLocator = new DefaultFontLocator(dirs.split(";"));
            instances.put(dirs, fontLocator);
        }
        return fontLocator;
	}
}
