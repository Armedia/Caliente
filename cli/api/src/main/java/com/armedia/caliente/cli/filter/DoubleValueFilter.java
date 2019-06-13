package com.armedia.caliente.cli.filter;

public class DoubleValueFilter extends NumericValueFilter<Double> {

	public DoubleValueFilter(Double min) {
		this(min, Double.MAX_VALUE);
	}

	public DoubleValueFilter(Double min, boolean minInclusive) {
		this(min, minInclusive, Double.MAX_VALUE, NumericValueFilter.DEFAULT_INCLUSIVE);
	}

	public DoubleValueFilter(Double min, Double max) {
		this(min, NumericValueFilter.DEFAULT_INCLUSIVE, max, NumericValueFilter.DEFAULT_INCLUSIVE);
	}

	public DoubleValueFilter(Double min, boolean minInclusive, Double max, boolean maxInclusive) {
		super("double", min, minInclusive, max, maxInclusive);
	}

	@Override
	public int compare(Double a, Double b) {
		return a.compareTo(b);
	}

	@Override
	protected Double convert(String str) throws NumberFormatException {
		return Double.valueOf(str);
	}
}