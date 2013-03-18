package com.adobe.dp.office.word;

public class NumberingLevelDefinition {

	int start = 1;

	int lvl = 0;

	int lvlRestart = -1;

	String lvlText = "";

	String lvlJc = "left";

	String numFmt = "decimal";

	ParagraphProperties paragraphProperties;

	RunProperties runProperties;

	NumberingLabelIterator iterator;

	public String getLvlJc() {
		return lvlJc;
	}

	public ParagraphProperties getParagraphProperties() {
		return paragraphProperties;
	}

	public RunProperties getRunProperties() {
		return runProperties;
	}

	NumberingLevelDefinition cloneLevel() {
		NumberingLevelDefinition r = new NumberingLevelDefinition();
		r.start = start;
		r.lvl = lvl;
		r.lvlRestart = lvlRestart;
		r.lvlText = lvlText;
		r.lvlJc = lvlJc;
		r.numFmt = numFmt;
		r.paragraphProperties = paragraphProperties;
		r.runProperties = runProperties;
		return r;
	}

}
