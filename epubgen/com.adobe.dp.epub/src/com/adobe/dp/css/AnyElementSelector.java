package com.adobe.dp.css;

import java.io.PrintWriter;

public class AnyElementSelector extends Selector {

	public ElementMatcher getElementMatcher() {
		return new AnyElementMatcher(this);
	}

	public void serialize(PrintWriter out) {
		out.print("*");
	}
	
	public int getSpecificity() {
		return 0;
	}
	
}
