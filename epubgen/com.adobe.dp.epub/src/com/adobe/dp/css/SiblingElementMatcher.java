package com.adobe.dp.css;

import com.adobe.dp.xml.util.SMap;

public class SiblingElementMatcher extends ElementMatcher {

	private ElementMatcher prev;

	private ElementMatcher curr;

	boolean prevMatched;

	private SparseStack state = new SparseStack();

	public SiblingElementMatcher(Selector selector, ElementMatcher prev, ElementMatcher curr) {
		super(selector);
		this.prev = prev;
		this.curr = curr;
	}

	public void popElement() {
		prev.popElement();
		curr.popElement();
		prevMatched = state.pop() != null;
	}

	public MatchResult pushElement(String ns, String name, SMap attrs) {
		MatchResult p = prev.pushElement(ns, name, attrs);
		if( p != null && p.getPseudoElement() != null )
			return null; // something illegal like foo:first-line + bar
		state.push(p);
		MatchResult c = curr.pushElement(ns, name, attrs);
		if( !prevMatched )
			c = null;
		prevMatched = false;
		return c;
	}

}
