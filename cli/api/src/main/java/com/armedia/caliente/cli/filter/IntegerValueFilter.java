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