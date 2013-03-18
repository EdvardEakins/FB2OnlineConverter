package com.adobe.dp.css;

import com.adobe.dp.xml.util.SMap;

public class AttributeElementMatcher extends ElementMatcher {

	String ns;

	String attr;

	String op;

	Object value;

	AttributeElementMatcher(Selector selector, String ns, String attr, String op, Object value) {
		super(selector);
		this.ns = ns;
		this.attr = attr;
		this.op = op;
		this.value = value;
	}

	public void popElement() {
	}

	public static boolean isInList(Object list, String value) {
		String sv = list.toString();
		int index = sv.indexOf(' ');
		if (index < 0)
			return value.equals(sv);
		else {
			int prevIndex = 0;
			String v = value.toString();
			int vlen = v.length();
			int svlen = sv.length();
			while (true) {
				if (index == prevIndex + vlen && sv.regionMatches(prevIndex, v, 0, vlen))
					return true;
				prevIndex = index + 1;
				if (prevIndex > svlen)
					return false;
				index = sv.indexOf(' ', prevIndex);
				if (index < 0)
					index = svlen;
			}
		}
	}

	public MatchResult pushElement(String ns, String name, SMap attrs) {
		if (attrs == null)
			return null;
		Object val = attrs.get(this.ns, this.attr);
		if (val == null)
			return null;
		if (value == null) {
			return MatchResult.ALWAYS;
		} else if (op.equals("=")) {
			return value.toString().equals(val.toString()) ? MatchResult.ALWAYS : null;
		} else if (op.equals("~=")) {
			return isInList(val, value.toString()) ? MatchResult.ALWAYS : null;
		} else if (op.equals("|=")) {
			String sv = val.toString();
			String v = value.toString();
			return v.equals(sv) || sv.startsWith(v + "-") ? MatchResult.ALWAYS : null;
		} else if (op.equals("^=")) {
			String sv = val.toString();
			String v = value.toString();
			return sv.startsWith(v) ? MatchResult.ALWAYS : null;
		} else if (op.equals("$=")) {
			String sv = val.toString();
			String v = value.toString();
			return sv.endsWith(v) ? MatchResult.ALWAYS : null;
		} else if (op.equals("*=")) {
			String sv = val.toString();
			String v = value.toString();
			return sv.indexOf(v) >= 0 ? MatchResult.ALWAYS : null;
		} else {
			throw new RuntimeException("Unknown op: " + op);
		}
	}

}
