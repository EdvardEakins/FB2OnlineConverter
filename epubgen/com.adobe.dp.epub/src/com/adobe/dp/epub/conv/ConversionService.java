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
package com.adobe.dp.epub.conv;

import java.awt.Image;
import java.io.File;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;

public abstract class ConversionService {

	private static final String[] defaultConversionSerices = { "com.adobe.dp.fb2.convert.FB2ConversionService",
			"com.adobe.dp.office.conv.DOCXConversionService", "com.adobe.dp.office.conv.RTFConversionService" };

	static Vector conversionSericeList = new Vector();

	static {
		for (int i = 0; i < defaultConversionSerices.length; i++)
			addConversionService(defaultConversionSerices[i]);
	}

	private static void addConversionService(String className) {
		try {
			conversionSericeList.add(Class.forName(className).newInstance());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static Iterator registeredSerivces() {
		return conversionSericeList.iterator();
	}

	public abstract void setProperties(Properties prop);

	public abstract boolean canConvert(File src);

	public abstract boolean canUse(File src);

	public abstract Image getIcon(File src);

	public abstract File convert(File src, File[] aux, ConversionClient client, PrintWriter log);
}
