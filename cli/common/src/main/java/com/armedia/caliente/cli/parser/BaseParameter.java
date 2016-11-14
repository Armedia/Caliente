package com.armedia.caliente.cli.parser;

import com.armedia.commons.utilities.Tools;

public abstract class BaseParameter implements Parameter {

	public static final char DEFAULT_VALUE_SEP = ',';

	@Override
	public String getKey() {
		return BaseParameter.calculateKey(getLongOpt(), getShortOpt());
	}

	@Override
	public int hashCode() {
		return getKey().hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) { return true; }
		if (!Tools.baseEquals(this, o)) { return false; }
		BaseParameter other = BaseParameter.class.cast(o);
		return Tools.equals(this.getKey(), other.getKey());
	}

	static String calculateKey(Parameter def) {
		if (def == null) { throw new IllegalArgumentException(
			"Must provide a parameter definition to calculate a key for"); }
		return BaseParameter.calculateKey(def.getLongOpt(), def.getShortOpt());
	}

	static String calculateKey(String longOpt, String shortOpt) {
		if ((longOpt == null) && (shortOpt == null)) { throw new IllegalArgumentException(
			"Must provide one or both short and long options"); }
		String opt = (longOpt != null ? longOpt : shortOpt.toString());
		String prefix = (longOpt != null ? "-" : "");
		return String.format("-%s%s", prefix, opt);
	}

	static String calculateKey(String longOpt, Character shortOpt) {
		return BaseParameter.calculateKey(longOpt, shortOpt != null ? shortOpt.toString() : null);
	}
}