package com.adobe.dp.css;

import java.io.PrintWriter;
import java.io.StringWriter;

public abstract class CSSValue {

	public abstract void serialize(PrintWriter out);

	public String toCSSString() {
		StringWriter out = new StringWriter();
		PrintWriter pout = new PrintWriter(out);
		serialize(pout);
		pout.flush();
		return out.toString();
	}

	public String toString() {
		return toCSSString();
	}
}
