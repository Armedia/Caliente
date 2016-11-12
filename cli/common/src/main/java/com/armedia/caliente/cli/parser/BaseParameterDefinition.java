package com.armedia.caliente.cli.parser;

import com.armedia.commons.utilities.Tools;

public abstract class BaseParameterDefinition implements ParameterDefinition {

	public static final char DEFAULT_VALUE_SEP = ',';

	@Override
	public String getKey() {
		return BaseParameterDefinition.calculateKey(getLongOpt(), getShortOpt());
	}

	@Override
	public int hashCode() {
		return getKey().hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) { return true; }
		if (!Tools.baseEquals(this, o)) { return false; }
		BaseParameterDefinition other = BaseParameterDefinition.class.cast(o);
		return Tools.equals(this.getKey(), other.getKey());
	}

	static String calculateKey(ParameterDefinition def) {
		if (def == null) { throw new IllegalArgumentException(
			"Must provide a parameter definition to calculate a key for"); }
		return BaseParameterDefinition.calculateKey(def.getLongOpt(), def.getShortOpt());
	}

	static String calculateKey(String longOpt, String shortOpt) {
		if ((longOpt == null) && (shortOpt == null)) { throw new IllegalArgumentException(
			"Must provide one or both short and long options"); }
		String opt = (longOpt != null ? longOpt : shortOpt.toString());
		String prefix = (longOpt != null ? "-" : "");
		return String.format("-%s%s", prefix, opt);
	}

	static String calculateKey(String longOpt, Character shortOpt) {
		return BaseParameterDefinition.calculateKey(longOpt, shortOpt != null ? shortOpt.toString() : null);
	}
}