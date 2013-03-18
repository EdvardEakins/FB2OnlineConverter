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
package com.adobe.dp.office.vml;

import java.util.Vector;

public class VMLPathSegment {

	final public String command;

	final public Object[] args;

	private final static int INIT = 0;

	private final static int CALLOUT = 1;

	private final static int ARG = 2;

	private final static int COMMAND = 3;

	private final static int COMMA = 4;

	public VMLPathSegment(String command, Object[] args) {
		this.command = command;
		this.args = args;
	}

	public static VMLPathSegment[] parse(String path) {
		if (path == null)
			return null;
		Vector segments = new Vector();
		Vector args = new Vector();
		StringBuffer command = new StringBuffer();
		int index = 0;
		int len = path.length();
		int state = INIT;
		while (index <= len) {
			char c = (index == len ? 'e' : path.charAt(index));
			if (('0' <= c && c <= '9') || c == '-') {
				// read numerical arg
				int start = index;
				while (index < len) {
					c = path.charAt(++index);
					if ('0' > c || c > '9')
						break;
				}
				int arg = Integer.parseInt(path.substring(start, index));
				if (state == CALLOUT)
					args.add(new VMLCallout('@', arg));
				else
					args.add(new Integer(arg));
				state = ARG;
			} else if (c == ',') {
				index++;
				if (state == COMMA || state == COMMAND)
					args.add(new Integer(0));
				state = COMMA;
			} else if ('a' <= c && c <= 'z') {
				if (command.length() > 0) {
					char f = command.charAt(0);
					if (command.length() == 2 || (f != 'h' && f != 'n' && f != 'a' && f != 'w' && f != 'q')) {
						String cmd = command.toString();
						if (state == COMMA)
							args.add(new Integer(0));
						Object[] cmdargs = new Object[args.size()];
						args.copyInto(cmdargs);
						segments.add(new VMLPathSegment(cmd, cmdargs));
						args.setSize(0);
						command.delete(0, command.length());
					}
				}
				command.append(c);
				state = COMMAND;
				index++;
			} else if (c == '@') {
				state = CALLOUT;
				index++;
			} else if (c == ' ') {
				index++;
			} else {
				index++;
				System.out.println("unknown char in path string: " + c);
			}
		}
		VMLPathSegment[] segs = new VMLPathSegment[segments.size()];
		segments.copyInto(segs);
		return segs;
	}
}
