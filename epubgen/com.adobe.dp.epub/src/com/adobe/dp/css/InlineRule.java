package com.adobe.dp.css;

import java.io.PrintWriter;

public class InlineRule extends BaseRule {

	public InlineRule() {
	}
	
	protected InlineRule(InlineRule other) {
		super(other);
	}
	
	public void serialize(PrintWriter out) {
		serializeProperties(out, false);
	}
	
	public InlineRule cloneObject() {
		return new InlineRule(this);
	}

	public boolean equals(Object arg) {
		if( getClass() != arg.getClass() )
			return false;
		return properties.equals(((InlineRule)arg).properties);
	}

	public int hashCode() {
		return properties.hashCode();
	}
	
	
	
}
