/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
 * %%
 * This file is part of the Caliente software. 
 *  
 * If the software was purchased under a paid Caliente license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *   
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
package com.armedia.caliente.cli.filter;

import java.math.BigInteger;

public class BigIntegerValueFilter extends NumericValueFilter<BigInteger> {

	public BigIntegerValueFilter(BigInteger min, BigInteger max) {
		this(min, NumericValueFilter.DEFAULT_INCLUSIVE, max, NumericValueFilter.DEFAULT_INCLUSIVE);
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