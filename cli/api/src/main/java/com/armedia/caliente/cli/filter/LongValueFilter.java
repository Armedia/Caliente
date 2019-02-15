package com.armedia.caliente.cli.filter;

public class LongValueFilter extends NumericValueFilter<Long> {

	public LongValueFilter(Long min, Long max) {
		super("long", min, max);
	}

	public LongValueFilter(Long min, boolean minInclusive, Long max, boolean maxInclusive) {
		super("long", min, minInclusive, max, maxInclusive);
	}

	@Override
	public int compare(Long a, Long b) {
		return a.compareTo(b);
	}

	@Override
	protected Long convert(String str) throws NumberFormatException {
		return Long.valueOf(str);
	}
}