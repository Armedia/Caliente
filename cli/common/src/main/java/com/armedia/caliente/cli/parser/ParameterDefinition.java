package com.armedia.caliente.cli.parser;

import com.armedia.commons.utilities.Tools;

public abstract class ParameterDefinition {

	public static final char DEFAULT_VALUE_SEP = ',';

	protected String getKey() {
		return ParameterDefinition.calculateKey(getLongOpt(), getShortOpt());
	}

	public abstract boolean isRequired();

	public abstract String getDescription();

	public abstract String getLongOpt();

	public abstract Character getShortOpt();

	public abstract Character getValueSep();

	public abstract String getValueName();

	public abstract int getValueCount();

	public abstract boolean isValueOptional();

	@Override
	public int hashCode() {
		return getKey().hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) { return true; }
		if (!Tools.baseEquals(this, o)) { return false; }
		ParameterDefinition other = ParameterDefinition.class.cast(o);
		return Tools.equals(this.getKey(), other.getKey());
	}

	static String calculateKey(ParameterDefinition def) {
		if (def == null) { throw new IllegalArgumentException(
			"Must provide a parameter definition to calculate a key for"); }
		return ParameterDefinition.calculateKey(def.getLongOpt(), def.getShortOpt());
	}

	static String calculateKey(String longOpt, String shortOpt) {
		if ((longOpt == null) && (shortOpt == null)) { throw new IllegalArgumentException(
			"Must provide one or both short and long options"); }
		String opt = (longOpt != null ? longOpt : shortOpt.toString());
		String prefix = (longOpt != null ? "-" : "");
		return String.format("-%s%s", prefix, opt);
	}

	static String calculateKey(String longOpt, Character shortOpt) {
		return ParameterDefinition.calculateKey(longOpt, shortOpt != null ? shortOpt.toString() : null);
	}
}