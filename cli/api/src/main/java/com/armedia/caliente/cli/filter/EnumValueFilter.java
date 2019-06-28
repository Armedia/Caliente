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

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.cli.OptionValueFilter;
import com.armedia.commons.utilities.Tools;

public class EnumValueFilter<E extends Enum<E>> extends OptionValueFilter {

	private final Class<E> klass;
	private final Set<E> allowed;
	private final String description;
	private final boolean caseSensitive;
	private final Map<String, E> canon;

	public EnumValueFilter(Class<E> enumClass) {
		this(true, enumClass);
	}

	public EnumValueFilter(boolean caseSensitive, Class<E> enumClass) {
		this(caseSensitive, enumClass, null);
	}

	public EnumValueFilter(Class<E> enumClass, Set<E> excluded) {
		this(true, enumClass, excluded);
	}

	public EnumValueFilter(boolean caseSensitive, Class<E> enumClass, Set<E> excluded) {
		this.klass = Objects.requireNonNull(enumClass, "Must provide an enum class");
		if (!enumClass.isEnum()) {
			throw new IllegalArgumentException(
				String.format("The class %s is not an Enum", enumClass.getCanonicalName()));
		}
		this.caseSensitive = caseSensitive;

		// Limit to only the non-excluded (allowed) values
		Set<E> allowed = EnumSet.allOf(this.klass);
		if ((excluded != null) && !excluded.isEmpty()) {
			allowed.removeAll(excluded);
		}
		if (allowed.isEmpty()) {
			throw new IllegalArgumentException("No values are marked as allowed, this is illegal");
		}

		// Generate the canonicalized set
		Set<String> v = new TreeSet<>();
		Map<String, E> canon = new TreeMap<>();
		for (E e : allowed) {
			v.add(e.name());
			E old = canon.put(canon(e.name()), e);
			if (old != null) {
				throw new IllegalArgumentException(String.format(
					"Enums of type %s can't be handled case-insensitively - the values %s and %s would collide",
					enumClass.getCanonicalName(), old.name(), e.name()));
			}
		}
		this.allowed = Tools.freezeSet(allowed);
		this.canon = Tools.freezeMap(new LinkedHashMap<>(canon));
		this.description = String.format("one of%s: %s", (caseSensitive ? "" : " (case insensitive)"), v);
	}

	public Set<E> getAllowed() {
		return this.allowed;
	}

	public E decode(String value) {
		value = canon(value);
		if (value == null) { return null; }
		return this.canon.get(value);
	}

	public boolean isCaseSensitive() {
		return this.caseSensitive;
	}

	protected String canon(String value) {
		value = StringUtils.strip(value);
		if ((value == null) || this.caseSensitive) { return value; }
		return value.toUpperCase();
	}

	@Override
	protected boolean checkValue(String value) {
		return this.canon.containsKey(canon(value));
	}

	@Override
	public String getDefinition() {
		return this.description;
	}
}