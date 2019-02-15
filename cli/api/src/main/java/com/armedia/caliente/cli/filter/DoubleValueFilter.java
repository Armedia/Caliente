package com.armedia.caliente.cli.filter;

public class DoubleValueFilter extends NumericValueFilter<Double> {

	public DoubleValueFilter(Double min, Double max) {
		super("double", min, max);
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