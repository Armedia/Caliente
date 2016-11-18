package com.armedia.caliente.cli.parser;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public final class RootParameterSet extends ParameterSet {

	private final Map<String, ParameterSet> subs = new TreeMap<>();

	public RootParameterSet(String name) {
		super(name);
	}

	public boolean isSubcommandName(String subName) {
		return validateLong(subName) && this.subs.containsKey(subName);
	}

	public ParameterSet getSubcommand(String subName) {
		if (!validateLong(subName)) { throw new IllegalArgumentException(String.format(
			"The string [%s] is not a valid subcommand name - it may not be null, the empty string, or contain whitespace",
			subName)); }
		return this.subs.get(subName);
	}

	public boolean hasSubcommand(String subName) {
		if (!validateLong(subName)) { throw new IllegalArgumentException(String.format(
			"The string [%s] is not a valid subcommand name - it may not be null, the empty string, or contain whitespace",
			subName)); }
		return this.subs.containsKey(subName);
	}

	public void addSubcommand(String subName, ParameterSet sub) {
		if (!validateLong(subName)) { throw new IllegalArgumentException(String.format(
			"The string [%s] is not a valid subcommand name - it may not be null, the empty string, or contain whitespace",
			subName)); }
		if (sub == null) { throw new IllegalArgumentException("Must provide a subcommand parameter set"); }
		this.subs.put(subName, sub);
	}

	public ParameterSet removeSubcommand(String subName) {
		if (!validateLong(subName)) { throw new IllegalArgumentException(String.format(
			"The string [%s] is not a valid subcommand name - it may not be null, the empty string, or contain whitespace",
			subName)); }
		return this.subs.remove(subName);
	}

	public Set<String> getSubcommands() {
		return new LinkedHashSet<>(this.subs.keySet());
	}
}