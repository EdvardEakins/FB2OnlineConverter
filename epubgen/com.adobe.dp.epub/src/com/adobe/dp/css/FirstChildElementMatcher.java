package com.adobe.dp.css;

import com.adobe.dp.xml.util.SMap;

public class FirstChildElementMatcher extends ElementMatcher {

	private boolean firstChild;
	
	FirstChildElementMatcher(PseudoClassSelector selector) {
		super(selector);
	}
	
	public void popElement() {
		firstChild = false;
	}

	public MatchResult pushElement(String ns, String name, SMap attrs) {
		boolean firstChild = this.firstChild;
		this.firstChild = true;
		return firstChild ? MatchResult.ALWAYS : null;
	}

}
