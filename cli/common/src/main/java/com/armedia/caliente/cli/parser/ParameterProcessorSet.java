package com.armedia.caliente.cli.parser;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.armedia.commons.utilities.Tools;

public class ParameterProcessorSet extends ParameterProcessor {

	private static final ParserErrorPolicy DEFAULT_ERROR_POLICY = new BasicParserErrorPolicy();

	private final Map<String, ParameterProcessor> subs = new TreeMap<>();
	private final Map<String, String> aliasToName = new TreeMap<>();
	private final Map<String, Set<String>> aliases = new TreeMap<>();

	public ParameterProcessorSet(String name) {
		super(name);
	}

	public ParameterProcessor getSubProcessor(String subName) {
		String alias = this.aliasToName.get(subName);
		if (alias != null) {
			subName = alias;
		}
		return this.subs.get(subName);
	}

	public boolean hasSubProcessor(String subName) {
		return this.subs.containsKey(subName) || this.aliasToName.containsKey(subName);
	}

	public void addSubProcessor(String subName, ParameterProcessor sub, String... aliases) {
		if (sub == null) { throw new IllegalArgumentException("Must provide a subprocessor"); }
		if (!validateLong(subName)) { throw new IllegalArgumentException(String.format(
			"The string [%s] is not a valid subprocessor name - it may not be null, the empty string, or contain whitespace",
			subName)); }

		this.subs.put(subName, sub);
		Set<String> finalAliases = new LinkedHashSet<>();
		for (String s : aliases) {
			if (validateLong(s) && (s.length() >= 2)) {
				finalAliases.add(s);
			}
		}
		this.aliases.put(subName, Tools.freezeSet(finalAliases));
		for (String s : finalAliases) {
			this.aliasToName.put(s, subName);
		}
	}

	public ParameterProcessor removeSubProcessor(String subName) {
		String alias = this.aliasToName.get(subName);
		if (alias != null) {
			subName = alias;
		}
		Set<String> aliases = this.aliases.remove(subName);
		if (aliases != null) {
			for (String a : aliases) {
				this.aliasToName.remove(a);
			}
		}
		return this.subs.remove(subName);
	}

	public String getSubprocessorName(String alias) {
		if (this.aliasToName.containsKey(alias)) { return this.aliasToName.get(alias); }
		if (this.subs.containsKey(alias)) { return alias; }
		return null;
	}

	public Set<String> getSubProcessorNames() {
		return new LinkedHashSet<>(this.subs.keySet());
	}

	public Set<String> getSubProcessorAliases(String subName) {
		Set<String> ret = this.aliases.get(subName);
		if (ret == null) { return null; }
		return new LinkedHashSet<>(ret);
	}

	@Override
	public ParserErrorPolicy getErrorPolicy() {
		return ParameterProcessorSet.DEFAULT_ERROR_POLICY;
	}
}