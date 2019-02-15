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