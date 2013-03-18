package com.adobe.dp.css;

import java.io.PrintWriter;

public class ClassSelector extends Selector {

	String className;

	ClassSelector(String className) {
		this.className = className;
	}

	public ElementMatcher getElementMatcher() {
		return new ClassElementMatcher(this, className);
	}

	public int getSpecificity() {
		return 0x100;
	}

	public void serialize(PrintWriter out) {
		out.print('.');
		out.print(className);
	}

	public boolean equals(Object arg) {
		if (getClass() != arg.getClass())
			return false;
		return className.equals(((ClassSelector) arg).className);
	}

	public int hashCode() {
		return className.hashCode();
	}

}
