package com.adobe.dp.css;

import com.adobe.dp.xml.util.SMap;

public class ChildElementMatcher extends ElementMatcher {

	private ElementMatcher parent;

	private ElementMatcher child;

	private boolean parentMatched;

	private SparseStack state = new SparseStack();

	ChildElementMatcher(ChildSelector selector, ElementMatcher parent, ElementMatcher child) {
		super(selector);
		this.parent = parent;
		this.child = child;
	}

	public void popElement() {
		parent.popElement();
		parentMatched = state.pop() != null;
		if (parentMatched)
			child.popElement();
	}

	public MatchResult pushElement(String ns, String name, SMap attrs) {
		boolean parentMatched = this.parentMatched;
		state.push(parentMatched ? Boolean.TRUE : null);
		MatchResult r = parent.pushElement(ns, name, attrs);
		this.parentMatched = r != null && r.getPseudoElement() == null;
		if( parentMatched )
			return child.pushElement(ns, name, attrs);
		return null;
	}

}
