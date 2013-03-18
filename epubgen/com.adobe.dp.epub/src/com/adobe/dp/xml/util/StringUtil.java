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

package com.adobe.dp.xml.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class StringUtil {

	public static String replace(String src, String olds, String news) {
		int index = src.indexOf(olds);
		if (index < 0)
			return src;
		StringBuffer sb = new StringBuffer(src.substring(0, index));
		int olen = olds.length();
		while (true) {
			sb.append(news);
			index += olen;
			int newIndex = src.indexOf(olds, index);
			if (newIndex < 0) {
				sb.append(src.substring(index));
				break;
			}
			sb.append(src.substring(index, newIndex));
			index = newIndex;
		}
		String result = sb.toString();
		return result;
	}
	
	public static String dateToW3CDTF(Date date) {
		SimpleDateFormat w3cdtf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		String s = w3cdtf.format(date);
		int index = s.length() - 2;
		return s.substring(0, index) + ":" + s.substring(index);
	}
	
	public static String toShortW3CDTF(Date d, boolean yearOnly) {
		SimpleDateFormat format = new SimpleDateFormat(yearOnly ? "yyyy" : "yyyy-MM-dd");
		return format.format(d);
	}

	public static int parseRoman(String roman) {
		int acc = 0;
		int lastDigit = 0;
		int len = roman.length();
		for (int i = 0; i < len; i++) {
			char c = roman.charAt(i);
			int digit = 0;
			switch (c) {
			case 'i':
				digit = 1;
				break;
			case 'v':
				digit = 5;
				break;
			case 'x':
				digit = 10;
				break;
			case 'l':
				digit = 50;
				break;
			case 'c':
				digit = 100;
				break;
			case 'd':
				digit = 500;
				break;
			case 'm':
				digit = 1000;
				break;
			default:
				return 0;
			}
			if (lastDigit >= digit) {
				acc += lastDigit;
			} else {
				acc -= lastDigit;
			}
			lastDigit = digit;
		}
		acc += lastDigit;
		return acc;
	}

	public static String printRoman(int n) {
		switch (n) {
		case 1:
			return "i";
		case 2:
			return "ii";
		case 3:
			return "iii";
		case 4:
			return "iv";
		case 5:
			return "v";
		case 9:
			return "ix";
		case 10:
			return "x";
		case 40:
			return "xl";
		case 50:
			return "l";
		case 90:
			return "xc";
		case 100:
			return "c";
		case 400:
			return "cd";
		case 500:
			return "d";
		case 900:
			return "cm";
		case 1000:
			return "m";
		}
		if (5 < n && n <= 8)
			return "v" + printRoman(n - 5);
		if (10 < n && n <= 20)
			return "x" + printRoman(n - 10);
		if (20 < n && n <= 30)
			return "xx" + printRoman(n - 20);
		if (30 < n && n <= 39)
			return "xxx" + printRoman(n - 30);
		if (40 < n && n <= 49)
			return "xl" + printRoman(n - 40);
		if (50 < n && n <= 89)
			return "l" + printRoman(n - 50);
		if (90 < n && n <= 99)
			return "xc" + printRoman(n - 90);
		if (100 < n && n <= 200)
			return "c" + printRoman(n - 100);
		if (200 < n && n <= 300)
			return "cc" + printRoman(n - 200);
		if (300 < n && n < 400)
			return "ccc" + printRoman(n - 300);
		if (400 < n && n < 500)
			return "cd" + printRoman(n - 400);
		if (500 < n && n < 900)
			return "d" + printRoman(n - 500);
		if (900 < n && n < 1000)
			return "cm" + printRoman(n - 900);
		if (1000 < n && n <= 2000)
			return "m" + printRoman(n - 1000);
		if (2000 < n && n <= 3000)
			return "mm" + printRoman(n - 2000);
		if (3000 < n && n < 4000)
			return "mmm" + printRoman(n - 3000);
		return null;
	}	
}
