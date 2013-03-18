package com.adobe.dp.css;

public class CSSParsingError {
	private int line;

	private String err;

	public CSSParsingError(int line, String err) {
		super();
		this.line = line;
		this.err = err;
	}

	public int getLine() {
		return line;
	}

	public String getError() {
		return err;
	}

}
