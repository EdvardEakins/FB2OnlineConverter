package com.adobe.dp.office.vml;

import org.xml.sax.Attributes;

public class VMLRectElement extends VMLElement {
	VMLRectElement(Attributes attr) {
		super(attr);
	}
	
	public float[] getTextBox() {
		float width = getNumberValue(style, "width", 0);
		float height = getNumberValue(style, "height", 0);
		float[] result = { -width/2, -height/2, width/2, height/2 };
		return result;
	}
	
}
