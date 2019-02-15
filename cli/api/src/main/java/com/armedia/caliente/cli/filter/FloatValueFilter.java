package com.armedia.caliente.cli.filter;

public class FloatValueFilter extends NumericValueFilter<Float> {

	public FloatValueFilter(Float min, Float max) {
		super("float", min, max);
	}

	public FloatValueFilter(Float min, boolean minInclusive, Float max, boolean maxInclusive) {
		super("float", min, minInclusive, max, maxInclusive);
	}

	@Override
	public int compare(Float a, Float b) {
		return a.compareTo(b);
	}

	@Override
	protected Float convert(String str) throws NumberFormatException {
		return Float.valueOf(str);
	}
}