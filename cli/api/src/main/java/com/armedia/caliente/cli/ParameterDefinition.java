package com.armedia.caliente.cli;

import java.util.List;
import java.util.Set;

import com.armedia.commons.utilities.Tools;

public abstract class ParameterDefinition {

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

	/**
	 * <p>
	 * Checks for conflict between the parameters. Two parameters {@code a} and {@code b} are said
	 * to be in conflict if and only if the following methods return identical (as per
	 * {@link Object#equals(Object)} values:
	 * </p>
	 * <ul>
	 * <li>{@link #getShortOpt()}</li>
	 * <li>{@link #getLongOpt()}</li>
	 * </ul>
	 *
	 * @param a
	 * @param b
	 * @return {@code true} if there's a conflict between a and b, {@code false} otherwise
	 */
	public static boolean isConflicting(ParameterDefinition a, ParameterDefinition b) {
		if (a == b) { return true; }
		if ((a == null) || (b == null)) { return false; }
		if (!Tools.equals(a.getShortOpt(), b.getShortOpt())) { return false; }
		if (!Tools.equals(a.getLongOpt(), b.getLongOpt())) { return false; }
		return true;
	}

	/**
	 * <p>
	 * Checks for the equivalency between the parameters. Two parameters {@code a} and {@code b} are
	 * said to be equivalent if they're conflicting (as per
	 * {@link #isConflicting(ParameterDefinition, ParameterDefinition)}, and the following methods
	 * return identical (as per {@link Object#equals(Object)} values:
	 * </p>
	 * <ul>
	 * <li>{@link #getMinValueCount()}</li>
	 * <li>{@link #getMaxValueCount()}</li>
	 * <li>{@link #getValueSep()}</li>
	 * <li>{@link #getAllowedValues()}</li>
	 * </ul>
	 *
	 * @param a
	 * @param b
	 * @return {@code true} if the parameters are equivalent, {@code false} otherwise
	 */
	public static boolean isEquivalent(ParameterDefinition a, ParameterDefinition b) {
		if (!ParameterDefinition.isConflicting(a, b)) { return false; }
		if (!Tools.equals(a.getMinValueCount(), b.getMinValueCount())) { return false; }
		if (!Tools.equals(a.getMaxValueCount(), b.getMaxValueCount())) { return false; }
		if (!Tools.equals(a.getValueSep(), b.getValueSep())) { return false; }
		if (!Tools.equals(a.getAllowedValues(), b.getAllowedValues())) { return false; }
		return true;
	}

	/**
	 * <p>
	 * Checks two parameters to see if they're identical in every respect. Two parameters {@code a}
	 * and {@code b} are said to be identical if they're equivalent (as per
	 * {@link #isEquivalent(ParameterDefinition, ParameterDefinition)}, and the following methods
	 * return identical (as per {@link Object#equals(Object)} values:
	 * </p>
	 * <ul>
	 * <li>{@link #getMinValueCount()}</li>
	 * <li>{@link #getMaxValueCount()}</li>
	 * <li>{@link #getValueSep()}</li>
	 * <li>{@link #getAllowedValues()}</li>
	 * </ul>
	 *
	 * @param a
	 * @param b
	 * @return {@code true} if the parameters are identical, {@code false} otherwise
	 */
	public static boolean isIdentical(ParameterDefinition a, ParameterDefinition b) {
		if (!ParameterDefinition.isEquivalent(a, b)) { return false; }
		if (!Tools.equals(a.isRequired(), b.isRequired())) { return false; }
		if (!Tools.equals(a.getDescription(), b.getDescription())) { return false; }
		if (!Tools.equals(a.getValueName(), b.getValueName())) { return false; }
		if (!Tools.equals(a.getAllowedValues(), b.getAllowedValues())) { return false; }
		if (!Tools.equals(a.getDefaults(), b.getDefaults())) { return false; }
		return true;
	}

	static String calculateKey(ParameterDefinition def) {
		if (def == null) { throw new IllegalArgumentException(
			"Must provide a parameter definition to calculate a key for"); }
		return ParameterDefinition.calculateKey(def.getLongOpt(), def.getShortOpt());
	}

	static String calculateKey(String longOpt, Character shortOpt) {
		return ParameterDefinition.calculateKey(longOpt, shortOpt != null ? shortOpt.toString() : null);
	}

	public static String calculateKey(String longOpt, String shortOpt) {
		if ((longOpt == null) && (shortOpt == null)) { throw new IllegalArgumentException(
			"Must provide at least one short or long option"); }
		String opt = (longOpt != null ? longOpt : shortOpt.toString());
		String prefix = (longOpt != null ? "-" : "");
		return String.format("-%s%s", prefix, opt);
	}
}