package com.armedia.caliente.cli;

import java.util.regex.Pattern;

public class RegexValueFilter extends OptionValueFilter {
	private final Pattern pattern;
	private final String description;

	public RegexValueFilter(String regex) {
		this.pattern = Pattern.compile(regex);
		this.description = String.format("a string that matches the regex /%s/", this.pattern.pattern());
	}

	protected Pattern getPattern() {
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