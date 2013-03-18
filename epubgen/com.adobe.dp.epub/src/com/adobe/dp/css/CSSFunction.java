package com.adobe.dp.css;

import java.io.PrintWriter;

public class CSSFunction extends CSSValue {

	private final String ident;
	private final CSSValue[] params;
	
	CSSFunction( String ident, CSSValue[] params ) {
		this.ident = ident;
		this.params = params;
	}
	
	public void serialize(PrintWriter out) {
		out.print(ident);
		out.print('(');
		String sep = "";
		for( int i = 0 ; i < params.length ; i++ ) {
			out.print(sep);
			params[i].serialize(out);
			sep = ", ";
		}
		out.print(')');
	}
	
	public boolean equals(Object other) {
		if( this == other )
			return true;
		if (other.getClass() != getClass())
			return false;
		CSSFunction o = (CSSFunction) other;
		if (!o.ident.equals(ident) || o.params.length != params.length)
			return false;
		for (int i = 0; i < params.length; i++) {
			if (!params[i].equals(o.params[i]))
				return false;
		}
		return true;
	}

	public int hashCode() {
		int code = ident.hashCode();
		for (int i = 0; i < params.length; i++) {
			code += (i+2) * params[i].hashCode();
		}
		return code;
	}	
}
