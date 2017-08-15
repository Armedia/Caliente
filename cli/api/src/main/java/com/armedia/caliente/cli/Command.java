package com.armedia.caliente.cli;

import java.util.regex.Pattern;

public class Command extends ParameterScheme {

	private static final Pattern NAME_PATTERN = Pattern.compile("^\\S+$");

	private final String name;

	public Command(String name) {
		if (name == null) { throw new IllegalArgumentException("Must provide a non-null name"); }
		if (!Command.NAME_PATTERN.matcher(name).matches()) { throw new IllegalArgumentException(
			String.format("The name must match the regular expression /%s/", Command.NAME_PATTERN.pattern())); }
		this.name = name;
	}

	public final String getName() {
		return this.name;
	}
}