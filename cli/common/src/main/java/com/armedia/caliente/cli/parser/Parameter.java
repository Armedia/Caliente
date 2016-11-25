package com.armedia.caliente.cli.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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

	static String calculateKey(Parameter def) {
		if (def == null) { throw new IllegalArgumentException(
			"Must provide a parameter definition to calculate a key for"); }
		return Parameter.calculateKey(def.getLongOpt(), def.getShortOpt());
	}

	static String calculateKey(String longOpt, String shortOpt) {
		if ((longOpt == null) && (shortOpt == null)) { throw new IllegalArgumentException(
			"Must provide one or both short and long options"); }
		String opt = (longOpt != null ? longOpt : shortOpt.toString());
		String prefix = (longOpt != null ? "-" : "");
		return String.format("-%s%s", prefix, opt);
	}

	static String calculateKey(String longOpt, Character shortOpt) {
		return Parameter.calculateKey(longOpt, shortOpt != null ? shortOpt.toString() : null);
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
		if (!Parameter.isEquivalent(a, b)) { return false; }
		if (!Tools.equals(a.isRequired(), b.isRequired())) { return false; }
		if (!Tools.equals(a.getDescription(), b.getDescription())) { return false; }
		if (!Tools.equals(a.getValueName(), b.getValueName())) { return false; }
		if (!Tools.equals(a.getAllowedValues(), b.getAllowedValues())) { return false; }
		if (!Tools.equals(a.getDefaults(), b.getDefaults())) { return false; }
		return true;
	}

	/**
	 * <p>
	 * Produces a list of the {@link Parameter} instances wrapped by the given wrappers. All
	 * {@code null} values are filtered out and not preserved. If the array given is {@code null},
	 * the {@code null} value is returned.
	 * </p>
	 *
	 * @param wrappers
	 * @return a list of the {@link Parameter} instances wrapped by the given wrappers
	 */
	public static List<Parameter> getUnwrappedList(ParameterWrapper... wrappers) {
		if (wrappers == null) { return null; }
		return Parameter.getUnwrappedList(Arrays.asList(wrappers));
	}

	/**
	 * <p>
	 * Produces a list of the {@link Parameter} instances wrapped by the given wrappers. All
	 * {@code null} values are filtered out and not preserved. If the collection given is
	 * {@code null}, the {@code null} value is returned.
	 * </p>
	 *
	 * @param wrappers
	 * @return a list of the {@link Parameter} instances wrapped by the given wrappers
	 */
	public static List<Parameter> getUnwrappedList(Collection<ParameterWrapper> wrappers) {
		if (wrappers == null) { return null; }
		List<Parameter> l = new ArrayList<>(wrappers.size());
		for (ParameterWrapper d : wrappers) {
			if (d == null) {
				continue;
			}
			Parameter p = d.getParameter();
			if (p == null) {
				continue;
			}
			l.add(p);
		}
		return l;
	}

	/**
	 * <p>
	 * Extracts the {@link Parameter} instance from the given wrapper. It'll return {@code null} if
	 * either the wrapper itself is {@code null}, or if its wrapped parameter is {@code null}.
	 * </p>
	 *
	 * @param wrapper
	 * @return the {@link Parameter} instance from the given wrapper
	 */
	public static Parameter unwrap(ParameterWrapper wrapper) {
		if (wrapper == null) { return null; }
		return wrapper.getParameter();
	}
}