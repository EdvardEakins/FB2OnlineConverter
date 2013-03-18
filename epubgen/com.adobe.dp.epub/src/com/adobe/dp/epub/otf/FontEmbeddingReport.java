package com.adobe.dp.epub.otf;

import java.util.Iterator;

public interface FontEmbeddingReport {

	public Iterator missingFonts();

	public Iterator prohibitedFonts();

	public Iterator usedFonts();

}
