package com.armedia.caliente.cli;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class ParameterScheme {

	private final Map<String, Parameter> parameters = new TreeMap<>();
	private final Map<String, Parameter> longKeys = new HashMap<>();
	private final Map<Character, Parameter> shortKeys = new HashMap<>();
	private int minArgs = 0;
	private int maxArgs = -1;

	/**
	 * Returns {@code true} if this parameter scheme supports positional arguments, {@code false}
	 * otherwise. A ParameterScheme supports positional arguments if {@code maxArgs != 0}.
	 *
	 * @return {@code true} if this parameter scheme supports positional arguments, {@code false}
	 *         otherwise
	 */
	public boolean isSupportsArguments() {
		return (this.maxArgs != 0);
	}

	/**
	 * Returns the minimum number of allowed positional arguments. The number will always be a
	 * positive integer, where 0 means there is no minimum set.
	 *
	 * @return the minimum number of allowed positional arguments
	 */
	public int getMinArgs() {
		return this.minArgs;
	}

	/**
	 * <p>
	 * Set the minimum number of allowed positional arguments. Negative integers will be converted
	 * to 0.
	 * </p>
	 * <p>
	 * If the number given is less restrictive than the configured maximum, the maximum will be
	 * adjusted appropriately.
	 * </p>
	 *
	 * @param minArgs
	 *            the minimum number of allowed positional arguments
	 */
	public void setMinArgs(int minArgs) {
		this.minArgs = Math.max(0, minArgs);
		if ((this.minArgs > 0) && (this.maxArgs >= 0) && (this.minArgs > this.maxArgs)) {
			this.maxArgs = this.minArgs;
		}
	}

	/**
	 * Returns the maximum number of allowed positional arguments. A value of {@code -1} means that
	 * there is no upper limit. A value of 0 means that no arguments are allowed. Any other number
	 * is the maximum number of allowed positional arguments.
	 *
	 * @return the maximum number of allowed positional arguments
	 */
	public int getMaxArgs() {
		return this.maxArgs;
	}

	/**
	 * <p>
	 * Sets the maximum number of allowed positional arguments. Negative values will be converted to
	 * {@code -1}, meaning that there is no upper limit. A value of 0 means that no arguments are
	 * allowed. Any other number is the maximum number of allowed positional arguments.
	 * </p>
	 * <p>
	 * If the number given is more restrictive than the configured minimum, the minimum will be
	 * adjusted appropriately.
	 * </p>
	 *
	 * @param maxArgs
	 *            the maximum number of allowed positional arguments
	 */
	public void setMaxArgs(int maxArgs) {
		this.maxArgs = Math.max(-1, maxArgs);
		if ((this.minArgs > 0) && (this.maxArgs >= 0) && (this.minArgs > this.maxArgs)) {
			this.minArgs = this.maxArgs;
		}
	}

	private static Collection<Parameter> buildCollection(Parameter a, Parameter b) {
		if ((a == null) && (b == null)) { return null; }
		if ((a == null) || (b == null)) { return Collections.unmodifiableCollection(Arrays.asList(a != null ? a : b)); }
		if (a == b) { return Collections.unmodifiableCollection(Collections.singleton(a)); }
		return Collections.unmodifiableCollection(Arrays.asList(a, b));
	}

	private Collection<Parameter> replace(Parameter parameter, boolean add) {
		if (parameter == null) { throw new IllegalArgumentException(
			String.format("Must provide a parameter to %s", add ? "add" : "remove")); }

		// Make sure nobody can change anything on us while we work
		parameter = new ImmutableParameter(parameter);

		final String longOpt = parameter.getLongOpt();
		final Parameter oldLong = remove(longOpt);
		final Character shortOpt = parameter.getShortOpt();
		final Parameter oldShort = remove(shortOpt);

		if (add) {
			this.longKeys.put(longOpt, parameter);
			this.shortKeys.put(shortOpt, parameter);
			this.parameters.put(parameter.getKey(), parameter);
		}
		return ParameterScheme.buildCollection(oldShort, oldLong);
	}

	/**
	 * Adds the given parameter to this parameter scheme, returning any and all parameters (a
	 * maximum of 2) that may have collided with the given parameter's short or long option forms.
	 * This means that a collection of at most two parameters may be returned. If {@code null} is
	 * returned, then there was no collision.
	 *
	 * @param parameter
	 *            the parameter to add
	 * @return the parameters that were replaced.
	 */
	public Collection<Parameter> add(Parameter parameter) {
		return replace(parameter, true);
	}

	/**
	 * Remove any and all parameters (a maximum of 2) that may collide with the given parameter's
	 * short or long option forms. If {@code null} is returned, then there was no collision.
	 *
	 * @param parameter
	 *            the parameter to check against
	 * @return the parameters that were removed
	 */
	public Collection<Parameter> remove(Parameter parameter) {
		return replace(parameter, false);
	}

	/**
	 * Remove the parameter which matches the given long option
	 *
	 * @param longOpt
	 * @return the parameter which matches the given long option, or {@code null} if none matches.
	 */
	public Parameter remove(String longOpt) {
		if (longOpt == null) { return null; }
		Parameter old = this.longKeys.remove(longOpt);
		if (old == null) { return null; }
		this.parameters.remove(old.getKey());
		Character shortOpt = old.getShortOpt();
		if (shortOpt != null) {
			this.shortKeys.remove(shortOpt);
		}
		return old;
	}

	public Parameter remove(Character shortOpt) {
		if (shortOpt == null) { return null; }
		Parameter old = this.shortKeys.remove(shortOpt);
		if (old == null) { return null; }
		this.parameters.remove(old.getKey());
		String longOpt = old.getLongOpt();
		if (longOpt != null) {
			this.longKeys.remove(longOpt);
		}
		return old;
	}

	/**
	 * Returns a {@link Collection} containing the parameters defined in this scheme. The collection
	 * returned is independent and changes to it will not reflect on this ParameterScheme instance.
	 *
	 * @return a {@link Collection} containing the parameters defined in this scheme
	 */
	public Collection<Parameter> getParameters() {
		return new ArrayList<>(this.parameters.values());
	}

	/**
	 * Returns the number of parameters defined in this scheme.
	 *
	 * @return the number of parameters defined in this scheme.
	 */
	public int getParameterCount() {
		return this.parameters.size();
	}
}