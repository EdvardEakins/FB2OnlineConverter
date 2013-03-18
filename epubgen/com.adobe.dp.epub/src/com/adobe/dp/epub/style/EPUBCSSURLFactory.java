package com.adobe.dp.epub.style;

import com.adobe.dp.css.CSSURL;
import com.adobe.dp.css.CSSURLFactory;
import com.adobe.dp.epub.opf.Resource;
import com.adobe.dp.epub.opf.ResourceRef;
import com.adobe.dp.epub.util.PathUtil;

public class EPUBCSSURLFactory implements CSSURLFactory {

	Resource owner;

	public EPUBCSSURLFactory(Resource owner) {
		this.owner = owner;
	}

	public CSSURL createCSSURL(String url) {
		url = PathUtil.resolveRelativeReference(owner.getName(), url);
		int hash = url.indexOf('#');
		if (hash < 0)
			return new ResourceURL(owner, owner.getPublication().getResourceRef(url));
		String fragment = url.substring(hash + 1);
		url = url.substring(0, hash);
		ResourceRef ref = owner.getPublication().getResourceRef(url);
		return new ResourceURL(owner, ref.getXRef(fragment));
	}
}
