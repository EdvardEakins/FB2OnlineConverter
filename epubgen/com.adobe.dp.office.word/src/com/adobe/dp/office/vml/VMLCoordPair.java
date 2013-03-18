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

public class VMLCoordPair {

	public VMLCoordPair(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public static VMLCoordPair parse(String str) {
		if (str == null)
			return null;
		return parse(str, 0, 0);
	}

	static VMLCoordPair parse(String str, int x, int y) {
		if (str != null) {
			int index = str.indexOf(',');
			if (index > 0) {
				String xstr = str.substring(0, index);
				try {
					if (xstr.endsWith("pt")) {
						double r = Double.parseDouble(xstr.substring(0, xstr.length() - 2));
						x = (int) Math.round(r * 20);
					} else
						x = Integer.parseInt(xstr);
				} catch (Exception e) {
					e.printStackTrace();
				}
				String ystr = str.substring(index + 1);
				try {
					if (ystr.endsWith("pt")) {
						double r = Double.parseDouble(ystr.substring(0, ystr.length() - 2));
						y = (int) Math.round(r * 20);
					} else
						y = Integer.parseInt(ystr);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return new VMLCoordPair(x, y);
	}

	public final int x;

	public final int y;
}
