package com.adobe.dp.css;

public class MatchResult {
	String pseudoElement;

	private MatchResult() {
	}

	MatchResult(String pseudoElement) {
		this.pseudoElement = pseudoElement;
	}
	
	public String getPseudoElement() {
		return pseudoElement;
	}
	
	public final static MatchResult ALWAYS = new MatchResult();
}
