package com.adobe.dp.css;

import java.io.PrintWriter;

public class CSSLength extends CSSValue {

	double value;

	String unit;

	public CSSLength(double value, String unit) {
		this.value = value;
		this.unit = unit;
	}

	public double getValue() {
		return value;
	}

	public String getUnit() {
		return unit;
	}

	public String toString() {
		double sv = Math.round(value * 1000) / 1000.0;
		if (sv == (int) sv)
			return (int) sv + unit;
		return sv + unit;
	}

	public void serialize(PrintWriter out) {
		double sv = Math.round(value * 1000) / 1000.0;
		if (sv == (int) sv)
			out.print((int) sv);
		else
			out.print(sv);
		out.print(unit);
	}

	public int hashCode() {
		return (int) Math.round(value * 1000) + unit.hashCode();
	}

	public boolean equals(Object other) {
		if (other.getClass() == getClass()) {
			CSSLength o = (CSSLength) other;
			return o.value == value && o.unit.equals(unit);
		}
		return false;
	}
}
