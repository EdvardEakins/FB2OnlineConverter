package com.adobe.dp.css;

import java.io.PrintWriter;

public class CSSName extends CSSValue {

	private String name;

	public CSSName(String name) {
		this.name = name;
	}

	public void serialize(PrintWriter out) {
		out.print(name);
	}

	public int hashCode() {
		return name.hashCode();
	}

	public String toString() {
		return name;
	}

	public boolean equals(Object obj) {
		return obj.getClass() == getClass() && ((CSSName) obj).name.equals(name);
	}

}
