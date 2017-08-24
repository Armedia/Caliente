package com.armedia.caliente.cli;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ParameterTools {

	/**
	 * <p>
	 * Produces a list of the {@link ParameterDefinition} instances wrapped by the given wrappers. All
	 * {@code null} values are filtered out and not preserved. If the array given is {@code null},
	 * the {@code null} value is returned.
	 * </p>
	 *
	 * @param wrappers
	 * @return a list of the {@link ParameterDefinition} instances wrapped by the given wrappers
	 */
	public static List<ParameterDefinition> getUnwrappedList(ParameterWrapper... wrappers) {
		if (wrappers == null) { return null; }
		return ParameterTools.getUnwrappedList(Arrays.asList(wrappers));
	}

	/**
	 * <p>
	 * Produces a list of the {@link ParameterDefinition} instances wrapped by the given wrappers. All
	 * {@code null} values are filtered out and not preserved. If the collection given is
	 * {@code null}, the {@code null} value is returned.
	 * </p>
	 *
	 * @param wrappers
	 * @return a list of the {@link ParameterDefinition} instances wrapped by the given wrappers
	 */
	public static List<ParameterDefinition> getUnwrappedList(Collection<ParameterWrapper> wrappers) {
		if (wrappers == null) { return null; }
		List<ParameterDefinition> l = new ArrayList<>(wrappers.size());
		for (ParameterWrapper d : wrappers) {
			if (d == null) {
				continue;
			}
			ParameterDefinition p = d.getParameter();
			if (p == null) {
				continue;
			}
			l.add(p);
		}
		return l;
	}

	/**
	 * <p>
	 * Extracts the {@link ParameterDefinition} instance from the given wrapper. It'll return {@code null} if
	 * either the wrapper itself is {@code null}, or if its wrapped parameter is {@code null}.
	 * </p>
	 *
	 * @param wrapper
	 * @return the {@link ParameterDefinition} instance from the given wrapper
	 */
	public static ParameterDefinition unwrap(ParameterWrapper wrapper) {
		if (wrapper == null) { return null; }
		return wrapper.getParameter();
	}
}