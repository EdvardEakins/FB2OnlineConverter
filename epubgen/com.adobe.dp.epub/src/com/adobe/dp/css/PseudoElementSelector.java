package com.adobe.dp.css;

import java.io.PrintWriter;

public class PseudoElementSelector extends Selector {

	private String name;
	
	PseudoElementSelector(String name) {
		this.name = name;
	}
	
	public ElementMatcher getElementMatcher() {
		return new PseudoElementMatcher(this, name);
	}

	public int getSpecificity() {
		return 1;
	}

	public void serialize(PrintWriter out) {
		out.print(":");
		out.print(name);
	}

}
