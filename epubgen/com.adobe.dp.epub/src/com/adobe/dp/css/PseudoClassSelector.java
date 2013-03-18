package com.adobe.dp.css;

import java.io.PrintWriter;

public class PseudoClassSelector extends Selector {

	private String name;
	
	PseudoClassSelector(String name) {
		this.name = name;
	}
	
	public ElementMatcher getElementMatcher() {
		if( name.equals("first-child"))
			return new FirstChildElementMatcher(this);
		// TODO others
		return null;
	}

	public int getSpecificity() {
		return 0x100;
	}

	public void serialize(PrintWriter out) {
		out.print(":");
		out.print(name);
	}

}
