package com.armedia.caliente.cli.parser;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.armedia.commons.utilities.Tools;

public final class ImmutableParameterCommandSet extends ImmutableParameterSet
	implements ParameterSchema, Cloneable {

	private final Map<String, ParameterSet> subs;
	private final Map<String, String> aliasToName;
	private final Map<String, Set<String>> aliases;

	public ImmutableParameterCommandSet(ParameterSchema other) {
		super(other);
		Map<String, ParameterSet> subs = new LinkedHashMap<>();
		Map<String, String> aliasToName = new LinkedHashMap<>();
		Map<String, Set<String>> aliases = new LinkedHashMap<>();
		for (String s : other.getSubSetNames()) {
			ParameterSet set = other.getSubSet(s);
			Set<String> a = Tools.freezeCopy(other.getSubSetAliases(s), true);
			subs.put(s, new ImmutableParameterSet(set));
			aliases.put(s, a);
			for (String alias : a) {
				aliasToName.put(alias, s);
			}
		}
		this.subs = Tools.freezeMap(subs);
		this.aliasToName = Tools.freezeMap(aliasToName);
		this.aliases = Tools.freezeMap(aliases);
	}

	@Override
	public MutableParameterCommandSet thawCopy() {
		return new MutableParameterCommandSet(this);
	}

	@Override
	public ImmutableParameterCommandSet clone() {
		return new ImmutableParameterCommandSet(this);
	}

	@Override
	public ParameterSet getSubSet(String subName) {
		String alias = this.aliasToName.get(subName);
		if (alias != null) {
			subName = alias;
		}
		return this.subs.get(subName);
	}

	@Override
	public String getSubSetName(String alias) {
		if (this.aliasToName.containsKey(alias)) { return this.aliasToName.get(alias); }
		if (this.subs.containsKey(alias)) { return alias; }
		return null;
	}

	@Override
	public boolean hasSubSet(String subName) {
		return this.subs.containsKey(subName) || this.aliasToName.containsKey(subName);
	}

	@Override
	public Set<String> getSubSetNames() {
		return this.subs.keySet();
	}

	@Override
	public Set<String> getSubSetAliases(String subName) {
		return this.aliases.get(subName);
	}
}