package com.armedia.caliente.cli;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import com.armedia.commons.utilities.Tools;

public class StringValueFilter extends OptionValueFilter {

	private final Set<String> allowed;
	private final String description;

	private static Collection<String> toCollection(String[] allowed) {
		if ((allowed == null) || (allowed.length == 0)) { return Collections.emptyList(); }
		return Arrays.asList(allowed);
	}

	public StringValueFilter(String... allowed) {
		this(StringValueFilter.toCollection(allowed));
	}

	public StringValueFilter(Collection<String> allowed) {
		Set<String> v = new TreeSet<>();
		if ((allowed != null) && !allowed.isEmpty()) {
			for (String s : allowed) {
				if (s == null) {
					continue;
				}
				v.add(s);
			}
		}
		if (v.isEmpty()) { throw new IllegalArgumentException("No values are marked as allowed, this is illegal"); }
		this.allowed = Tools.freezeSet(new LinkedHashSet<>(v));
		this.description = String.format("one of %s", this.allowed.toString());
	}

	@Override
	protected boolean checkValue(String value) {
		return this.allowed.contains(value);
	}

	@Override
	public String getDefinition() {
		return this.description;
	}
}