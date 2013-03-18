package com.adobe.dp.css;

import java.io.PrintWriter;

public class SiblingSelector extends Selector {

	private Selector prev;

	private Selector curr;

	SiblingSelector(Selector prev, Selector curr) {
		this.prev = prev;
		this.curr = curr;
	}

	public ElementMatcher getElementMatcher() {
		return new SiblingElementMatcher(this, prev.getElementMatcher(), curr.getElementMatcher());
	}

	public int getSpecificity() {
		return addSpecificity(prev.getSpecificity(), curr.getSpecificity());
	}

	public void serialize(PrintWriter out) {
		prev.serialize(out);
		out.print("+");
		curr.serialize(out);
	}

}
