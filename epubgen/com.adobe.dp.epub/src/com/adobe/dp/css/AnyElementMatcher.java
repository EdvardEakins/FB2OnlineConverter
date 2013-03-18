package com.adobe.dp.css;

import com.adobe.dp.xml.util.SMap;

public class AnyElementMatcher extends ElementMatcher {

	public AnyElementMatcher(AnyElementSelector selector) {
		super(selector);
	}

	public void popElement() {
	}

	public MatchResult pushElement(String ns, String name, SMap attrs) {
		return MatchResult.ALWAYS;
	}

}
