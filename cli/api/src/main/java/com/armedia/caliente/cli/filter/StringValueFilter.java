/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.cli.OptionValueFilter;
import com.armedia.commons.utilities.Tools;

public class StringValueFilter extends OptionValueFilter {

	private static final boolean DEFAULT_CASE_SENSITIVE = true;

	private final boolean caseSensitive;
	private final Set<String> allowed;
	private final Set<String> canonical;
	private final String description;

	private static Collection<String> toCollection(String[] allowed) {
		if ((allowed == null) || (allowed.length == 0)) { return Collections.emptyList(); }
		return Arrays.asList(allowed);
	}

	public StringValueFilter(String... allowed) {
		this(StringValueFilter.DEFAULT_CASE_SENSITIVE, StringValueFilter.toCollection(allowed));
	}

	public StringValueFilter(boolean caseSensitive, String... allowed) {
		this(caseSensitive, StringValueFilter.toCollection(allowed));
	}

	public StringValueFilter(Collection<String> allowed) {
		this(StringValueFilter.DEFAULT_CASE_SENSITIVE, allowed);
	}

	public StringValueFilter(boolean caseSensitive, Collection<String> allowed) {
		this.caseSensitive = caseSensitive;
		Set<String> defined = new TreeSet<>();
		Set<String> canonicalized = new TreeSet<>();
		if ((allowed != null) && !allowed.isEmpty()) {
			for (String s : allowed) {
				s = StringUtils.strip(s);
				if (s == null) {
					continue;
				}
				defined.add(s);
				canonicalized.add(canon(s));
			}
		}
		if (defined.isEmpty()) {
			throw new IllegalArgumentException("No values are marked as canonical, this is illegal");
		}
		this.allowed = Tools.freezeSet(new LinkedHashSet<>(defined));
		this.canonical = (caseSensitive ? this.allowed : Tools.freezeSet(new LinkedHashSet<>(canonicalized)));
		this.description = String.format("one of%s: %s", (caseSensitive ? "" : " (case insensitive)"),
			this.allowed.toString());
	}

	protected String canon(String value) {
		value = StringUtils.strip(value);
		if ((value == null) || this.caseSensitive) { return value; }
		return value.toUpperCase();
	}

	public boolean isCaseSensitive() {
		return this.caseSensitive;
	}

	public Set<String> getAllowed() {
		return this.allowed;
	}

	@Override
	protected boolean checkValue(String value) {
		return this.canonical.contains(canon(value));
	}

	@Override
	public String getDefinition() {
		return this.description;
	}
}