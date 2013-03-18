package com.adobe.dp.css;

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Set;

public class ImportRule {

	String url;

	Set mediaList;

	public ImportRule(String url, Set mediaList) {
		this.url = url;
		this.mediaList = mediaList;
	}

	public void serialize(PrintWriter out) {
		out.print("@import url(");
		out.print(url);
		out.print(")");
		if (mediaList != null) {
			Iterator it = mediaList.iterator();
			String sep = " ";
			while(it.hasNext()) {
				out.print(sep);
				out.print(it.next());
				sep = ", ";
			}
		}
		out.println(";");
	}

}
