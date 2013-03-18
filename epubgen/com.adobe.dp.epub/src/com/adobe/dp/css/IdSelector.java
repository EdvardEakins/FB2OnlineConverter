package com.adobe.dp.css;

import java.io.PrintWriter;

public class IdSelector extends Selector {

	private String id;

	public IdSelector(String id) {
		this.id = id;
	}

	public ElementMatcher getElementMatcher() {
		return new IdElementMatcher(this, id);
	}

	public int getSpecificity() {
		return 0x10000;
	}

	public void serialize(PrintWriter out) {
		out.print("#");
		out.print(id);
	}

}
