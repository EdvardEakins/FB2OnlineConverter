package com.adobe.dp.css;

import java.io.PrintWriter;

public class NamedElementSelector extends Selector {

	private String prefix;

	private String ns;

	private String name;

	NamedElementSelector(String prefix, String ns, String name) {
		this.prefix = prefix;
		this.ns = ns;
		this.name = name;
	}

	public ElementMatcher getElementMatcher() {
		return new NamedElementMatcher(this, ns, name);
	}

	public int getSpecificity() {
		return 1;
	}

	public void serialize(PrintWriter out) {
		if (prefix != null) {
			out.print(prefix);
			out.print("|");
		}
		out.print(name);
	}

	public boolean equals(Object obj) {
		if (getClass() != obj.getClass())
			return false;
		NamedElementSelector other = (NamedElementSelector) obj;
		if (prefix != null) {
			if (other.prefix == null || !other.prefix.equals(prefix))
				return false;
		} else if (other.prefix != null)
			return false;
		if (ns != null) {
			if (other.ns == null || !other.ns.equals(ns))
				return false;
		} else if (other.ns != null)
			return false;
		return other.name.equals(name);
	}

	public int hashCode() {
		int code = name.hashCode();
		if (ns != null)
			code += 3 * ns.hashCode();
		if (prefix != null)
			code += 5 * prefix.hashCode();
		return code;
	}

	public boolean hasElementNamespace() {
		return ns != null;
	}

	public String getElementNamespace() {
		return ns;
	}

	public String getElementName() {
		return name;
	}
}
