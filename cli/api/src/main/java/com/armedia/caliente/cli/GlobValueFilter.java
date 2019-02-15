package com.armedia.caliente.cli;

import com.armedia.commons.utilities.Tools;

public class GlobValueFilter extends RegexValueFilter {
	private final String glob;

	public GlobValueFilter(String glob) {
		this(true, glob);
	}

	public GlobValueFilter(boolean caseSensitive, String glob) {
		super(caseSensitive, Tools.globToRegex(glob),
			String.format("a string that matches the glob [%s]%s", glob, caseSensitive ? "" : " (case-insensitively)"));
		this.glob = glob;
	}

	public String getGlob() {
		return this.glob;
	}
}