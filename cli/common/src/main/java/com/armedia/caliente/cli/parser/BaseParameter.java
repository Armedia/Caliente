package com.armedia.caliente.cli.parser;

import com.armedia.commons.utilities.Tools;

public abstract class BaseParameter implements Parameter {

	public static final char DEFAULT_VALUE_SEP = ',';

	public static String calculateKey(Parameter def) {
		if (def == null) { throw new IllegalArgumentException(
			"Must provide a parameter definition to calculate a key for"); }
		return BaseParameter.calculateKey(def.getLongOpt(), def.getShortOpt());
	}

	public static String calculateKey(String longOpt, String shortOpt) {
		if ((longOpt == null) && (shortOpt == null)) { throw new IllegalArgumentException(
			"Must provide one or both short and long options"); }
		String opt = (longOpt != null ? longOpt : shortOpt.toString());
		String prefix = (longOpt != null ? "-" : "");
		return String.format("-%s%s", prefix, opt);
	}

	public static String calculateKey(String longOpt, Character shortOpt) {
		return BaseParameter.calculateKey(longOpt, shortOpt != null ? shortOpt.toString() : null);
	}

	public static boolean isEquivalent(Parameter a, Parameter b) {
		if (a == b) { return true; }
		if ((a == null) || (b == null)) { return false; }
		if (!Tools.equals(a.getKey(), b.getKey())) { return false; }
		if (!Tools.equals(a.getShortOpt(), b.getShortOpt())) { return false; }
		if (!Tools.equals(a.getLongOpt(), b.getLongOpt())) { return false; }
		if (!Tools.equals(a.getMinValueCount(), b.getMinValueCount())) { return false; }
		if (!Tools.equals(a.getMaxValueCount(), b.getMaxValueCount())) { return false; }
		if (!Tools.equals(a.getValueSep(), b.getValueSep())) { return false; }
		if (!Tools.equals(a.getAllowedValues(), b.getAllowedValues())) { return false; }
		return true;
	}

	public static boolean isIdentical(Parameter a, Parameter b) {
		if (!BaseParameter.isEquivalent(a, b)) { return false; }
		if (!Tools.equals(a.isRequired(), b.isRequired())) { return false; }
		if (!Tools.equals(a.getDescription(), b.getDescription())) { return false; }
		if (!Tools.equals(a.getValueName(), b.getValueName())) { return false; }
		return true;
	}
}