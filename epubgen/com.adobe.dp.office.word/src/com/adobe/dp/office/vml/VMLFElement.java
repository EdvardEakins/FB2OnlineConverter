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

import java.util.StringTokenizer;

import org.xml.sax.Attributes;

public class VMLFElement extends VMLElement {

	String command;

	Object[] args;

	public VMLFElement(Attributes attr) {
		super(attr);
		String eqn = attr.getValue("eqn");
		StringTokenizer tok = new StringTokenizer(eqn, " ");
		int len = tok.countTokens();
		command = tok.nextToken();
		args = new Object[len - 1];
		for (int i = 1; i < len; i++) {
			String t = tok.nextToken();
			char c = t.charAt(0);
			if (c == '@' || c == '#') {
				int v = Integer.parseInt(t.substring(1));
				args[i - 1] = new VMLCallout(t.charAt(0), v);
			} else if ('0' <= c && c <= '9') {
				int v = Integer.parseInt(t);
				args[i - 1] = new Integer(v);
			} else if ('a' <= c && c <= 'z') {
				args[i - 1] = t;
			} else {
				throw new RuntimeException("Unknown stuff: " + t);
			}
		}
	}

	private static int resolve(VMLEnv env, Object arg) {
		if (arg instanceof Integer)
			return ((Integer) arg).intValue();
		if (arg instanceof VMLCallout)
			return env.resolveCallout((VMLCallout) arg);
		if (arg instanceof String)
			return env.resolveEnv((String) arg);
		throw new RuntimeException("Unknown stuff: " + arg);
	}

	public int eval(VMLEnv env) {
		if (command.equals("val")) {
			return resolve(env, args[0]);
		} else if (command.equals("sum")) {
			int v = resolve(env, args[0]);
			int p1 = resolve(env, args[1]);
			int p2 = resolve(env, args[2]);
			return v + p1 - p2;
		} else if (command.equals("prod")) {
			int v = resolve(env, args[0]);
			int p1 = resolve(env, args[1]);
			int p2 = resolve(env, args[2]);
			return (int) Math.round(v * p1 / (double) p2);
		} else if (command.equals("mid")) {
			int v = resolve(env, args[0]);
			int p1 = resolve(env, args[1]);
			return (v + p1) / 2;
		} else if (command.equals("abs")) {
			int v = resolve(env, args[0]);
			return v > 0 ? v : -v;
		} else if (command.equals("min")) {
			int v = resolve(env, args[0]);
			int p1 = resolve(env, args[1]);
			return v > p1 ? p1 : v;
		} else if (command.equals("max")) {
			int v = resolve(env, args[0]);
			int p1 = resolve(env, args[1]);
			return v > p1 ? v : p1;
		} else if (command.equals("if")) {
			int v = resolve(env, args[0]);
			int p1 = resolve(env, args[1]);
			int p2 = resolve(env, args[2]);
			return v > 0 ? p1 : p2;
		} else if (command.equals("mod")) {
			int v = resolve(env, args[0]);
			int p1 = resolve(env, args[1]);
			int p2 = resolve(env, args[2]);
			return (int) Math.floor(Math.sqrt(v * (double) v + p1 * (double) p1 + p2 * (double) p2));
		} else if (command.equals("atan2")) {
			int v = resolve(env, args[0]);
			int p1 = resolve(env, args[1]);
			return (int) Math.floor(0x10000 * Math.toDegrees(Math.atan2(p1, v)));
		} else if (command.equals("sin")) {
			int v = resolve(env, args[0]);
			int p1 = resolve(env, args[1]);
			return (int) Math.floor(v * Math.sin(Math.toRadians(p1 / (double) 0x10000)));
		} else if (command.equals("cos")) {
			int v = resolve(env, args[0]);
			int p1 = resolve(env, args[1]);
			return (int) Math.floor(v * Math.cos(Math.toRadians(p1 / (double) 0x10000)));
		} else if (command.equals("cosatan2")) {
			int v = resolve(env, args[0]);
			int p1 = resolve(env, args[1]);
			int p2 = resolve(env, args[2]);
			return (int) Math.floor(v * Math.cos(Math.atan2(p2, p1)));
		} else if (command.equals("sinatan2")) {
			int v = resolve(env, args[0]);
			int p1 = resolve(env, args[1]);
			int p2 = resolve(env, args[2]);
			return (int) Math.floor(v * Math.sin(Math.atan2(p2, p1)));
		} else if (command.equals("sqrt")) {
			int v = resolve(env, args[0]);
			return (int) Math.floor(Math.sqrt(v));
		} else if (command.equals("sumangle")) {
			int v = resolve(env, args[0]);
			int p1 = resolve(env, args[1]);
			int p2 = resolve(env, args[2]);
			return v + p1 * 0x10000 - p2 * 0x10000;
		} else if (command.equals("ellipse")) {
			int v = resolve(env, args[0]);
			int p1 = resolve(env, args[1]);
			int p2 = resolve(env, args[2]);
			double r = v / (double) p1;
			return (int) Math.floor(p2 * Math.sqrt(1 - r * r));
		} else if (command.equals("tan")) {
			int v = resolve(env, args[0]);
			int p1 = resolve(env, args[1]);
			return (int) Math.floor(v * Math.tan(Math.toRadians(p1 / (double) 0x10000)));
		} else {
			throw new RuntimeException("unknown command: " + command);
		}
	}
}
