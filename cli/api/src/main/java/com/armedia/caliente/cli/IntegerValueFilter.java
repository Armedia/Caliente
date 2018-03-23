package com.armedia.caliente.cli;

public class IntegerValueFilter extends NumericValueFilter<Integer> {

	public IntegerValueFilter(Integer min, Integer max) {
		super("integer", min, max);
	}

	public IntegerValueFilter(Integer min, boolean minInclusive, Integer max, boolean maxInclusive) {
		super("integer", min, minInclusive, max, maxInclusive);
	}

	@Override
	public int compare(Integer a, Integer b) {
		return a.compareTo(b);
	}

	@Override
	protected Integer convert(String str) throws NumberFormatException {
		return Integer.valueOf(str);
	}
}