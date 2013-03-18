package com.adobe.dp.css;

import java.io.PrintWriter;

public class FontFaceRule extends BaseRule {
	
	public void serialize(PrintWriter out) {
		out.println("@font-face {");
		serializeProperties(out, true);
		out.println("}");
	}

}
