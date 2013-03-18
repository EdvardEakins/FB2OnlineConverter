package com.adobe.dp.css;

import java.io.PrintWriter;

public class PageRule extends BaseRule {

	private String pseudo;

	public PageRule(String pseudo) {
		this.pseudo = pseudo;
	}

	public void serialize(PrintWriter out) {
		out.println("@page");
		if (pseudo != null) {
			out.print(" :");
			out.print(pseudo);
		}
		out.println(" {");
		serializeProperties(out, true);
		out.println("}");
	}

}
