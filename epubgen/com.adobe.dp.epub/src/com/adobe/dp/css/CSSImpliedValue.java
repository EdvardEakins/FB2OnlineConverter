package com.adobe.dp.css;

import java.io.PrintWriter;

public class CSSImpliedValue extends CSSValue {

	CSSValue wrapped;

	public CSSImpliedValue(CSSValue wrapped) {
		this.wrapped = wrapped;
	}

	public CSSValue getWrapped() {
		return wrapped;
	}

	public void serialize(PrintWriter out) {
		wrapped.serialize(out);
	}

	public int hashCode() {
		return wrapped.hashCode() + 189;
	}

	public boolean equals(Object other) {
		if (other.getClass() == getClass()) {
			CSSImpliedValue o = (CSSImpliedValue) other;
			return o.wrapped.equals(wrapped);
		}
		return false;
	}
}
