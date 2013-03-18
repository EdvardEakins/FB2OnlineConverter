package com.adobe.dp.css;

import com.adobe.dp.xml.util.SMap;

public class ClassElementMatcher extends ElementMatcher {

	private String className;

	private static final String fb2NS = "http://www.gribuser.ru/xml/fictionbook/2.0";

	ClassElementMatcher(ClassSelector selector, String className) {
		super(selector);
		this.className = className;
	}

	public void popElement() {
	}

	public static String getClassAttribute(String ns, String name) {
		if (ns.equals(fb2NS)) {
			// By FB2's designer's infinite wisdom, CSS class is allowed only
			// on an element named "style" and is given by the attribute named
			// "name". Go figure.
			if (name.equals("style"))
				return "name";
			return null;
		}
		// assume it is "class" (holds true for XHTML and SVG, but questionable
		// for other XML dialects)
		return "class";
	}

	public MatchResult pushElement(String ns, String name, SMap attrs) {
		if (attrs == null)
			return null;
		String classAttrName = getClassAttribute(ns, name);
		if (classAttrName == null)
			return null;
		Object classValue = attrs.get(null, classAttrName);
		if (classValue == null)
			return null;
		return AttributeElementMatcher.isInList(classValue, className) ? MatchResult.ALWAYS : null;
	}

}
