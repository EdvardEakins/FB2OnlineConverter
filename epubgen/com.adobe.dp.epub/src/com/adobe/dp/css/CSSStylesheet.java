package com.adobe.dp.css;

import java.io.PrintWriter;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

public class CSSStylesheet {
	Vector statements = new Vector();

	Hashtable rulesBySelector = new Hashtable();

	public void add(Object rule) {
		if (rule instanceof SelectorRule) {
			SelectorRule srule = (SelectorRule) rule;
			Selector[] selectors = srule.selectors;
			if (selectors.length == 1)
				rulesBySelector.put(selectors[0], rule);
		}
		statements.add(rule);
	}

	public Selector getSimpleSelector(String elementName, String className) {
		NamedElementSelector elementSelector = null;
		if (elementName != null)
			elementSelector = new NamedElementSelector(null, null, elementName);
		if (className == null)
			return elementSelector;
		Selector selector = new ClassSelector(className);
		if (elementSelector != null)
			selector = new AndSelector(elementSelector, selector);
		return selector;
	}

	public SelectorRule getRuleForSelector(Selector selector, boolean create) {
		SelectorRule rule = (SelectorRule) rulesBySelector.get(selector);
		if (rule == null && create) {
			Selector[] selectors = { selector };
			rule = new SelectorRule(selectors);
			add(rule);
		}
		return rule;
	}

	public void serialize(PrintWriter out) {
		Iterator list = statements.iterator();
		while (list.hasNext()) {
			Object stmt = list.next();
			if (stmt instanceof FontFaceRule) {
				((FontFaceRule) stmt).serialize(out);
				out.println();
			} else if (stmt instanceof BaseRule) {
				((SelectorRule) stmt).serialize(out);
				out.println();
			} else if (stmt instanceof MediaRule) {
				((MediaRule) stmt).serialize(out);
				out.println();
			} else if (stmt instanceof ImportRule) {
				((ImportRule) stmt).serialize(out);
				out.println();
			} else if (stmt instanceof PageRule) {
				((PageRule) stmt).serialize(out);
				out.println();
			}
		}
	}

	public Iterator statements() {
		return statements.iterator();
	}
}
