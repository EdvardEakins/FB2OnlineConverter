package com.adobe.dp.css;

import java.io.PrintWriter;

public class CSSRGBColor extends CSSValue {

	int rgb;

	public CSSRGBColor(int rgb) {
		if( (rgb & 0xFF000000) != 0 )
			throw new RuntimeException("invalid parameter");
		this.rgb = rgb;
	}

	public String toString() {
		return "#" + Integer.toHexString(rgb + 0x1000000).substring(1);
	}

	public void serialize(PrintWriter out) {
		out.print("#");
		out.print(Integer.toHexString(rgb + 0x1000000).substring(1));
	}

	public int hashCode() {
		return rgb;
	}

	public boolean equals(Object other) {
		if (other.getClass() == getClass()) {
			CSSRGBColor o = (CSSRGBColor) other;
			return o.rgb == rgb;
		}
		return false;
	}	
}
