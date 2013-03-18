package com.adobe.dp.otf;

import java.io.IOException;

public class ChainedFontLocator extends FontLocator {

	FontLocator fl1;
	FontLocator fl2;

	public ChainedFontLocator(FontLocator fl1, FontLocator fl2) {
		this.fl1 = fl1;
		this.fl2 = fl2;
	}

	public FontInputStream locateFont(FontProperties key) throws IOException {
		FontInputStream fi = fl1.locateFont(key);
		if( fi == null )
			fi = fl2.locateFont(key);
		return fi;
	}

	public boolean hasFont(FontProperties key) {
		return fl1.hasFont(key) || fl2.hasFont(key);
	}

}
