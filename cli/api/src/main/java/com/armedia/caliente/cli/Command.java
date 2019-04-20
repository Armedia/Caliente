package com.armedia.caliente.cli;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import com.armedia.caliente.cli.exception.CommandLineSyntaxException;

public class Command extends OptionScheme {

	private static final Pattern NAME_PATTERN = Pattern.compile("^\\S+$");

	private final Set<String> aliases;

	public Command(String name, Collection<String> aliases) {
		super(name);
		if (!Command.NAME_PATTERN.matcher(name).matches()) {
			throw new IllegalArgumentException(String.format("The name [%s] does not match the regular expression /%s/",
				name, Command.NAME_PATTERN.pattern()));
		}
		Set<String> A = new TreeSet<>();
		A.add(name);
		if (aliases != null) {
			for (String a : aliases) {
				Objects.requireNonNull(a, "aliases may not be null");
				if (!Command.NAME_PATTERN.matcher(a).matches()) {
					throw new IllegalArgumentException(
						String.format("The given alias [%s] does not match the regular expression /%s/", a,
							Command.NAME_PATTERN.pattern()));
				}
				A.add(a);
			}
		}
		this.aliases = Collections.unmodifiableSet(A);
	}

	public Command(String name, String... aliases) {
		this(name, aliases != null ? Arrays.asList(aliases) : null);
	}

	public void initializeDynamicOptions(boolean helpRequested, OptionValues baseValues)
		throws CommandLineSyntaxException {
		// by default, do nothing
	}

	public Set<String> getAliases() {
		return this.aliases;
	}
}