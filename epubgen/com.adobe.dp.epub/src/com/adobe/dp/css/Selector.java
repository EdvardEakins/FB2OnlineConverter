package com.adobe.dp.css;

import java.io.PrintWriter;

public abstract class Selector {

	public abstract ElementMatcher getElementMatcher();

	public abstract void serialize(PrintWriter out);

	public abstract int getSpecificity();

	static public int addSpecificity(int s1, int s2) {
		return s1 + s2;
	}
}
