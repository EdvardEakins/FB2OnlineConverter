package com.adobe.dp.css;

import com.adobe.dp.xml.util.SMap;

public class NamedElementMatcher extends ElementMatcher {

	private String ns;

	private String name;

	public NamedElementMatcher(NamedElementSelector selector, String ns, String name) {
		super(selector);
		this.ns = ns;
		this.name = name;
	}

	public void popElement() {
	}

	public MatchResult pushElement(String ns, String name, SMap attrs) {
		return this.name.equals(name) && (this.ns == null || this.ns.equals(ns == null ? "" : ns)) ? MatchResult.ALWAYS
				: null;
	}

}
