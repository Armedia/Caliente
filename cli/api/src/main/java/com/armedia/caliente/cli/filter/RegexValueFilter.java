package com.armedia.caliente.cli.filter;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.cli.OptionValueFilter;

public class RegexValueFilter extends OptionValueFilter {

	private final Pattern pattern;
	private final String description;

	public RegexValueFilter(String regex) {
		this(true, regex, null);
	}

	public RegexValueFilter(boolean caseSensitive, String regex) {
		this(caseSensitive, regex, null);
	}

	public RegexValueFilter(boolean caseSensitive, String regex, String description) {
		this.pattern = Pattern.compile(regex, caseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
		if (StringUtils.isBlank(description)) {
			description = String.format("a string that matches the regex /%s/%s", this.pattern.pattern(),
				caseSensitive ? "" : " (case insensitively)");
		}
		this.description = description;
	}

	public boolean isCaseSensitive() {
		return ((this.pattern.flags() | Pattern.CASE_INSENSITIVE) == 0);
	}

	public Pattern getPattern() {
		return this.pattern;
	}

	@Override
	protected boolean checkValue(String value) {
		return this.pattern.matcher(value).matches();
	}

	@Override
	public String getDefinition() {
		return this.description;
	}
}