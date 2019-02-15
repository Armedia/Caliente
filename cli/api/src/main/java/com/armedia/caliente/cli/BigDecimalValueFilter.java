package com.armedia.caliente.cli;

import java.math.BigDecimal;

public class BigDecimalValueFilter extends NumericValueFilter<BigDecimal> {

	public BigDecimalValueFilter(BigDecimal min, BigDecimal max) {
		super("big decimal", min, max);
	}

	public BigDecimalValueFilter(BigDecimal min, boolean minInclusive, BigDecimal max, boolean maxInclusive) {
		super("big decimal", min, minInclusive, max, maxInclusive);
	}

	@Override
	public int compare(BigDecimal a, BigDecimal b) {
		return a.compareTo(b);
	}

	@Override
	protected BigDecimal convert(String str) throws NumberFormatException {
		return new BigDecimal(str);
	}
}