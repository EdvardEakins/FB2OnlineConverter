package com.adobe.dp.css;

import com.adobe.dp.xml.util.SMap;

public abstract class ElementMatcher {

	private Selector selector;

	ElementMatcher(Selector selector) {
		this.selector = selector;
	}

	public Selector getSelector() {
		return selector;
	}

	/**
	 * Matches an element with a given namespace, name and attributes
	 * 
	 * @param ns
	 *            element's namespace
	 * @param name
	 *            element's local name
	 * @param attrs
	 *            element's attributes
	 * @return MatchResult if element matches this selector, null otherwise
	 */
	public abstract MatchResult pushElement(String ns, String name, SMap attrs);

	/**
	 * Finish element's processing. Note that pushElement/popElement calls
	 * should correspond to the elements nesting.
	 */
	public abstract void popElement();

}
