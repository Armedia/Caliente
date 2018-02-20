package com.armedia.caliente.cli;

import java.util.regex.Pattern;

import com.armedia.commons.utilities.Tools;

public class GlobValueFilter extends OptionValueFilter {
	private final Pattern pattern;
	private final String description;

	public GlobValueFilter(String glob) {
		this.pattern = Pattern.compile(Tools.globToRegex(glob));
		this.description = String.format("a string that matches the glob [%s]", glob);
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