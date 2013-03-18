package com.adobe.dp.epub.style;

import com.adobe.dp.css.CSSURL;
import com.adobe.dp.epub.opf.Resource;
import com.adobe.dp.epub.opf.ResourceRef;
import com.adobe.dp.epub.ops.XRef;

public class ResourceURL extends CSSURL {

	Resource owner;

	ResourceRef target;

	XRef xref;

	public ResourceURL(Resource owner, ResourceRef target) {
		this.owner = owner;
		this.target = target;
	}

	public ResourceURL(Resource owner, XRef xref) {
		this.owner = owner;
		this.xref = xref;
	}

	public String getURI() {
		if (xref != null)
			return xref.makeReference(owner);
		else
			return owner.makeReference(target.getResourceName(), null);
	}

}
