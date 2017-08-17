package com.armedia.caliente.cli;

import java.util.regex.Pattern;

public class Command extends ParameterScheme {

	private static final Pattern NAME_PATTERN = Pattern.compile("^\\S+$");

	public Command(String name) {
		super(name);
		if (!Command.NAME_PATTERN.matcher(name).matches()) { throw new IllegalArgumentException(
			String.format("The name must match the regular expression /%s/", Command.NAME_PATTERN.pattern())); }
	}
}