package com.armedia.caliente.cli;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class ParameterScheme {

	private final boolean caseSensitive;
	private final String name;
	private final Map<String, Parameter> requiredParameters = new TreeMap<>();
	private final Map<String, Parameter> parameters = new TreeMap<>();
	private final Map<String, Parameter> longKeys = new HashMap<>();
	private final Map<Character, Parameter> shortKeys = new HashMap<>();
	private int minArgs = 0;
	private int maxArgs = -1;
	private boolean dynamic = false;

	/**
	 * @param name
	 */
	public ParameterScheme(String name) {
		this(name, false);
	}

	public ParameterScheme(String name, boolean caseSensitive) {
		if (name == null) { throw new IllegalArgumentException("Must provide a non-null name"); }
		this.name = name;
		this.caseSensitive = caseSensitive;
	}

	public ParameterScheme(ParameterScheme pattern) {
		this.name = pattern.getName();
		this.requiredParameters.putAll(pattern.requiredParameters);
		this.parameters.putAll(pattern.parameters);
		this.longKeys.putAll(pattern.longKeys);
		this.shortKeys.putAll(pattern.shortKeys);
		this.minArgs = pattern.minArgs;
		this.maxArgs = pattern.maxArgs;
		this.dynamic = pattern.dynamic;
		this.caseSensitive = pattern.caseSensitive;
	}

	public boolean isCaseSensitive() {
		return this.caseSensitive;
	}

	protected final String canonicalize(String str) {
		if (str == null) { return null; }
		if (!this.caseSensitive) {
			str = str.toLowerCase();
		}
		return str;
	}

	protected final Character canonicalize(Character c) {
		if (c == null) { return null; }
		if (!this.caseSensitive) {
			c = Character.toLowerCase(c.charValue());
		}
		return c;
	}

	public boolean isDynamic() {
		return this.dynamic;
	}

	public ParameterScheme setDynamic(boolean dynamic) {
		this.dynamic = dynamic;
		return this;
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
	public ParameterScheme setMinArgs(int minArgs) {
		this.minArgs = Math.max(0, minArgs);
		if ((this.minArgs > 0) && (this.maxArgs >= 0) && (this.minArgs > this.maxArgs)) {
			this.maxArgs = this.minArgs;
		}
		return this;
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
	public ParameterScheme setMaxArgs(int maxArgs) {
		this.maxArgs = Math.max(-1, maxArgs);
		if ((this.minArgs > 0) && (this.maxArgs >= 0) && (this.minArgs > this.maxArgs)) {
			this.minArgs = this.maxArgs;
		}
		return this;
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
	public final Parameter getParameter(String longOpt) {
		if (longOpt == null) { return null; }
		return this.longKeys.get(canonicalize(longOpt));
	}

	/**
	 * Find the parameter in this scheme which matches the given short option
	 *
	 * @param shortOpt
	 *            the short option
	 * @return the parameter in this scheme which matches the given short option
	 */
	public final Parameter getParameter(Character shortOpt) {
		if (shortOpt == null) { return null; }
		return this.shortKeys.get(canonicalize(shortOpt));
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
	public final ParameterScheme addParameter(Parameter parameter)
		throws InvalidParameterException, DuplicateParameterException {
		if (parameter == null) { throw new IllegalArgumentException("Must provide a parameter to add"); }

		final String longOpt = canonicalize(parameter.getLongOpt());
		final Character shortOpt = canonicalize(parameter.getShortOpt());

		// TODO: Validate that the short and long options are valid

		final Parameter oldLong = getParameter(longOpt);
		final Parameter oldShort = getParameter(shortOpt);

		if ((oldLong != null) || (oldShort != null)) {
			Parameter existing = null;
			if ((oldLong != null) && (oldShort != null)) {
				existing = ParameterScheme.buildCollection(oldShort, oldLong).iterator().next();
			} else if (oldLong != null) {
				existing = oldLong;
			} else {
				existing = oldShort;
			}
			throw new DuplicateParameterException(
				String.format("The given parameter %s would collide with %s", parameter, existing), existing,
				parameter);
		}

		if (longOpt != null) {
			removeParameter(longOpt);
			this.longKeys.put(longOpt, parameter);
		}
		if (shortOpt != null) {
			removeParameter(shortOpt);
			this.shortKeys.put(shortOpt, parameter);
		}
		final String key = canonicalize(parameter.getKey());
		this.parameters.put(key, parameter);
		if (parameter.isRequired()) {
			this.requiredParameters.put(key, parameter);
		}
		return this;
	}

	/**
	 * Adds all the given parameters (i.e. invoke {@link #addParameter(Parameter) add} for each
	 * non-{@code null} parameter in the array).
	 *
	 * @param parameters
	 */
	public final ParameterScheme addParameters(Parameter... parameters)
		throws InvalidParameterException, DuplicateParameterException {
		if (parameters == null) { throw new IllegalArgumentException("Must provide a non-null parameter array"); }
		// TODO: Change this algorithm - first check that no parameters collide or are illegal, then
		// move on
		for (Parameter p : parameters) {
			if (p != null) {
				addParameter(p);
			}
		}
		return this;
	}

	/**
	 * Remove any and all parameters (a maximum of 2) that may collide with the given parameter's
	 * short or long option forms. If {@code null} is returned, then there was no collision.
	 *
	 * @param parameter
	 *            the parameter to check against
	 * @return the parameters that were removed
	 */
	public final Collection<Parameter> removeParameter(Parameter parameter)
		throws InvalidParameterException, DuplicateParameterException {
		if (parameter == null) { throw new IllegalArgumentException("Must provide a parameter to remove"); }

		final String longOpt = canonicalize(parameter.getLongOpt());
		final Parameter oldLong = removeParameter(longOpt);
		final Character shortOpt = canonicalize(parameter.getShortOpt());
		final Parameter oldShort = removeParameter(shortOpt);

		return ParameterScheme.buildCollection(oldShort, oldLong);
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
		Parameter old = this.longKeys.remove(canonicalize(longOpt));
		if (old == null) { return null; }
		final String oldKey = canonicalize(old.getKey());
		this.parameters.remove(oldKey);
		this.requiredParameters.remove(oldKey);
		Character shortOpt = canonicalize(old.getShortOpt());
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
		Parameter old = this.shortKeys.remove(canonicalize(shortOpt));
		if (old == null) { return null; }
		final String oldKey = canonicalize(old.getKey());
		this.parameters.remove(oldKey);
		this.requiredParameters.remove(oldKey);
		String longOpt = canonicalize(old.getLongOpt());
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
		return (getParameter(shortOpt) != null);
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
		return (getParameter(longOpt) != null);
	}

	/**
	 * Returns an integer between 0 and 3 where the low bit is the presence indicator for the short
	 * option parameter, and the high bit is the presence indicator for the long option parameter.
	 * This method does not take into account whether both flags are associated to the same
	 * parameter. Use {@link #findCollisions(Character, String)} for that.
	 *
	 * @param shortOpt
	 * @param longOpt
	 * @return an integer between 0 and 3 where the low bit is the presence indicator for the short
	 *         option parameter, and the high bit is the presence indicator for the long option
	 *         parameter.
	 */
	public final int hasEitherParameter(Character shortOpt, String longOpt) {
		int ret = 0;
		if (hasParameter(shortOpt)) {
			ret |= 1;
		}
		if (hasParameter(longOpt)) {
			ret |= (1 << 1);
		}
		return ret;
	}

	/**
	 * Returns {@code true} if and only if this parameter scheme contains a parameter that
	 * equivalent (as per {@link Parameter#isEquivalent(Parameter, Parameter)}) to the given
	 * parameter, {@code false} otherwise.
	 *
	 * @param parameter
	 * @return {@code true} if and only if this parameter scheme contains a parameter that exactly
	 *         matches the given parameter in both long and short options, {@code false} otherwise.
	 *
	 */
	public final boolean hasEquivalentParameter(Parameter parameter) {
		Collection<Parameter> collisions = findCollisions(parameter);
		if ((collisions == null) || (collisions.size() != 1)) { return false; }
		Parameter current = collisions.iterator().next();
		return Parameter.isEquivalent(parameter, current);
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
		Collection<Parameter> collisions = findCollisions(parameter);
		if ((collisions == null) || collisions.isEmpty()) { return 0; }
		return collisions.size();
	}

	/**
	 * Returns the parameters already in this scheme that would collide with the given parameter
	 * based on short or long options. If no collisions are found, {@code null} is returned. This
	 * means that the collection may contain either 1 or 2 elements.
	 *
	 * @param parameter
	 *            the parameter to check for
	 * @return the parameters already in this scheme that would collide with the given parameter
	 *         based on short or long options, or {@code null} if none collide
	 */
	public final Collection<Parameter> findCollisions(Parameter parameter) {
		if (parameter == null) { throw new IllegalArgumentException("Must provide a non-null parameter"); }
		return findCollisions(parameter.getShortOpt(), parameter.getLongOpt());
	}

	/**
	 * Returns the Parameter already in this scheme that would collide with the given short or long
	 * options. If no collisions are found, {@code null} is returned. This means that the collection
	 * may contain either 1 or 2 elements.
	 *
	 * @param shortOpt
	 *            the short option to check for
	 * @param longOpt
	 *            the long option to check for
	 * @return the Parameter already in this scheme that would collide with the given short or long
	 *         options, or {@code null} if none collide
	 */
	public final Collection<Parameter> findCollisions(Character shortOpt, String longOpt) {
		Parameter longParam = null;
		if (longOpt != null) {
			longParam = this.longKeys.get(canonicalize(longOpt));
		}
		Parameter shortParam = null;
		if (shortOpt != null) {
			shortParam = this.shortKeys.get(canonicalize(shortOpt));
		}
		return ParameterScheme.buildCollection(longParam, shortParam);
	}
}