package com.armedia.caliente.cli.parser;

import java.util.List;
import java.util.Set;

import com.armedia.commons.utilities.Tools;

public abstract class Parameter {

	public static final char DEFAULT_VALUE_SEP = ',';

	public abstract String getKey();

	public abstract boolean isRequired();

	public abstract String getDescription();

	public abstract String getLongOpt();

	public abstract Character getShortOpt();

	public abstract Character getValueSep();

	public abstract Set<String> getAllowedValues();

	public abstract String getValueName();

	public abstract int getMinValueCount();

	public abstract int getMaxValueCount();

	public abstract String getDefault();

	public abstract List<String> getDefaults();

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
		if (!Parameter.isEquivalent(a, b)) { return false; }
		if (!Tools.equals(a.isRequired(), b.isRequired())) { return false; }
		if (!Tools.equals(a.getDescription(), b.getDescription())) { return false; }
		if (!Tools.equals(a.getValueName(), b.getValueName())) { return false; }
		if (!Tools.equals(a.getAllowedValues(), b.getAllowedValues())) { return false; }
		if (!Tools.equals(a.getDefaults(), b.getDefaults())) { return false; }
		return true;
	}
}