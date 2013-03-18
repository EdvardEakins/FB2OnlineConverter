package com.adobe.dp.office.conv;

import com.adobe.dp.css.InlineRule;

public class StylingResult {

	InlineRule containerRule;

	InlineRule elementRule = new InlineRule();

	String containerClassName;

	String elementClassName;

	String elementName;

	InlineRule tableCellRule;
	
	Integer cols;
}
