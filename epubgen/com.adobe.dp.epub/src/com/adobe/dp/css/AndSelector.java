package com.adobe.dp.css;

import java.io.PrintWriter;

public class AndSelector extends Selector {

	Selector first;

	Selector second;

	AndSelector(Selector first, Selector second) {
		this.first = first;
		this.second = second;
	}

	public ElementMatcher getElementMatcher() {
		return new AndElementMatcher(this, first.getElementMatcher(), second.getElementMatcher());
	}

	public int getSpecificity() {
		return addSpecificity(first.getSpecificity(), second.getSpecificity());
	}

	public void serialize(PrintWriter out) {
		first.serialize(out);
		second.serialize(out);
	}

	public boolean equals(Object other) {
		if( this == other )
			return true;
		if (other.getClass() != getClass())
			return false;
		AndSelector o = (AndSelector) other;
		return o.first.equals(first) && o.second.equals(second);
	}

	public int hashCode() {
		return 3*first.hashCode() + second.hashCode();
	}	
}
