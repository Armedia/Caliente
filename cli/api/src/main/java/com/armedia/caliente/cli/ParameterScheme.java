package com.armedia.caliente.cli;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class ParameterScheme {

	private final String name;
	private final Map<String, Parameter> requiredParameters = new TreeMap<>();
	private final Map<String, Parameter> parameters = new TreeMap<>();
	private final Map<String, Parameter> longKeys = new HashMap<>();
	private final Map<Character, Parameter> shortKeys = new HashMap<>();
	private int minArgs = 0;
	private int maxArgs = -1;

	/**
	 * @param name
	 */
	public ParameterScheme(String name) {
		if (name == null) { throw new IllegalArgumentException("Must provide a non-null name"); }
		this.name = name;
	}

	/**
	 * Returns this scheme's given name.
	 *
	 * @return this scheme's given name
	 */
	public String getName() {
		return this.name;
	}

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
	 * to 0, to indicate that no minimum is required.
	 * </p>
	 * <p>
	 * If the number given is higher than the configured maximum, the maximum will be adjusted to
	 * match.
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
	 * {@code -1}, to indicate that there is no upper limit. A value of 0 means that no arguments
	 * are allowed. Any other number is the maximum number of allowed positional arguments.
	 * </p>
	 * <p>
	 * If the number given is lower than the configured minimum, the minimum will be adjusted to
	 * match.
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

	/**
	 * Find the parameter in this scheme which matches the given long option
	 *
	 * @param longOpt
	 *            the long option
	 * @return the parameter in this scheme which matches the given long option
	 */
	protected final Parameter findParameter(String longOpt) {
		if (longOpt == null) { return null; }
		return this.longKeys.get(longOpt);
	}

	/**
	 * Find the parameter in this scheme which matches the given short option
	 *
	 * @param shortOpt
	 *            the short option
	 * @return the parameter in this scheme which matches the given short option
	 */
	protected final Parameter findParameter(Character shortOpt) {
		if (shortOpt == null) { return null; }
		return this.shortKeys.get(shortOpt);
	}

	private final Collection<Parameter> replaceParameter(Parameter parameter, boolean add) {
		if (parameter == null) { throw new IllegalArgumentException(
			String.format("Must provide a parameter to %s", add ? "add" : "remove")); }

		// Make sure nobody can change anything on us while we work
		parameter = ParameterTools.ensureImmutable(parameter);

		final String longOpt = parameter.getLongOpt();
		final Parameter oldLong = (add ? findParameter(longOpt) : removeParameter(longOpt));
		final Character shortOpt = parameter.getShortOpt();
		final Parameter oldShort = (add ? findParameter(shortOpt) : removeParameter(shortOpt));

		if (add) {
			if ((oldLong != null) || (oldShort != null)) {
				Object msgParam = null;
				if ((oldLong != null) && (oldShort != null)) {
					msgParam = ParameterScheme.buildCollection(oldShort, oldLong);
				} else if (oldLong != null) {
					msgParam = oldLong;
				} else {
					msgParam = oldShort;
				}

				throw new IllegalArgumentException(
					String.format("The given parameter %s would collide with %s", parameter, msgParam));
			}

			if (longOpt != null) {
				removeParameter(longOpt);
				this.longKeys.put(longOpt, parameter);
			}
			if (shortOpt != null) {
				removeParameter(shortOpt);
				this.shortKeys.put(shortOpt, parameter);
			}
			this.parameters.put(parameter.getKey(), parameter);
			if (parameter.isRequired()) {
				this.requiredParameters.put(parameter.getKey(), parameter);
			}
		}
		return ParameterScheme.buildCollection(oldShort, oldLong);
	}

	/**
	 * Adds the given parameter to this parameter scheme.
	 *
	 * @param parameter
	 *            the parameter to add
	 * @throws IllegalArgumentException
	 *             if the given parameter collides with any already-existing parameters (you can
	 *             check with {@link #hasParameter(Character)}, {@link #hasParameter(String)}, or
	 *             {@link #countCollisions(Parameter)}
	 */
	public final void addParameter(Parameter parameter) {
		replaceParameter(parameter, true);
	}

	/**
	 * Adds all the given parameters (i.e. invoke {@link #addParameter(Parameter) add} for each
	 * non-{@code null} parameter in the array).
	 *
	 * @param parameters
	 */
	public final void addParameters(Parameter... parameters) {
		if (parameters == null) { throw new IllegalArgumentException("Must provide a non-null parameter array"); }
		for (Parameter p : parameters) {
			if (p != null) {
				addParameter(p);
			}
		}
	}

	/**
	 * Remove any and all parameters (a maximum of 2) that may collide with the given parameter's
	 * short or long option forms. If {@code null} is returned, then there was no collision.
	 *
	 * @param parameter
	 *            the parameter to check against
	 * @return the parameters that were removed
	 */
	public final Collection<Parameter> removeParameter(Parameter parameter) {
		return replaceParameter(parameter, false);
	}

	/**
	 * Remove the parameter which matches the given long option
	 *
	 * @param longOpt
	 *            the long option
	 * @return the parameter which matches the given long option, or {@code null} if none matches.
	 */
	public final Parameter removeParameter(String longOpt) {
		if (longOpt == null) { return null; }
		Parameter old = this.longKeys.remove(longOpt);
		if (old == null) { return null; }
		this.parameters.remove(old.getKey());
		this.requiredParameters.remove(old.getKey());
		Character shortOpt = old.getShortOpt();
		if (shortOpt != null) {
			this.shortKeys.remove(shortOpt);
		}
		return old;
	}

	/**
	 * Remove the parameter which matches the given short option
	 *
	 * @param shortOpt
	 *            the short option
	 * @return the parameter which matches the given short option, or {@code null} if none matches.
	 */
	public final Parameter removeParameter(Character shortOpt) {
		if (shortOpt == null) { return null; }
		Parameter old = this.shortKeys.remove(shortOpt);
		if (old == null) { return null; }
		this.parameters.remove(old.getKey());
		this.requiredParameters.remove(old.getKey());
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
	public final Collection<Parameter> getParameters() {
		return new ArrayList<>(this.parameters.values());
	}

	/**
	 * Returns a collection of the parameters in this scheme that have required flag set (as
	 * determined by {@link Parameter#isRequired()}). The collection returned is a copy, and changes
	 * to it do not affect this scheme's contents.
	 *
	 * @return the parameters in this scheme that have required flag set
	 */
	public final Collection<Parameter> getRequiredParameters() {
		return new ArrayList<>(this.requiredParameters.values());
	}

	/**
	 * Returns the number of parameters in this scheme that have required flag set (as determined by
	 * {@link Parameter#isRequired()}).
	 *
	 * @return the number of parameters in this scheme that have required flag set
	 */
	public final int getRequiredParameterCount() {
		return this.requiredParameters.size();
	}

	/**
	 * Returns the number of parameters defined in this scheme.
	 *
	 * @return the number of parameters defined in this scheme.
	 */
	public final int getParameterCount() {
		return this.parameters.size();
	}

	/**
	 * Returns {@code true} if this scheme contains a parameter that uses the given short option,
	 * {@code false} otherwise.
	 *
	 * @param shortOpt
	 * @return {@code true} if this scheme contains a parameter that uses the given short option,
	 *         {@code false} otherwise.
	 */
	public final boolean hasParameter(Character shortOpt) {
		return (findParameter(shortOpt) != null);
	}

	/**
	 * Returns {@code true} if this scheme contains a parameter that uses the given long option,
	 * {@code false} otherwise.
	 *
	 * @param longOpt
	 * @return {@code true} if this scheme contains a parameter that uses the given long option,
	 *         {@code false} otherwise.
	 */
	public final boolean hasParameter(String longOpt) {
		return (findParameter(longOpt) != null);
	}

	/**
	 * Returns the number of parameters already in this scheme that would collide with the given
	 * parameter based on short or long options. This means that only 3 values can be returned: 0,
	 * 1, or 2.
	 *
	 * @param parameter
	 * @return the number of parameters already in this scheme that would collide with the given
	 *         parameter based on short or long options
	 */
	public final int countCollisions(Parameter parameter) {
		if (parameter == null) { throw new IllegalArgumentException("Must provide a non-null parameter"); }
		String longOpt = parameter.getLongOpt();
		Parameter longParam = null;
		if (longOpt != null) {
			longParam = this.longKeys.get(longOpt);
		}
		Character shortOpt = parameter.getShortOpt();
		Parameter shortParam = null;
		if (shortOpt != null) {
			shortParam = this.shortKeys.get(shortOpt);
		}
		// If both are null, then there are no collisions
		if ((shortParam == null) && (longParam == null)) { return 0; }
		// If only one is null, then there is one collision
		if ((shortParam == null) || (longParam == null)) { return 1; }
		// If neither is null, then there can be 1 or 2 collisions depending on whether they're the
		// same object, or different objects
		return (shortParam == longParam ? 1 : 2);
	}
}