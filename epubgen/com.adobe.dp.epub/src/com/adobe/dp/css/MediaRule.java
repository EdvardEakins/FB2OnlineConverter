package com.adobe.dp.css;

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

public class MediaRule {

	Set mediaList;

	Vector statements = new Vector();

	public MediaRule(Set mediaList) {
		this.mediaList = mediaList;
	}

	public void add(Object rule) {
		statements.add(rule);
	}

	public void serialize(PrintWriter out) {
		out.print("@media ");
		String sep = "";
		Iterator it = mediaList.iterator();
		while(it.hasNext()) {
			out.print(sep);
			out.print(it.next());
			sep = ", ";
		}
		out.println(" {");
		Iterator list = statements.iterator();
		while (list.hasNext()) {
			Object stmt = list.next();
			if (stmt instanceof BaseRule) {
				((SelectorRule) stmt).serialize(out);
				out.println();
			} else if (stmt instanceof MediaRule) {
				((MediaRule) stmt).serialize(out);
				out.println();
			}
		}
		out.println("}");
	}

}
