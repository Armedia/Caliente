package com.armedia.caliente.cli;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.armedia.caliente.cli.parser.ParameterWrapper;

public class ParameterTools {

	public static String calculateKey(Parameter def) {
		if (def == null) { throw new IllegalArgumentException(
			"Must provide a parameter definition to calculate a key for"); }
		return ParameterTools.calculateKey(def.getLongOpt(), def.getShortOpt());
	}

	public static String calculateKey(String longOpt, String shortOpt) {
		if ((longOpt == null) && (shortOpt == null)) { throw new IllegalArgumentException(
			"Must provide at least one short or long option"); }
		String opt = (longOpt != null ? longOpt : shortOpt.toString());
		String prefix = (longOpt != null ? "-" : "");
		return String.format("-%s%s", prefix, opt);
	}

	static String calculateKey(String longOpt, Character shortOpt) {
		return ParameterTools.calculateKey(longOpt, shortOpt != null ? shortOpt.toString() : null);
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
		return ParameterTools.getUnwrappedList(Arrays.asList(wrappers));
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