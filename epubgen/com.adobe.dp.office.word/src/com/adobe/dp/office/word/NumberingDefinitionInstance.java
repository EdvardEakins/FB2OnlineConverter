package com.adobe.dp.office.word;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

public class NumberingDefinitionInstance {

	private AbstractNumberingDefinition abstractNumbering;

	Hashtable startOverrides = new Hashtable();

	int numId;

	boolean instantiated;

	// overrides
	Hashtable numberingLevelDefinitions = new Hashtable();

	WordDocument doc;

	NumberingDefinitionInstance(WordDocument doc, int numId) {
		this.doc = doc;
		this.numId = numId;
	}

	static void fillDefsFrom(Hashtable to, Hashtable from) {
		Enumeration keys = from.keys();
		while (keys.hasMoreElements()) {
			Integer key = (Integer) keys.nextElement();
			if (to.get(key) == null) {
				NumberingLevelDefinition levelDef = (NumberingLevelDefinition) from.get(key);
				to.put(key, levelDef.cloneLevel());
			}
		}
	}

	Hashtable getNumberingLevelDefinitions() {
		if (!instantiated) {
			instantiated = true;
			Hashtable defs = abstractNumbering.getNumberingLevelDefinitions(doc);
			fillDefsFrom(numberingLevelDefinitions, defs);
		}
		return numberingLevelDefinitions;
	}

	public Iterator iteratorForLevel(int lvl) {
		Integer key = new Integer(lvl);
		Hashtable defs = getNumberingLevelDefinitions();
		NumberingLevelDefinition levelDef = (NumberingLevelDefinition) defs.get(key);
		if (levelDef == null)
			return null;
		if (levelDef.iterator == null)
			levelDef.iterator = new NumberingLabelIterator(this, levelDef);
		return levelDef.iterator;
	}
	
	void resetLevels(int lvl) {
		Enumeration keys = numberingLevelDefinitions.keys();
		while (keys.hasMoreElements()) {
			Integer key = (Integer) keys.nextElement();
			NumberingLevelDefinition def = (NumberingLevelDefinition) numberingLevelDefinitions.get(key);
			if (def == null)
				continue;
			if (key.intValue() <= lvl) {
				if (def.iterator == null)
					def.iterator = new NumberingLabelIterator(this, def);
				def.iterator.first = false;
			} else {
				if (def.lvlRestart == lvl && def.iterator != null)
					def.iterator.reset();
			}
		}
	}

	String formatText(String text, int maxLevel) {
		int index = text.indexOf('%');
		if (index < 0)
			return text;
		StringBuffer res = new StringBuffer();
		int prevIndex = 0;
		while (true) {
			res.append(text.substring(prevIndex, index));
			index++;
			if (index >= text.length())
				break;
			char c = text.charAt(index);
			if ('1' <= c && c <= '9') {
				int level = c - '1'; // one-based in Microsoft infinite wisdom
				if (level <= maxLevel) {
					NumberingLabelIterator it = (NumberingLabelIterator) iteratorForLevel(level);
					if (it != null)
						res.append(it.getNumberStr());
				}
			} else {
				res.append(c);
			}
			prevIndex = index + 1;
			index = text.indexOf('%', index);
			if (index < 0)
				index = text.length();
		}
		return res.toString();
	}

	void setAbstractNumbering(AbstractNumberingDefinition abstractNumbering) {
		if (abstractNumbering != null) {
			this.abstractNumbering = abstractNumbering;
			abstractNumbering.instances.add(this);
		}
	}
}
