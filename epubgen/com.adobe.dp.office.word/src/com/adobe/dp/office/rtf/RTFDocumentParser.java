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

package com.adobe.dp.office.rtf;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;

public class RTFDocumentParser {

	InputStream in;

	RTFDocument doc;

	int start;

	int stop;

	boolean eof;

	byte[] buffer = new byte[4096];

	final static int MAX_WORD_LEN = 128;

	Level curr;

	Stack levels;

	StringBuffer sbuf;

	ByteArrayOutputStream bbuf;

	Hashtable fonts;

	Hashtable paragraphStyles;

	Hashtable characterStyles;

	static class Level {

		Level() {
			this.encoding = "Cp1252";
		}

		Level(Level prev) {
			this.encoding = prev.encoding;
			this.skipCount = prev.skipCount;
		}

		Vector list = new Vector();

		String encoding;

		int skipCount = 0;
	}

	public RTFDocumentParser(File docFile) throws IOException {
		this.in = new FileInputStream(docFile);
	}

	public RTFDocumentParser(InputStream in) throws IOException {
		this.in = in;
	}

	public RTFDocument parse() throws IOException {
		doc = new RTFDocument();
		parseInternal();
		return doc;
	}

	void fillAtLeast(int count) throws IOException {
		if (buffer.length - start < count) {
			stop -= start;
			System.arraycopy(buffer, start, buffer, 0, stop);
			start = 0;
		}
		while (true) {
			if (stop - start >= count)
				return;
			int r = in.read(buffer, stop, buffer.length - stop);
			if (r <= 0) {
				eof = true;
				return;
			}
			stop += r;
		}
	}

	void flushText0() {
		if (bbuf.size() > 0) {
			String text;
			try {
				text = bbuf.toString(curr.encoding);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				text = "???";
			}
			sbuf.append(text);
			bbuf.reset();
		}
	}

	void processHexChar() {
		char[] h = new char[2];
		h[0] = (char) buffer[start++];
		h[1] = (char) buffer[start++];
		String hs = new String(h);
		injectByte((byte) Integer.parseInt(hs, 16));
	}

	public void injectByte(byte b) {
		bbuf.write(b);
	}

	public void injectText(String text) {
		flushText0();
		sbuf.append(text);
	}

	private void flushText() {
		flushText0();
		if (sbuf.length() > 0) {
			curr.list.add(sbuf.toString());
			sbuf.setLength(0);
		}
	}

	public void injectControl(RTFControl control) {
		flushText();
		curr.list.add(control);
	}

	public void setEncoding(String encoding) {
        if (!Charset.isSupported(encoding)) {
            encoding = "Cp1251";
        }
		if (!curr.encoding.equals(encoding)) {
			flushText0();
			curr.encoding = encoding;
		}
	}

	public void setSkipCount(int n) {
		curr.skipCount = n;
	}

	public void addFont(int index, RTFFont font) {
		fonts.put(new Integer(index), font);
	}

	public RTFFont getFont(int index) {
		return (RTFFont) fonts.get(new Integer(index));
	}

	public void setColorTable(RTFColor[] colors) {
		doc.colorTable = colors;
	}

	public void addParagraphStyle(int index, RTFStyle style) {
		paragraphStyles.put(new Integer(index), style);
	}

	public RTFStyle getParagraphStyle(int index) {
		return (RTFStyle) paragraphStyles.get(new Integer(index));
	}

	public void addCharacterStyle(int index, RTFStyle style) {
		characterStyles.put(new Integer(index), style);
	}

	public RTFStyle getCharacterStyle(int index) {
		return (RTFStyle) characterStyles.get(new Integer(index));
	}

	void parseInternal() throws IOException {
		start = 0;
		stop = 0;
		eof = false;
		levels = new Stack();

		curr = new Level();
		bbuf = new ByteArrayOutputStream();
		sbuf = new StringBuffer();

		fonts = new Hashtable();
		doc.fonts = fonts;

		paragraphStyles = new Hashtable();
		doc.paragraphStyles = paragraphStyles;

		characterStyles = new Hashtable();
		doc.characterStyles = characterStyles;

		StringBuffer cbuf = new StringBuffer();
		while (true) {
			fillAtLeast(MAX_WORD_LEN);
			if (start == stop && eof)
				break;
			byte c = buffer[start];
			if (c == '\n' || c == '\r') {
				start++;
				continue;
			}
			if (c == '{') {
				// System.out.println("+++ start");
				flushText();
				levels.push(curr);
				curr = new Level(curr);
				start++;
				continue;
			}
			if (c == '}') {
				// System.out.println("+++ end");
				flushText();
				Object[] arr = new Object[curr.list.size()];
				curr.list.copyInto(arr);
				curr = (Level) levels.pop();
				RTFGroup group = new RTFGroup(arr);
				RTFControl control = group.getHead();
				if (control == null || !control.getType().parseTimeGroupExec(group, this)) {
					curr.list.add(group);
				}
				start++;
				continue;
			}
			if (c == '\\') {
				if (curr.skipCount > 0)
					curr.skipCount--;
				boolean optional = false;
				start++;
				c = buffer[start];
				if (c == '*') {
					optional = true;
					start++;
					if (buffer[start] != '\\')
						continue;
					start++;
					c = buffer[start];
				}
				RTFControl ctrl;
				if (('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z')) {
					// control word
					cbuf.setLength(0);
					cbuf.append((char) c);
					start++;
					while (start < stop) {
						c = buffer[start];
						if (('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z')) {
							cbuf.append((char) c);
							start++;
						} else
							break;
					}
					RTFControlType type = RTFControlType.getControlTypeByName(cbuf.toString());
					if (c == ' ') {
						// no params
						start++;
						ctrl = new RTFControl(type, optional);
					} else if (c == '-' || ('0' <= c && c <= '9')) {
						// number param
						int param = 0;
						int sign = 1;
						if (c == '-') {
							start++;
							sign = -1;
						}
						while (start < stop) {
							c = buffer[start];
							if ('0' > c || c > '9') {
								if (c == ' ')
									start++;
								break;
							}
							start++;
							param = param * 10 + (c - '0');
						}
						ctrl = new RTFControlWithParam(type, optional, sign * param);
					} else {
						// no params
						ctrl = new RTFControl(type, optional);
					}
				} else {
					RTFControlType type = RTFControlType.getControlTypeByName(Character.toString((char) c));
					start++;
					ctrl = new RTFControl(type, optional);
				}
				if (!ctrl.parseTimeExec(this)) {
					injectControl(ctrl);
				}
				continue;
			}
			int i = start;
			start++;
			while (start < stop) {
				c = buffer[start];
				if (c == '\\' || c == '{' || c == '}' || c == '\r' || c == '\n')
					break;
				start++;
			}
			int count = start - i;
			if (curr.skipCount < count) {
				i += curr.skipCount;
				count -= curr.skipCount;
				curr.skipCount = 0;
				this.bbuf.write(buffer, i, count);
			} else {
				curr.skipCount -= count;
			}
		}
		if (curr.list.size() >= 0) {
			doc.root = (RTFGroup) curr.list.get(0);
		}
	}
}
