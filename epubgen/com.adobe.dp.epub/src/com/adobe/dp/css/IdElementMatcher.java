package com.adobe.dp.css;

import com.adobe.dp.xml.util.SMap;

public class IdElementMatcher extends ElementMatcher {

	private String id;
	
	IdElementMatcher(IdSelector selector, String id) {
		super(selector);
		this.id = id;
	}
	
	public void popElement() {
	}

	public MatchResult pushElement(String ns, String name, SMap attrs) {
		Object id = attrs.get(null, "id");
		return id != null && id.toString().equals(this.id) ? MatchResult.ALWAYS : null;
	}

}
