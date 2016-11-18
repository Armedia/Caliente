package com.armedia.caliente.cli.parser;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public final class CommandLineInterface extends MutableParameterSet {

	private final Map<String, MutableParameterSet> subs = new TreeMap<>();

	public CommandLineInterface(String name) {
		super(name);
	}

	public MutableParameterSet getSubcommand(String subName) {
		return this.subs.get(subName);
	}

	public boolean hasSubcommand(String subName) {
		return this.subs.containsKey(subName);
	}

	public void addSubcommand(String subName, MutableParameterSet sub) {
		if (!validateLong(subName)) { throw new IllegalArgumentException(String.format(
			"The string [%s] is not a valid subcommand name - it may not be null, the empty string, or contain whitespace",
			subName)); }
		if (sub == null) { throw new IllegalArgumentException("Must provide a subcommand parameter set"); }
		this.subs.put(subName, sub);
	}

	public MutableParameterSet removeSubcommand(String subName) {
		return this.subs.remove(subName);
	}

	public Set<String> getSubcommands() {
		return new LinkedHashSet<>(this.subs.keySet());
	}
}