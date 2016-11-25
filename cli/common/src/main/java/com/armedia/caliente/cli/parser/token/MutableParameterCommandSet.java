package com.armedia.caliente.cli.parser.token;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.armedia.commons.utilities.Tools;

public final class MutableParameterCommandSet extends MutableParameterSet implements ParameterSchema, Cloneable {

	private final Map<String, ParameterSubSchema> subs = new TreeMap<>();
	private final Map<String, String> aliasToName = new TreeMap<>();
	private final Map<String, Set<String>> aliases = new TreeMap<>();

	private boolean requiresSubSet = false;

	public MutableParameterCommandSet() {
		this(null);
	}

	public MutableParameterCommandSet(ParameterSchema other) {
		super(other);
		if (other != null) {
			for (String s : other.getSubSetNames()) {
				ParameterSubSchema set = other.getSubSet(s);
				Set<String> a = Tools.freezeCopy(other.getSubSetAliases(s), true);
				this.subs.put(s, new MutableParameterSet(set));
				this.aliases.put(s, a);
				for (String alias : a) {
					this.aliasToName.put(alias, s);
				}
				this.requiresSubSet = other.isRequiresSubSet();
			}
		}
	}

	@Override
	public MutableParameterCommandSet clone() {
		return new MutableParameterCommandSet(this);
	}

	@Override
	public ImmutableParameterCommandSet freezeCopy() {
		return new ImmutableParameterCommandSet(this);
	}

	@Override
	public ParameterSubSchema getSubSet(String subName) {
		String alias = this.aliasToName.get(subName);
		if (alias != null) {
			subName = alias;
		}
		return this.subs.get(subName);
	}

	@Override
	public boolean hasSubSet(String subName) {
		return this.subs.containsKey(subName) || this.aliasToName.containsKey(subName);
	}

	public void addSubSet(String subName, ParameterSubSchema sub, String... aliases) {
		if (sub == null) { throw new IllegalArgumentException("Must provide a subset"); }
		if (!validateLong(subName)) { throw new IllegalArgumentException(String.format(
			"The string [%s] is not a valid subset name - it may not be null, the empty string, or contain whitespace",
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

	public ParameterSubSchema removeSubSet(String subName) {
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

	@Override
	public String getSubSetName(String alias) {
		if (this.aliasToName.containsKey(alias)) { return this.aliasToName.get(alias); }
		if (this.subs.containsKey(alias)) { return alias; }
		return null;
	}

	@Override
	public Set<String> getSubSetNames() {
		return new LinkedHashSet<>(this.subs.keySet());
	}

	@Override
	public Set<String> getSubSetAliases(String subName) {
		Set<String> ret = this.aliases.get(subName);
		if (ret == null) { return null; }
		return new LinkedHashSet<>(ret);
	}

	@Override
	public boolean isRequiresSubSet() {
		return this.requiresSubSet;
	}

	public void setRequiresSubSet(boolean requiresSubSet) {
		this.requiresSubSet = requiresSubSet;
	}
}