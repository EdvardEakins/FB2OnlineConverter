package com.adobe.dp.css;

import com.adobe.dp.xml.util.SMap;

public class PseudoElementMatcher extends ElementMatcher {

	MatchResult result;
	
	PseudoElementMatcher(PseudoElementSelector selector, String pseudoElement) {
		super(selector);
		result = new MatchResult(pseudoElement);
	}
	
	public void popElement() {
	}

	public MatchResult pushElement(String ns, String name, SMap attrs) {
		return result;
	}

}
