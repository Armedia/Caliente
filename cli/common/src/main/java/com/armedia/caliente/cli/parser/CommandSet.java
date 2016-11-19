package com.armedia.caliente.cli.parser;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public final class CommandSet extends MutableParameterSet {

	private final Map<String, MutableParameterSet> subs = new TreeMap<>();

	public CommandSet(String name) {
		super(name);
	}

	public MutableParameterSet getSubcommand(String subName) {
		return this.subs.get(subName);
	}

	public boolean hasSubcommand(String subName) {
		return this.subs.containsKey(subName);
	}

	public void addSubcommand(String subName, MutableParameterSet sub, String... aliases) {
		if (!validateLong(subName)) { throw new IllegalArgumentException(String.format(
			"The string [%s] is not a valid subcommand name - it may not be null, the empty string, or contain whitespace",
			subName)); }
		if (sub == null) { throw new IllegalArgumentException("Must provide a subcommand parameter set"); }
		Set<String> finalAliases = new LinkedHashSet<>();
		finalAliases.add(subName);
		for (String s : aliases) {
			if (validateLong(s) && (s.length() >= 2)) {
				finalAliases.add(s);
			}
		}
		for (String s : finalAliases) {
			this.subs.put(s, sub);
		}
	}

	public MutableParameterSet removeSubcommand(String subName) {
		return this.subs.remove(subName);
	}

	public Set<String> getSubcommands() {
		return new LinkedHashSet<>(this.subs.keySet());
	}
}