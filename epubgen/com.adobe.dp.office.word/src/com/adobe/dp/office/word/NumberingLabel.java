package com.adobe.dp.office.word;

public class NumberingLabel {

	String text;

	NumberingDefinitionInstance inst;

	NumberingLevelDefinition level;

	NumberingLabel(NumberingDefinitionInstance inst, NumberingLevelDefinition level, String text) {
		this.inst = inst;
		this.level = level;
		this.text = text;
	}

	public String getText() {
		return text;
	}

	public RunProperties getRunProperties() {
		return level.getRunProperties();
	}

	public ParagraphProperties getParagraphProperties() {
		return level.getParagraphProperties();
	}

	public String getJc() {
		return level.lvlJc;
	}

	public int getNumId() {
		return inst.numId;
	}

	public int getLevel() {
		return level.lvl;
	}
}
