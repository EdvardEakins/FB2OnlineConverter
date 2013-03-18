package com.adobe.dp.css;

import java.io.PrintWriter;

public class SelectorRule extends BaseRule {

	Selector[] selectors;
	
	SelectorRule( Selector[] selectors ) {
		this.selectors = selectors;
	}
	
	public void serialize(PrintWriter out) {
		String sep = "";
		for( int i = 0 ; i < selectors.length ; i++ ) {
			out.print(sep);
			selectors[i].serialize(out);
			sep = ", ";
		}
		out.println(" {");
		serializeProperties(out, true);
		out.println("}");
	}

}
