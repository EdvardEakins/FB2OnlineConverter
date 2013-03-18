package com.adobe.dp.office.conv;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import com.adobe.dp.css.CSSImpliedValue;
import com.adobe.dp.css.CSSLength;
import com.adobe.dp.css.CSSName;
import com.adobe.dp.css.CSSQuotedString;
import com.adobe.dp.css.CSSValue;
import com.adobe.dp.css.CSSValueList;
import com.adobe.dp.css.InlineRule;
import com.adobe.dp.office.types.BorderSide;
import com.adobe.dp.office.types.FontFamily;
import com.adobe.dp.office.types.Paint;
import com.adobe.dp.office.types.RGBColor;
import com.adobe.dp.office.word.BaseProperties;
import com.adobe.dp.office.word.NumberingLabel;
import com.adobe.dp.office.word.ParagraphProperties;
import com.adobe.dp.office.word.RunProperties;
import com.adobe.dp.office.word.Style;
import com.adobe.dp.office.word.TableCellProperties;
import com.adobe.dp.office.word.TableProperties;

public class StyleConverter {

	Style documentDefaultParagraphStyle;

	double defaultFontSize = 22.0;

	boolean usingPX;

	private static final int RUN_COMPLEX = 1;

	private static final int RUN_STYLE = 2;

	private static final int RUN_BOLD = 4;

	private static final int RUN_ITALIC = 8;

	private static final int RUN_SUPER = 0x10;

	private static final int RUN_SUB = 0x20;

	StyleConverter(boolean usingPX) {
		this.usingPX = usingPX;
	}

	void setDocumentDefaultParagraphStyle(Style documentDefaultParagraphStyle) {
		this.documentDefaultParagraphStyle = documentDefaultParagraphStyle;
	}

	boolean usingPX() {
		return usingPX;
	}

	void setDefaultFontSize(double defaultFontSize) {
		this.defaultFontSize = defaultFontSize;
	}

	private String mapToElement(String styleId) {
		if (styleId != null) {
			if (styleId.equals("Title"))
				return "h1";
			if (styleId.equals("Heading1"))
				return "h1";
			if (styleId.equals("Heading2"))
				return "h2";
			if (styleId.equals("Heading3"))
				return "h3";
			if (styleId.equals("Heading4"))
				return "h4";
			if (styleId.equals("Heading5"))
				return "h5";
			if (styleId.equals("Heading6"))
				return "h6";
		}
		return null;
	}

	private int getRunMask(RunProperties rp) {
		int result = 0;
		if (rp == null)
			return 0;
		if (rp.getRunStyle() != null)
			result = RUN_STYLE;
		if (!rp.isEmpty()) {
			Iterator it = rp.properties();
			while (it.hasNext()) {
				String prop = (String) it.next();
				if (prop.equals("b")) {
					Object val = rp.get("b");
					if (val.equals(Boolean.TRUE))
						result |= RUN_BOLD;
				} else if (prop.equals("i")) {
					Object val = rp.get("i");
					if (val.equals(Boolean.TRUE))
						result |= RUN_ITALIC;
				} else if (prop.equals("vertAlign")) {
					Object val = rp.get("vertAlign");
					if (val.equals("superscript"))
						result |= RUN_SUPER;
					else if (val.equals("subscript"))
						result |= RUN_SUB;
				} else {
					result |= RUN_COMPLEX;
				}
			}
		}
		return result;
	}

	static void setIfNotPresent(InlineRule rule, String name, CSSValue value) {
		Object val = rule.get(name);
		if (val == null || val instanceof CSSImpliedValue)
			rule.set(name, value);
	}

	CSSValueList convertBorderSide(BorderSide side) {
		String type = side.getType();
		if (type.equals("single"))
			type = "solid";
		else
			type = "solid";
		Paint paint = side.getColor();
		CSSValue color = (paint instanceof RGBColor ? ((RGBColor) paint).toCSSValue() : new CSSName("black"));
		double width = side.getWidth() / 8.0;
		if (width < 1)
			width = 1;
		CSSValue[] list = { new CSSLength(width, "px"), new CSSName(type), color };
		return new CSSValueList(' ', list);
	}

	private CSSValue getFontFamilyString(FontFamily family) {
		String name = family.getName();
		Vector list = new Vector();
		if (name.indexOf(' ') >= 0) {
			list.add(new CSSQuotedString(name));
		} else {
			list.add(new CSSName(name));
		}
		String backupName = null;
		String shape = family.getFamily();
		String pitch = family.getPitch();
		if (pitch != null && pitch.equals("fixed")) {
			backupName = "monospace";
		} else if (shape != null) {
			if (shape.equals("roman"))
				backupName = "serif";
			else if (shape.equals("swiss"))
				backupName = "sans-serif";
		}
		if (backupName != null) {
			list.add(new CSSName(backupName));
		}
		if (list.size() == 1)
			return (CSSValue) list.get(0);
		CSSValue[] arr = new CSSValue[list.size()];
		list.copyInto(arr);
		return new CSSValueList(',', arr);
	}

	public boolean convertLabelToProperty(NumberingLabel label, InlineRule rule) {
		String text = label.getText();
		if (text.length() == 1) {
			int bulletChar = text.charAt(0);
			if (bulletChar == 0xf0b7) {
				// solid bullet
				if (rule != null) {
					rule.set("list-style-type", new CSSName("disc"));
					rule.set("text-indent", new CSSLength(0, "px"));
				}
				return true;
			} else if (bulletChar == 'o') {
				if (rule != null) {
					rule.set("list-style-type", new CSSName("circle"));
					rule.set("text-indent", new CSSLength(0, "px"));
				}
				return true;
			}
		}
		if (rule != null)
			rule.set("list-style-type", new CSSName("none"));
		return false;
	}

	public StylingResult getLabelRule(InlineRule paragraphRule, NumberingLabel label, float emScale) {
		StylingResult result = new StylingResult();
		if (label.getRunProperties() != null) {
			convertStylingRule(result, label.getRunProperties(), emScale, false, false);
		}
		if (paragraphRule != null) {
			Object textIndent = paragraphRule.get("text-indent");
			if (textIndent instanceof CSSLength) {
				CSSLength len = (CSSLength) textIndent;
				if (len.getValue() < 0) {
					CSSLength labelWidth = new CSSLength(-len.getValue(), len.getUnit());
					result.elementRule.set("display", new CSSName("inline-block"));
					result.elementRule.set("text-indent", new CSSLength(0, "px"));
					result.elementRule.set("min-width", labelWidth);
				}
			}
		}
		result.elementClassName = "label";
		return result;
	}

	private void setContainerIfNotPresent(StylingResult result, String prop, CSSValue value) {
		if (result.containerRule == null)
			result.containerRule = new InlineRule();
		setIfNotPresent(result.containerRule, prop, value);
	}

	private void setElementIfNotPresent(StylingResult result, String prop, CSSValue value) {
		setIfNotPresent(result.elementRule, prop, value);
	}

	private boolean keepTogether(Hashtable wprop) {
		Object contextualSpacing = wprop.get("contextualSpacing");
		if (contextualSpacing != null && contextualSpacing.equals(Boolean.TRUE))
			return true;
		return false;
	}

	private void convertWordToCSS(StylingResult result, Hashtable wprop, NumberingLabel label, float emScale,
			boolean sameStyleBefore, boolean sameStyleAfter) {
		final float normalWidth = 612; // convert to percentages of this
		if (wprop.isEmpty())
			return;
		double fontSize = 1;
		Object value = wprop.get("sz");
		if (value != null)
			fontSize = ((Number) value).doubleValue() / (emScale * defaultFontSize);
		value = wprop.get("vertAlign");
		if (value != null) {
			if (value.equals("superscript")) {
				setElementIfNotPresent(result, "vertical-align", new CSSName("super"));
				fontSize *= 0.8;
			} else if (value.equals("subscript")) {
				setElementIfNotPresent(result, "vertical-align", new CSSName("sub"));
				fontSize *= 0.8;
			}
		}
		if (fontSize != 1) {
			if (usingPX)
				setElementIfNotPresent(result, "font-size", new CSSLength(fontSize * emScale * defaultFontSize / 2,
						"px"));
			else
				setElementIfNotPresent(result, "font-size", new CSSLength(fontSize, "em"));
		}

		double containerFontSize = defaultFontSize * emScale;
		double elementFontSize = containerFontSize * fontSize;

		Number indLeft = (Number) wprop.get("ind-left");
		Number indHanging = (Number) wprop.get("ind-hanging");
		Number indFirstLine = (Number) wprop.get("ind-firstLine");
		boolean isList = label != null || result.elementName != null && result.elementName.equals("li");

		if (indLeft != null || indHanging != null || indFirstLine != null) {
			double left = indLeft == null ? 0 : indLeft.doubleValue();
			double hang = indHanging == null ? 0 : indHanging.doubleValue();
			double indent = indFirstLine == null ? 0 : indFirstLine.doubleValue();
			indent -= hang;

			if (isList) {
				double halfPtSize = left / 10.0;
				if (usingPX || indent < 0 || isList) {
					double pxSize = halfPtSize / 2;
					setElementIfNotPresent(result, "margin-left", new CSSLength(pxSize, "px"));
				} else {
					double remSize = halfPtSize / elementFontSize;
					setElementIfNotPresent(result, "margin-left", new CSSLength(remSize, "em"));
				}
			} else if (left > 0) {
				double pts = left / 20;
				if (pts > 0) {
					if (isList) {
						setElementIfNotPresent(result, "margin-left", new CSSLength(pts, "px"));
					} else {
						double percent = 100 * pts / normalWidth;
						setContainerIfNotPresent(result, "margin-left", new CSSLength(percent, "%"));
					}
				}
			}
			if (indent != 0) {
				double halfPtSize = indent / 10.0;
				if (usingPX || indent < 0 || isList) {
					double pxSize = halfPtSize / 2;
					setElementIfNotPresent(result, "text-indent", new CSSLength(pxSize, "px"));
				} else {
					double remSize = halfPtSize / elementFontSize;
					setElementIfNotPresent(result, "text-indent", new CSSLength(remSize, "em"));
				}
			}
		}

		Iterator props = wprop.keySet().iterator();
		while (props.hasNext()) {
			String name = (String) props.next();
			value = wprop.get(name);
			if (name.equals("b")) {
				boolean bold = value.equals(Boolean.TRUE);
				setElementIfNotPresent(result, "font-weight", new CSSName(bold ? "bold" : "normal"));
			} else if (name.equals("i")) {
				boolean italic = value.equals(Boolean.TRUE);
				setElementIfNotPresent(result, "font-style", new CSSName(italic ? "italic" : "normal"));
			} else if (name.equals("rFonts")) {
				FontFamily fontFamily = (FontFamily) value;
				setElementIfNotPresent(result, "font-family", getFontFamilyString(fontFamily));
			} else if (name.equals("u")) {
				Object val = result.elementRule.get("text-decoration");
				if (val == null || !val.equals("line-through"))
					result.elementRule.set("text-decoration", new CSSName("underline"));
				else {
					CSSValue[] td = { new CSSName("line-through"), new CSSName("underline") };
					result.elementRule.set("text-decoration", new CSSValueList(',', td));
				}
			} else if (name.equals("strike")) {
				Object val = result.elementRule.get("text-decoration");
				if (val == null || !val.equals("underline"))
					result.elementRule.set("text-decoration", new CSSName("line-through"));
				else {
					CSSValue[] td = { new CSSName("line-through"), new CSSName("underline") };
					result.elementRule.set("text-decoration", new CSSValueList(',', td));
				}
			} else if (name.equals("color")) {
				setElementIfNotPresent(result, "color", ((RGBColor) value).toCSSValue());
			} else if (name.equals("highlight")) {
				setElementIfNotPresent(result, "background-color", ((RGBColor) value).toCSSValue());
			} else if (name.equals("shd")) {
				if (value instanceof RGBColor)
					setContainerIfNotPresent(result, "background-color", ((RGBColor) value).toCSSValue());
			} else if (name.startsWith("gridSpan")) {
				result.cols = (Integer) value;
			} else if (name.startsWith("vAlign")) {
				String val = value.toString();
				if (val.equals("center"))
					result.elementRule.set("vertical-align", new CSSName("middle"));
				else if (val.equals("top"))
					result.elementRule.set("vertical-align", new CSSName("top"));
				else if (val.equals("bottom"))
					result.elementRule.set("vertical-align", new CSSName("bottom"));
			} else if (name.startsWith("border-")) {
				BorderSide side = (BorderSide) value;
				CSSLength paddingDef = new CSSLength(side.getSpace() / 8.0, "px");
				CSSValue borderDef = convertBorderSide(side);
				if (name.equals("border-insideH")) {
					if (result.tableCellRule == null)
						result.tableCellRule = new InlineRule();
					result.tableCellRule.set("padding-top", paddingDef);
					result.tableCellRule.set("padding-bottom", paddingDef);
					result.tableCellRule.set("border-top", borderDef);
					result.tableCellRule.set("border-bottom", borderDef);
				} else if (name.equals("border-insideV")) {
					if (result.tableCellRule == null)
						result.tableCellRule = new InlineRule();
					result.tableCellRule.set("padding-left", paddingDef);
					result.tableCellRule.set("padding-right", paddingDef);
					result.tableCellRule.set("border-left", borderDef);
					result.tableCellRule.set("border-right", borderDef);
				} else if (name.equals("border-top")) {
					setContainerIfNotPresent(result, "padding-top", paddingDef);
					setContainerIfNotPresent(result, "border-top", borderDef);
				} else if (name.equals("border-bottom")) {
					setContainerIfNotPresent(result, "padding-bottom", paddingDef);
					setContainerIfNotPresent(result, "border-bottom", borderDef);
				} else if (name.equals("border-left")) {
					setContainerIfNotPresent(result, "padding-left", paddingDef);
					setContainerIfNotPresent(result, "border-left", borderDef);
				} else if (name.equals("border-right")) {
					setContainerIfNotPresent(result, "padding-right", paddingDef);
					setContainerIfNotPresent(result, "border-right", borderDef);
				}
			} else if (name.equals("jc")) {
				String css = "left";
				if (value.equals("both"))
					css = "justify";
				else if (value.equals("right") || value.equals("center"))
					css = value.toString();
				setElementIfNotPresent(result, "text-align", new CSSName(css));
			} else if (name.equals("webHidden")) {
				boolean hidden = value.equals(Boolean.TRUE);
				if (hidden)
					setElementIfNotPresent(result, "display", new CSSName("none"));
			} else if (name.equals("pageBreakBefore")) {
				boolean pageBreakBefore = value.equals(Boolean.TRUE);
				if (pageBreakBefore)
					setContainerIfNotPresent(result, "page-break-before", new CSSName("always"));
			} else if (name.equals("keepNext")) {
				if (value.equals(Boolean.TRUE))
					setContainerIfNotPresent(result, "page-break-after", new CSSName("avoid"));
			} else if (name.equals("keepLines")) {
				if (value.equals(Boolean.TRUE))
					setContainerIfNotPresent(result, "page-break-inside", new CSSName("avoid"));
			} else if (name.equals("spacing-before")) {
				if (sameStyleBefore && keepTogether(wprop)) {
					// vertical margin is zeroed out in this case
				} else {
					double halfPtSize = ((Number) value).doubleValue() / 10.0;
					CSSLength len;
					boolean marginOnElement = sameStyleBefore;
					if (usingPX) {
						double pxSize = halfPtSize / 2;
						len = new CSSLength(pxSize, "px");
					} else {
						double remSize = halfPtSize / containerFontSize;
						len = new CSSLength(remSize, "em");
					}
					if (marginOnElement) {
						setElementIfNotPresent(result, "margin-top", len);
					} else {
						setContainerIfNotPresent(result, "margin-top", len);
					}
				}
			} else if (name.equals("spacing-after")) {
				if (sameStyleAfter && keepTogether(wprop)) {
					// vertical margin is zeroed out in this case
				} else {
					double halfPtSize = ((Number) value).doubleValue() / 10.0;
					CSSLength len;
					boolean marginOnElement = sameStyleAfter;
					if (usingPX) {
						double pxSize = halfPtSize / 2;
						len = new CSSLength(pxSize, "px");
					} else {
						double remSize = halfPtSize / containerFontSize;
						len = new CSSLength(remSize, "em");
					}
					if (marginOnElement) {
						setElementIfNotPresent(result, "margin-bottom", len);
					} else {
						setContainerIfNotPresent(result, "margin-bottom", len);
					}
				}
			} else if (name.equals("spacing-line")) {
				String lineRule = (String) wprop.get("spacing-lineRule");
				double halfPtSize = ((Number) value).doubleValue() / 10.0;
				if (halfPtSize > 0) {
					if (usingPX && lineRule != null) {
						double pxSize = halfPtSize / 2;
						setElementIfNotPresent(result, "line-height", new CSSLength(pxSize, "px"));
					} else {
						double base = elementFontSize;
						if (lineRule != null && lineRule.equals("auto"))
							base = 24.0;
						double remSize = halfPtSize / base;
						setElementIfNotPresent(result, "line-height", new CSSLength(remSize, ""));
					}
				}
			} else if (name.equals("ind-right")) {
				double pts = ((Number) value).doubleValue() / 20.0;
				if (pts > 0) {
					if (isList) {
						setElementIfNotPresent(result, "margin-right", new CSSLength(pts, "px"));
					} else {
						double percent = 100 * pts / normalWidth;
						setContainerIfNotPresent(result, "margin-right", new CSSLength(percent, "%"));
					}
				}
			} else if (name.equals("framePr-align")) {
				String align = (String) value;
				if (align != null) {
					setContainerIfNotPresent(result, "float", new CSSName(align));
				} else {
					setContainerIfNotPresent(result, "float", new CSSImpliedValue(new CSSName("left")));
				}
			} else if (name.equals("framePr-w")) {
				float width = ((Number) value).floatValue();
				if (width > 0) {
					float pts = width / 20;
					float percent = 100 * pts / normalWidth;
					setContainerIfNotPresent(result, "width", new CSSLength(percent, "%"));
					// extra space indicates it was not explicitly set
					setContainerIfNotPresent(result, "float", new CSSImpliedValue(new CSSName("left")));
				}
			} else if (name.equals("framePr-hSpace")) {
				float hSpace = ((Number) value).floatValue();
				if (hSpace > 0) {
					String align = (String) wprop.get("framePr-align");
					float pts = hSpace / 20;
					CSSLength margin = new CSSLength(pts, "px");
					if (align == null || align.equals("left")) {
						setContainerIfNotPresent(result, "margin-right", margin);
						setContainerIfNotPresent(result, "float", new CSSImpliedValue(new CSSName("left")));
					} else
						setContainerIfNotPresent(result, "margin-left", margin);
				}
			} else if (name.equals("framePr-vSpace")) {
				float vSpace = ((Number) value).floatValue();
				if (vSpace > 0) {
					float pts = vSpace / 20;
					setContainerIfNotPresent(result, "margin-bottom", new CSSLength(pts, "px"));
					setContainerIfNotPresent(result, "float", new CSSImpliedValue(new CSSName("left")));
				}
			}
		}
		if (label != null)
			convertLabelToProperty(label, result.elementRule);
	}

	private void cascade(Hashtable target, BaseProperties src, boolean force) {
		if (src == null || src.isEmpty())
			return;
		Iterator props = src.properties();
		while (props.hasNext()) {
			String prop = (String) props.next();
			if (force || target.get(prop) == null)
				target.put(prop, src.get(prop));
		}
	}

	Hashtable cascadeWordProperties(BaseProperties prop) {
		boolean runOnly = prop instanceof RunProperties;
		boolean tableOnly = prop instanceof TableProperties;
		Style style;
		NumberingLabel label = null;
		Hashtable wprop = new Hashtable();
		if (prop instanceof RunProperties) {
			RunProperties rp = (RunProperties) prop;
			cascade(wprop, rp, false);
			style = rp.getRunStyle();
		} else if (prop instanceof ParagraphProperties) {
			ParagraphProperties pp = (ParagraphProperties) prop;
			label = pp.getNumberingLabel();
			cascade(wprop, pp, false);
			style = pp.getParagraphStyle();
		} else if (prop instanceof TableCellProperties) {
			TableCellProperties tcp = (TableCellProperties) prop;
			cascade(wprop, tcp, false);
			style = null;
		} else if (prop instanceof TableProperties) {
			TableProperties tp = (TableProperties) prop;
			cascade(wprop, tp, false);
			style = ((TableProperties) prop).getTableStyle();
		} else {
			System.err.println("Unimplemented cascade: " + prop.getClass());
			style = null;
		}
		while (style != null) {
			if (tableOnly)
				cascade(wprop, style.getTableProperties(), false);
			else {
				cascade(wprop, style.getRunProperties(), false);
				if (!runOnly)
					cascade(wprop, style.getParagraphProperties(), false);
			}
			style = style.getParent();
		}
		if (label != null) {
			ParagraphProperties pp = label.getParagraphProperties();
			cascade(wprop, pp, true);
		}
		if (!runOnly && documentDefaultParagraphStyle != null)
			cascade(wprop, documentDefaultParagraphStyle.getParagraphProperties(), false);
		return wprop;
	}

	private void convertStylingRule(StylingResult result, BaseProperties prop, float emScale, boolean sameStyleBefore,
			boolean sameStyleAfter) {
		Hashtable wprop = cascadeWordProperties(prop);
		NumberingLabel label = null;
		if (prop instanceof ParagraphProperties)
			label = ((ParagraphProperties) prop).getNumberingLabel();

		convertWordToCSS(result, wprop, label, emScale, sameStyleBefore, sameStyleAfter);

		if (result.elementName != null && result.elementName.startsWith("h")) {
			if (result.elementRule.get("font-weight") == null)
				result.elementRule.set("font-weight", new CSSName("normal"));
			if (result.elementRule.get("margin-top") == null)
				result.elementRule.set("margin-top", new CSSLength(0, "px"));
			if (result.elementRule.get("margin-bottom") == null)
				result.elementRule.set("margin-bottom", new CSSLength(0, "px"));
			if (result.elementRule.get("margin-left") == null)
				result.elementRule.set("margin-left", new CSSLength(0, "px"));
			if (result.elementRule.get("margin-right") == null)
				result.elementRule.set("margin-right", new CSSLength(0, "px"));
		}
	}

	public StylingResult convertTableStylingRule(TableProperties prop, float emScale) {
		Hashtable wprop = cascadeWordProperties(prop);
		StylingResult result = new StylingResult();
		result.containerRule = result.elementRule;

		convertWordToCSS(result, wprop, null, emScale, false, false);

		return result;
	}

	public StylingResult convertTableCellStylingRule(TableCellProperties prop, float emScale,
			InlineRule tableGlobalProps) {
		Hashtable wprop = cascadeWordProperties(prop);
		StylingResult result = new StylingResult();
		result.containerRule = result.elementRule;

		convertWordToCSS(result, wprop, null, emScale, false, false);

		if (tableGlobalProps != null && !tableGlobalProps.isEmpty()) {
			Iterator tgpi = tableGlobalProps.properties();
			while (tgpi.hasNext()) {
				String tgpn = (String) tgpi.next();
				CSSValue tgpv = tableGlobalProps.get(tgpn);
				setIfNotPresent(result.elementRule, tgpn, tgpv);
			}
		}

		return result;
	}

	StylingResult styleElement(BaseProperties prop, boolean isListElement, float emScale, boolean sameStyleBefore,
			boolean sameStyleAfter) {
		StylingResult result = new StylingResult();
		if (prop == null)
			return result;
		if (prop instanceof ParagraphProperties) {
			ParagraphProperties pp = (ParagraphProperties) prop;
			Style style = pp.getParagraphStyle();
			boolean noInlineStyling = pp.isEmpty() && pp.getNumberingLabel() == null && pp.getRunProperties() == null;
			if (style == null) {
				if (noInlineStyling)
					return result;
			} else if (isListElement) {
				result.elementName = "li";
			} else {
				result.elementName = mapToElement(style.getStyleId());
			}
			if (result.elementName == null && style != null) {
				String styleId = style.getStyleId();
				if (styleId.equals("Normal"))
					result.elementClassName = "p";
				else
					result.elementClassName = styleId;
			} else {
				if (!noInlineStyling || result.elementName.equals("h1") || result.elementName.equals("li")
						|| style == null)
					result.elementClassName = "p";
				else
					result.elementClassName = style.getStyleId();
			}
		} else {
			RunProperties rp = (RunProperties) prop;
			int runMask = getRunMask(rp);
			switch (runMask) {
			case 0:
				// don't create unneeded span element
				return result;
			case RUN_BOLD:
				// bold only
				result.elementName = "b";
				return result;
			case RUN_ITALIC:
				// italic only
				result.elementName = "i";
				return result;
			case RUN_SUB:
				// subscript only
				result.elementName = "sub";
				return result;
			case RUN_SUPER:
				// superscript only
				result.elementName = "sup";
				return result;
			}
			if ((runMask & RUN_STYLE) != 0) {
				String styleId = rp.getRunStyle().getStyleId();
				if (styleId.equals("DefaultParagraphFont"))
					result.elementClassName = "r"; // shorten it
				else
					result.elementClassName = styleId;
			} else {
				result.elementClassName = "r";
			}
		}
		convertStylingRule(result, prop, emScale, sameStyleBefore, sameStyleAfter);
		return result;
	}

}
