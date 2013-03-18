package com.adobe.dp.css;

import java.io.PrintWriter;

public class DescendantSelector extends Selector {

	private Selector ancestor;

	private Selector descendant;

	DescendantSelector(Selector ancestor, Selector descendant) {
		this.descendant = descendant;
		this.ancestor = ancestor;
	}

	public ElementMatcher getElementMatcher() {
		return new DescendantElementMatcher(this, ancestor.getElementMatcher(), descendant.getElementMatcher());
	}

	public int getSpecificity() {
		return addSpecificity(ancestor.getSpecificity(), descendant.getSpecificity());
	}

	public void serialize(PrintWriter out) {
		ancestor.serialize(out);
		out.print(" ");
		descendant.serialize(out);
	}

}
