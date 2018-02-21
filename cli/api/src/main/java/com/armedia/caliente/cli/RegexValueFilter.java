package com.armedia.caliente.cli;

import java.util.regex.Pattern;

public class RegexValueFilter extends OptionValueFilter {

	private final Pattern pattern;
	private final String description;

	public RegexValueFilter(String regex) {
		this(true, regex);
	}

	public RegexValueFilter(boolean caseSensitive, String regex) {
		this.pattern = Pattern.compile(regex, caseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
		this.description = String.format("a string that matches the regex /%s/%s", this.pattern.pattern(),
			caseSensitive ? "" : " (case insensitively)");
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