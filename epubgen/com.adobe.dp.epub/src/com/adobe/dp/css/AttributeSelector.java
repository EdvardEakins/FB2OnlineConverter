package com.adobe.dp.css;

import java.io.PrintWriter;

public class AttributeSelector extends Selector {

	String prefix;

	String ns;

	String attr;

	String op;

	CSSValue value;

	AttributeSelector(String prefix, String ns, String attr, String op, CSSValue value) {
		this.prefix = prefix;
		this.ns = ns;
		this.attr = attr;
		this.op = op;
		this.value = value;
	}

	public ElementMatcher getElementMatcher() {
		return new AttributeElementMatcher(this, ns, attr, op, value);
	}

	public int getSpecificity() {
		return 0x100;
	}

	public void serialize(PrintWriter out) {
		out.print("[");
		if (value == null) {
			if (prefix != null) {
				out.print(prefix);
				out.print("|");
			}
			out.print(attr);
		} else {
			if (prefix != null) {
				out.print(prefix);
				out.print("|");
			}
			out.print(attr);
			out.print(op);
			value.serialize(out);
		}
		out.print("]");
	}
	
	public boolean equals(Object obj) {
		if (getClass() != obj.getClass())
			return false;
		AttributeSelector other = (AttributeSelector) obj;
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
		if (value != null) {
			if (other.value == null || !other.value.equals(value))
				return false;
		} else if (other.value != null)
			return false;
		return other.attr.equals(attr) && other.op.equals(op);
	}

	public int hashCode() {
		int code = attr.hashCode() + 11*op.hashCode();
		if (value != null)
			code += value.hashCode();
		if (ns != null)
			code += 3 * ns.hashCode();
		if (prefix != null)
			code += 5 * prefix.hashCode();
		return code;
	}	

}
