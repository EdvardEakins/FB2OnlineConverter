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

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;

public class CLDriver implements ConversionClient {

	Properties settings = new Properties();

	Vector resources = new Vector();

	Vector sources = new Vector();

	public CLDriver() {
		// initialize properties
		settings.put("translit", "true");
		settings.put("embedFonts", "true");
		settings.put("adobeMangling", "true");
		settings.put("pageBreaks", "false");
		settings.put("targetDir", ".");
	}

	void addFile(Vector list, File file) {
		if (!file.canRead()) {
			System.err.println("cannot read file: " + file.getPath());
			System.exit(1);
		}
		if (file.isDirectory()) {
			String[] files = file.list();
			if (files != null) {
				for (int i = 0; i < files.length; i++) {
					addFile(list, new File(file, files[i]));
				}
			}
		} else {
			list.add(file);
		}
	}

	int processParam(int index, String[] args) {
		String arg = args[index++];
		if (arg.startsWith("-")) {
			String a = arg.substring(1);
			if (index >= args.length) {
				System.err.println("missing option value: " + arg);
				System.exit(2);
			}
			String val = args[index++];
			if (a.equals("resource")) {
				addFile(resources, new File(val));
			} else if (settings.get(a) == null) {
				System.err.println("unknown option: " + arg);
				System.exit(2);
			} else {
				settings.put(a, val);
			}
		} else {
			addFile(sources, new File(arg));
		}
		return index;
	}

	public void invoke(String[] args) {

		if (args.length == 0) {
			System.err.println("User:");
			System.err.println(" java -jar epubgen.jar com.adobe.dp.epub.conv.CLDriver [options] srcfile ...");
			System.err.println("Options:");
			Enumeration keys = settings.keys();
			while (keys.hasMoreElements()) {
				System.err.println("\t-" + keys.nextElement());
			}
			System.exit(2);
		}

		int index = 0;
		while (index < args.length) {
			index = processParam(index, args);
		}

		File[] resourceArr = new File[resources.size()];
		resources.copyInto(resourceArr);

		File[] sourceArr = new File[sources.size()];
		sources.copyInto(sourceArr);

		invoke(sourceArr, resourceArr, settings);
	}

	public void invoke(File[] sourceArr, File[] resourceArr, Properties settings) {
		for (int i = 0; i < sourceArr.length; i++)
			invoke(sourceArr[i], resourceArr, settings);
	}

	public void invoke(File source, File[] resourceArr, Properties settings) {
		Iterator it = ConversionService.registeredSerivces();
		while (it.hasNext()) {
			ConversionService service = (ConversionService) it.next();
			if (service.canConvert(source)) {
				try {
					System.err.println("Converting " + source.getPath() + "...");
					service.setProperties(settings);
					StringWriter log = new StringWriter();
					File out = service.convert(source, resourceArr, this, new PrintWriter(log));
					if (out == null) {
						System.err.println("Error log:");
						System.err.println(log.toString());
					}
					System.err.println("Written " + out.getPath());
				} catch (Exception e) {
					e.printStackTrace();
				}
				return;
			}
		}
		System.err.println("No service to convert " + source.getPath());
	}

	public File makeFile(String baseName) {
		String targetDir = settings.getProperty("targetDir");
		File folder = new File(targetDir);
		File file = new File(folder, baseName);
		if (file.exists()) {
			String baseStr;
			String extStr;
			int ext = baseName.indexOf('.');
			if (ext < 0) {
				baseStr = baseName;
				extStr = "";
			} else {
				baseStr = baseName.substring(0, ext);
				extStr = baseName.substring(ext);
			}
			int count = 1;
			while (true) {
				file = new File(folder, baseStr + "-" + count + extStr);
				if (!file.exists())
					break;
				count++;
			}
		}
		return file;
	}

	public void reportIssue(String errorCode) {
		System.err.println(errorCode);
	}

	public void reportProgress(float progress) {
	}

	public static void main(String[] args) {
		(new CLDriver()).invoke(args);
	}
}
