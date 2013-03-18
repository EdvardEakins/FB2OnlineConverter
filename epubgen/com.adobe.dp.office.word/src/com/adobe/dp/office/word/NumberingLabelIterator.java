package com.adobe.dp.office.word;

import java.util.Iterator;

import com.adobe.dp.xml.util.StringUtil;

public class NumberingLabelIterator implements Iterator {

	NumberingDefinitionInstance instance;

	NumberingLevelDefinition level;

	int count;

	boolean first;

	NumberingLabelIterator(NumberingDefinitionInstance instance, NumberingLevelDefinition level) {
		this.instance = instance;
		this.level = level;
		reset();
	}

	void reset() {
		first = true;
		count = level.start;
	}

	public boolean hasNext() {
		return true;
	}

	public String latin(int n) {
		StringBuffer r = new StringBuffer();
		n--;
		char c = (char) ('a' + n % 26);
		int cc = n / 26 + 1;
		for (int i = 0; i < cc; i++)
			r.append(c);
		return r.toString();
	}

	String getNumberStr() {
		String fmt = level.numFmt;
		if (fmt.equals("decimal"))
			return Integer.toString(count);
		if (fmt.equals("upperLetter"))
			return latin(count).toUpperCase();
		if (fmt.equals("lowerLetter"))
			return latin(count);
		if (fmt.equals("upperRoman"))
			return StringUtil.printRoman(count).toUpperCase();
		if (fmt.equals("lowerRoman"))
			return StringUtil.printRoman(count);
		return "";
	}

	public Object next() {
		if (first)
			first = false;
		else
			count++;
		String txt = instance.formatText(level.lvlText, level.lvl);
		// System.out.println("[" + instance.numId + "," + level.lvl + "]: " +
		// level.lvlText + " -> " + txt);
		instance.resetLevels(level.lvl);
		return new NumberingLabel(instance, level, txt);
	}

	public void remove() {
		throw new RuntimeException("not supported");
	}

}
