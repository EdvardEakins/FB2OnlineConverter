package com.adobe.dp.css;

import com.adobe.dp.xml.util.SMap;

public class AndElementMatcher extends ElementMatcher {

	ElementMatcher first;

	ElementMatcher second;

	AndElementMatcher(AndSelector selector, ElementMatcher first, ElementMatcher second) {
		super(selector);
		if (first == null || second == null)
			throw new NullPointerException();
		this.first = first;
		this.second = second;
	}

	public void popElement() {
		second.popElement();
		first.popElement();
	}

	public MatchResult pushElement(String ns, String name, SMap attrs) {
		MatchResult f = first.pushElement(ns, name, attrs);
		MatchResult s = second.pushElement(ns, name, attrs);
		if (f == null || s == null)
			return null;
		if (f.getPseudoElement() == null)
			return s;
		if (s.getPseudoElement() == null)
			return f;
		return null; // something illegal like :first-letter:before
	}

}
