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
	private final Map<String, ParameterDefinition> requiredParameters = new TreeMap<>();
	private final Map<String, ParameterDefinition> parameterDefinitions = new TreeMap<>();
	private final Map<String, ParameterDefinition> longKeys = new HashMap<>();
	private final Map<Character, ParameterDefinition> shortKeys = new HashMap<>();
	private int minArgs = 0;
	private int maxArgs = -1;
	private boolean dynamic = false;

	/**
	 * @param name
	 */
	public ParameterScheme(String name) {
		if (name == null) { throw new IllegalArgumentException("Must provide a non-null name"); }
		this.name = name;
	}

	public ParameterScheme(ParameterScheme pattern) {
		this.name = pattern.getName();
		this.requiredParameters.putAll(pattern.requiredParameters);
		this.parameterDefinitions.putAll(pattern.parameterDefinitions);
		this.longKeys.putAll(pattern.longKeys);
		this.shortKeys.putAll(pattern.shortKeys);
		this.minArgs = pattern.minArgs;
		this.maxArgs = pattern.maxArgs;
		this.dynamic = pattern.dynamic;
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

	private static Collection<ParameterDefinition> buildCollection(ParameterDefinition a, ParameterDefinition b) {
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
	public final ParameterDefinition getParameter(String longOpt) {
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
	public final ParameterDefinition getParameter(Character shortOpt) {
		if (shortOpt == null) { return null; }
		return this.shortKeys.get(shortOpt);
	}

	private final Collection<ParameterDefinition> replaceParameter(ParameterDefinition parameterDefinition,
		boolean add) {
		if (parameterDefinition == null) { throw new IllegalArgumentException(
			String.format("Must provide a parameter to %s", add ? "add" : "remove")); }

		final String longOpt = parameterDefinition.getLongOpt();
		final ParameterDefinition oldLong = (add ? getParameter(longOpt) : removeParameter(longOpt));
		final Character shortOpt = parameterDefinition.getShortOpt();
		final ParameterDefinition oldShort = (add ? getParameter(shortOpt) : removeParameter(shortOpt));

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
					String.format("The given parameter %s would collide with %s", parameterDefinition, msgParam));
			}

			if (longOpt != null) {
				removeParameter(longOpt);
				this.longKeys.put(longOpt, parameterDefinition);
			}
			if (shortOpt != null) {
				removeParameter(shortOpt);
				this.shortKeys.put(shortOpt, parameterDefinition);
			}
			final String key = parameterDefinition.getKey();
			this.parameterDefinitions.put(key, parameterDefinition);
			if (parameterDefinition.isRequired()) {
				this.requiredParameters.put(key, parameterDefinition);
			}
		}
		return ParameterScheme.buildCollection(oldShort, oldLong);
	}

	/**
	 * Adds the given parameter to this parameter scheme.
	 *
	 * @param parameterDefinition
	 *            the parameter to add
	 * @throws IllegalArgumentException
	 *             if the given parameter collides with any already-existing parameterDefinitions
	 *             (you can check with {@link #hasParameter(Character)},
	 *             {@link #hasParameter(String)}, or {@link #countCollisions(ParameterDefinition)}
	 */
	public final ParameterScheme addParameter(ParameterDefinition parameterDefinition) {
		replaceParameter(parameterDefinition, true);
		return this;
	}

	/**
	 * Adds all the given parameterDefinitions (i.e. invoke
	 * {@link #addParameter(ParameterDefinition) add} for each non-{@code null} parameter in the
	 * array).
	 *
	 * @param parameters
	 */
	public final ParameterScheme addParameters(ParameterDefinition... parameters) {
		if (parameters == null) { throw new IllegalArgumentException("Must provide a non-null parameter array"); }
		for (ParameterDefinition p : parameters) {
			if (p != null) {
				addParameter(p);
			}
		}
		return this;
	}

	/**
	 * Remove any and all parameterDefinitions (a maximum of 2) that may collide with the given
	 * parameter's short or long option forms. If {@code null} is returned, then there was no
	 * collision.
	 *
	 * @param parameterDefinition
	 *            the parameter to check against
	 * @return the parameterDefinitions that were removed
	 */
	public final Collection<ParameterDefinition> removeParameter(ParameterDefinition parameterDefinition) {
		return replaceParameter(parameterDefinition, false);
	}

	/**
	 * Remove the parameter which matches the given long option
	 *
	 * @param longOpt
	 *            the long option
	 * @return the parameter which matches the given long option, or {@code null} if none matches.
	 */
	public final ParameterDefinition removeParameter(String longOpt) {
		if (longOpt == null) { return null; }
		ParameterDefinition old = this.longKeys.remove(longOpt);
		if (old == null) { return null; }
		final String oldKey = old.getKey();
		this.parameterDefinitions.remove(oldKey);
		this.requiredParameters.remove(oldKey);
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
	public final ParameterDefinition removeParameter(Character shortOpt) {
		if (shortOpt == null) { return null; }
		ParameterDefinition old = this.shortKeys.remove(shortOpt);
		if (old == null) { return null; }
		final String oldKey = old.getKey();
		this.parameterDefinitions.remove(oldKey);
		this.requiredParameters.remove(oldKey);
		String longOpt = old.getLongOpt();
		if (longOpt != null) {
			this.longKeys.remove(longOpt);
		}
		return old;
	}

	/**
	 * Returns a {@link Collection} containing the parameterDefinitions defined in this scheme. The
	 * collection returned is independent and changes to it will not reflect on this ParameterScheme
	 * instance.
	 *
	 * @return a {@link Collection} containing the parameterDefinitions defined in this scheme
	 */
	public final Collection<ParameterDefinition> getParameters() {
		return new ArrayList<>(this.parameterDefinitions.values());
	}

	/**
	 * Returns a collection of the parameterDefinitions in this scheme that have required flag set
	 * (as determined by {@link ParameterDefinition#isRequired()}). The collection returned is a
	 * copy, and changes to it do not affect this scheme's contents.
	 *
	 * @return the parameterDefinitions in this scheme that have required flag set
	 */
	public final Collection<ParameterDefinition> getRequiredParameters() {
		return new ArrayList<>(this.requiredParameters.values());
	}

	/**
	 * Returns the number of parameterDefinitions in this scheme that have required flag set (as
	 * determined by {@link ParameterDefinition#isRequired()}).
	 *
	 * @return the number of parameterDefinitions in this scheme that have required flag set
	 */
	public final int getRequiredParameterCount() {
		return this.requiredParameters.size();
	}

	/**
	 * Returns the number of parameterDefinitions defined in this scheme.
	 *
	 * @return the number of parameterDefinitions defined in this scheme.
	 */
	public final int getParameterCount() {
		return this.parameterDefinitions.size();
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
	 * Returns {@code true} if and only if this parameter scheme contains a parameter that
	 * equivalent (as per
	 * {@link ParameterDefinition#isEquivalent(ParameterDefinition, ParameterDefinition)}) to the
	 * given parameter, {@code false} otherwise.
	 *
	 * @param parameterDefinition
	 * @return {@code true} if and only if this parameter scheme contains a parameter that exactly
	 *         matches the given parameter in both long and short options, {@code false} otherwise.
	 *
	 */
	public final boolean hasEquivalentParameter(ParameterDefinition parameterDefinition) {
		Collection<ParameterDefinition> collisions = findCollisions(parameterDefinition);
		if ((collisions == null) || (collisions.size() != 1)) { return false; }
		ParameterDefinition current = collisions.iterator().next();
		return ParameterDefinition.isEquivalent(parameterDefinition, current);
	}

	/**
	 * Returns the number of parameterDefinitions already in this scheme that would collide with the
	 * given parameter based on short or long options. This means that only 3 values can be
	 * returned: 0, 1, or 2.
	 *
	 * @param parameterDefinition
	 * @return the number of parameterDefinitions already in this scheme that would collide with the
	 *         given parameter based on short or long options
	 */
	public final int countCollisions(ParameterDefinition parameterDefinition) {
		Collection<ParameterDefinition> collisions = findCollisions(parameterDefinition);
		if ((collisions == null) || collisions.isEmpty()) { return 0; }
		return collisions.size();
	}

	/**
	 * Returns the parameterDefinitions already in this scheme that would collide with the given
	 * parameter based on short or long options. If no collisions are found, {@code null} is
	 * returned. This means that the collection may contain either 1 or 2 elements.
	 *
	 * @param parameterDefinition
	 *            the parameter to check for
	 * @return the parameterDefinitions already in this scheme that would collide with the given
	 *         parameter based on short or long options, or {@code null} if none collide
	 */
	public final Collection<ParameterDefinition> findCollisions(ParameterDefinition parameterDefinition) {
		if (parameterDefinition == null) { throw new IllegalArgumentException("Must provide a non-null parameter"); }
		String longOpt = parameterDefinition.getLongOpt();
		ParameterDefinition longParam = null;
		if (longOpt != null) {
			longParam = this.longKeys.get(longOpt);
		}
		Character shortOpt = parameterDefinition.getShortOpt();
		ParameterDefinition shortParam = null;
		if (shortOpt != null) {
			shortParam = this.shortKeys.get(shortOpt);
		}
		return ParameterScheme.buildCollection(longParam, shortParam);
	}
}