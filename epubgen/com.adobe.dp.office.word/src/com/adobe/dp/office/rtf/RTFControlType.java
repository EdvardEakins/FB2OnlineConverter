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

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

public class RTFControlType {

	static Hashtable table = new Hashtable();
	
	private String name;
	
	static public final Set paragraphProps;

	static public final Set characterProps;

	static {
		Set pp = new HashSet();
		pp.add("q_");
		pp.add("sb");
		pp.add("sa");
		pp.add("fi");
		pp.add("li");
		pp.add("ri");
		pp.add("sl");
		pp.add("slmult");
		paragraphProps = pp;

		Set cp = new HashSet();
		cp.add("b");
		cp.add("i");
		cp.add("fs");
		cp.add("cf");
		cp.add("cb");
		cp.add("f");
		cp.add("ul");
		cp.add("sub");
		cp.add("super");
		cp.add("strike");
		cp.add("webhidden");
		characterProps = cp;
		
		new RTFSubstituteControlType("~","\u00A0");
		new RTFSubstituteControlType("{","{");
		new RTFSubstituteControlType("}","}");
		new RTFSubstituteControlType("\\","\\");
		new RTFSubstituteControlType("tab","\t");
		new RTFHexCharControlType("'");
		new RTFEncodingControlType("ansi", "Cp1252");
		new RTFEncodingControlType("mac", "MacRoman");
		new RTFEncodingControlType("pc", "Cp437");
		new RTFEncodingControlType("pca", "Cp850");
		new RTFEncodingControlType("ansicpg", null);
		new RTFEncodingControlType("lang", null);
		new RTFEncodingControlType("langfe", null);
		new RTFEncodingControlType("langnp", null);
		new RTFEncodingControlType("langfenp", null);
		new RTFUnicodeControlType("u");
		new RTFSkipCountControlType("uc");
		new RTFFontTableControlType("fonttbl");
		new RTFStylesheetControlType("stylesheet");
		new RTFColorTableControlType("colortbl");
		new RTFListTableControlType("listtable");
		new RTFListOverrideTableControlType("listoverridetable");
		new RTFMetadataTableControlType("info");
		new RTFPictControlType("pict");
		new RTFResetFormattingControlType("plain", characterProps);
		new RTFResetFormattingControlType("pard", paragraphProps);
		
		// paragraph formatting
		new RTFSpecificFormattingControlType("ql", "q_", "l");
		new RTFSpecificFormattingControlType("qr", "q_", "r");
		new RTFSpecificFormattingControlType("qc", "q_", "c");
		new RTFSpecificFormattingControlType("qj", "q_", "j");
		new RTFFormattingControlType("sb");
		new RTFFormattingControlType("sa");
		new RTFFormattingControlType("fi");
		new RTFFormattingControlType("li");
		new RTFFormattingControlType("ri");
		new RTFFormattingControlType("sl");
		new RTFFormattingControlType("slmult");
		new RTFSpecificFormattingControlType("pagebb");
		new RTFSpecificFormattingControlType("keep");
		new RTFSpecificFormattingControlType("keepn");
		new RTFSpecificFormattingControlType("widctlpar");
		new RTFSpecificFormattingControlType("nowidctlpar", "widctlpar", Boolean.FALSE);
		
		// character formatting
		new RTFFontControlType("f");
		new RTFFormattingControlType("fs");
		new RTFFormattingControlType("cb");
		new RTFFormattingControlType("cf");
		new RTFFormattingControlType("charscalex");
		new RTFFormattingControlType("dn", new Integer(6));
		new RTFFormattingControlType("up", new Integer(6));
		new RTFToggleFormatingControlType("b");
		new RTFToggleFormatingControlType("i");
		new RTFToggleFormatingControlType("ul");
		new RTFToggleFormatingControlType("uldash", "ul", "dash");
		new RTFToggleFormatingControlType("uldashd", "ul", "dashd");
		new RTFToggleFormatingControlType("uldashdd", "ul", "dashdd");
		new RTFToggleFormatingControlType("uldb", "ul", "db");
		new RTFToggleFormatingControlType("ulhwave", "ul", "hwave");
		new RTFToggleFormatingControlType("ulldash", "ul", "ldash");
		new RTFToggleFormatingControlType("ulth", "ul", "th");
		new RTFToggleFormatingControlType("ulthd", "ul", "thd");
		new RTFToggleFormatingControlType("ulthdash", "ul", "thdash");
		new RTFToggleFormatingControlType("ulthdashd", "ul", "thdashd");
		new RTFToggleFormatingControlType("ulthdashdd", "ul", "thdashdd");
		new RTFToggleFormatingControlType("ulthldash", "ul", "thldash");
		new RTFToggleFormatingControlType("ululdbwave", "ul", "uldbwave");
		new RTFToggleFormatingControlType("ulw", "ul", "w");
		new RTFToggleFormatingControlType("ulwave", "ul", "wave");
		new RTFSpecificFormattingControlType("ulnone", "ul", Boolean.FALSE);
		new RTFFormattingControlType("ulc");
		new RTFToggleFormatingControlType("outl");
		new RTFToggleFormatingControlType("shad");
		new RTFToggleFormatingControlType("strike");
		new RTFToggleFormatingControlType("striked", "strike", "d");
		new RTFToggleFormatingControlType("scaps");
		new RTFToggleFormatingControlType("caps");
		new RTFFormattingControlType("kerning");
		new RTFToggleFormatingControlType("v");
		new RTFSpecificFormattingControlType("webhidden");
		new RTFSpecificFormattingControlType("sub");
		new RTFSpecificFormattingControlType("super");
	}
	
	protected RTFControlType(String name) {
		this.name = name;
		table.put(name, this);
	}
	
	public String getName() {
		return name;
	}

	public static RTFControlType getControlTypeByName(String name) {
		synchronized(table) {
			RTFControlType res = (RTFControlType)table.get(name);
			if( res == null )
				res = new RTFGenericControlType(name);
			return res;
		}
	}

	public boolean parseTimeExec(RTFControl ctrl, RTFDocumentParser parser) {
		return false;
	}
	
	public boolean parseTimeGroupExec(RTFGroup group, RTFDocumentParser parser) {
		return false;
	}
	
	public boolean formattingExec(RTFControl ctrl, RTFStyle style) {
		return false;
	}
	
}
