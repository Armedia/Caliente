package com.armedia.caliente.cli;

import java.util.regex.Pattern;

import com.armedia.commons.utilities.Tools;

public class GlobValueFilter extends OptionValueFilter {
	private final String glob;
	private final Pattern pattern;
	private final String description;

	public GlobValueFilter(boolean caseSensitive, String glob) {
		this.glob = glob;
		this.pattern = Pattern.compile(Tools.globToRegex(glob), caseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
		this.description = String.format("a string that matches the glob [%s]%s", glob,
			caseSensitive ? "" : " (case-insensitively)");
	}

	public String getGlob() {
		return this.glob;
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