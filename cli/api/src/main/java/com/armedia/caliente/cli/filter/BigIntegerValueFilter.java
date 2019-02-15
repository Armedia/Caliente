package com.armedia.caliente.cli.filter;

import java.math.BigInteger;

public class BigIntegerValueFilter extends NumericValueFilter<BigInteger> {

	public BigIntegerValueFilter(BigInteger min, BigInteger max) {
		super("big integer", min, max);
	}

	public BigIntegerValueFilter(BigInteger min, boolean minInclusive, BigInteger max, boolean maxInclusive) {
		super("big integer", min, minInclusive, max, maxInclusive);
	}

	@Override
	public int compare(BigInteger a, BigInteger b) {
		return a.compareTo(b);
	}

	@Override
	protected BigInteger convert(String str) throws NumberFormatException {
		return new BigInteger(str);
	}
}