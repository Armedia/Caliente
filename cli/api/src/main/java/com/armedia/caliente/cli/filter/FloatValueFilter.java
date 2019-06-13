package com.armedia.caliente.cli.filter;

public class FloatValueFilter extends NumericValueFilter<Float> {

	public FloatValueFilter(Float min) {
		this(min, Float.MAX_VALUE);
	}

	public FloatValueFilter(Float min, boolean minInclusive) {
		this(min, minInclusive, Float.MAX_VALUE, NumericValueFilter.DEFAULT_INCLUSIVE);
	}

	public FloatValueFilter(Float min, Float max) {
		this(min, NumericValueFilter.DEFAULT_INCLUSIVE, max, NumericValueFilter.DEFAULT_INCLUSIVE);
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