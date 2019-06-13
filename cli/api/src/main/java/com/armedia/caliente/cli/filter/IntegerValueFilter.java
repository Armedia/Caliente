package com.armedia.caliente.cli.filter;

public class IntegerValueFilter extends NumericValueFilter<Integer> {

	public IntegerValueFilter(Integer min) {
		this(min, Integer.MAX_VALUE);
	}

	public IntegerValueFilter(Integer min, boolean minInclusive) {
		this(min, minInclusive, Integer.MAX_VALUE, NumericValueFilter.DEFAULT_INCLUSIVE);
	}

	public IntegerValueFilter(Integer min, Integer max) {
		this(min, NumericValueFilter.DEFAULT_INCLUSIVE, max, NumericValueFilter.DEFAULT_INCLUSIVE);
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