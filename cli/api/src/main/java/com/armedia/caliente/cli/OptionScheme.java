package com.armedia.caliente.cli;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import com.armedia.caliente.cli.exception.DuplicateOptionException;

public class OptionScheme implements Iterable<Option> {

	private final boolean caseSensitive;
	private final String name;
	private String description = null;
	private final Map<String, Option> required = new TreeMap<>();
	private final Map<String, Option> options = new TreeMap<>();
	private final Map<String, Option> longKeys = new HashMap<>();
	private final Map<Character, Option> shortKeys = new HashMap<>();
	private int minArgs = 0;
	private int maxArgs = -1;
	private boolean dynamic = false;

	/**
	 * @param name
	 */
	public OptionScheme(String name) {
		this(name, false);
	}

	public OptionScheme(String name, boolean caseSensitive) {
		if (name == null) { throw new IllegalArgumentException("Must provide a non-null name"); }
		this.name = name;
		this.caseSensitive = caseSensitive;
	}

	public OptionScheme(OptionScheme pattern) {
		this.name = pattern.getName();
		this.required.putAll(pattern.required);
		this.options.putAll(pattern.options);
		this.longKeys.putAll(pattern.longKeys);
		this.shortKeys.putAll(pattern.shortKeys);
		this.minArgs = pattern.minArgs;
		this.maxArgs = pattern.maxArgs;
		this.dynamic = pattern.dynamic;
		this.caseSensitive = pattern.caseSensitive;
	}

	public final String getDescription() {
		return this.description;
	}

	public final void setDescription(String description) {
		this.description = description;
	}

	public final boolean isCaseSensitive() {
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

	public final boolean isDynamic() {
		return this.dynamic;
	}

	public final OptionScheme setDynamic(boolean dynamic) {
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
	 * Returns {@code true} if this option scheme supports positional arguments, {@code false}
	 * otherwise. an optionScheme supports positional arguments if {@code maxArgs != 0}.
	 *
	 * @return {@code true} if this option scheme supports positional arguments, {@code false}
	 *         otherwise
	 */
	public boolean isSupportsPositionals() {
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
	public OptionScheme setMinArgs(int minArgs) {
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
	public OptionScheme setMaxArgs(int maxArgs) {
		this.maxArgs = Math.max(-1, maxArgs);
		if ((this.minArgs > 0) && (this.maxArgs >= 0) && (this.minArgs > this.maxArgs)) {
			this.minArgs = this.maxArgs;
		}
		return this;
	}

	private static Collection<Option> buildCollection(Option a, Option b) {
		if ((a == null) && (b == null)) { return null; }
		if ((a == null) || (b == null)) { return Collections.unmodifiableCollection(Arrays.asList(a != null ? a : b)); }
		if (a == b) { return Collections.unmodifiableCollection(Collections.singleton(a)); }
		return Collections.unmodifiableCollection(Arrays.asList(a, b));
	}

	/**
	 * Find the option in this scheme which matches the given long option
	 *
	 * @param longOpt
	 *            the long option
	 * @return the option in this scheme which matches the given long option
	 */
	public final Option getOption(String longOpt) {
		if (longOpt == null) { return null; }
		return this.longKeys.get(canonicalize(longOpt));
	}

	/**
	 * Find the option in this scheme which matches the given short option
	 *
	 * @param shortOpt
	 *            the short option
	 * @return the option in this scheme which matches the given short option
	 */
	public final Option getOption(Character shortOpt) {
		if (shortOpt == null) { return null; }
		return this.shortKeys.get(canonicalize(shortOpt));
	}

	private void assertValid(Option def) {
		if (def == null) { throw new IllegalArgumentException("Option definition may not be null"); }

		final boolean hasShortOpt = (def.getShortOpt() != null);
		final boolean hasLongOpt = (def.getShortOpt() != null);

		if (!hasShortOpt && !hasLongOpt) { throw new IllegalArgumentException(
			"The given option definition has neither a short or a long option"); }
	}

	/**
	 * Adds the given option to this option scheme.
	 *
	 * @param option
	 *            the option to add
	 * @throws IllegalArgumentException
	 *             if the given option collides with any already-existing options (you can check
	 *             with {@link #hasOption(Character)}, {@link #hasOption(String)}, or
	 *             {@link #countCollisions(Option)}
	 */
	public final OptionScheme add(Option option) throws DuplicateOptionException {
		if (option == null) { throw new IllegalArgumentException("Must provide an option to add"); }
		assertValid(option);

		final String longOpt = canonicalize(option.getLongOpt());
		final Character shortOpt = canonicalize(option.getShortOpt());

		// TODO: Validate that the short and long options are valid

		final Option oldLong = getOption(longOpt);
		final Option oldShort = getOption(shortOpt);

		if ((oldLong != null) || (oldShort != null)) {
			Option existing = null;
			if ((oldLong != null) && (oldShort != null)) {
				existing = OptionScheme.buildCollection(oldShort, oldLong).iterator().next();
			} else if (oldLong != null) {
				existing = oldLong;
			} else {
				existing = oldShort;
			}
			throw new DuplicateOptionException(
				String.format("The given option %s would collide with %s", option, existing), existing, option);
		}

		if (longOpt != null) {
			remove(longOpt);
			this.longKeys.put(longOpt, option);
		}
		if (shortOpt != null) {
			remove(shortOpt);
			this.shortKeys.put(shortOpt, option);
		}
		final String key = canonicalize(option.getKey());
		this.options.put(key, option);
		if (option.isRequired()) {
			this.required.put(key, option);
		}
		return this;
	}

	/**
	 * Adds the given option to this option scheme.
	 *
	 * @param option
	 *            the option to add
	 * @throws IllegalArgumentException
	 *             if the given option collides with any already-existing options (you can check
	 *             with {@link #hasOption(Character)}, {@link #hasOption(String)}, or
	 *             {@link #countCollisions(Option)}
	 */
	public final OptionScheme addOrReplace(Option option) {
		if (option == null) { throw new IllegalArgumentException("Must provide an option to add"); }
		assertValid(option);
		remove(option);
		try {
			add(option);
		} catch (DuplicateOptionException e) {
			// This should not be possible
			throw new RuntimeException("Unexpected DuplicateOptionException during addOrReplace()", e);
		}
		return this;
	}

	/**
	 * Remove any and all options (a maximum of 2) that may collide with the given option's short or
	 * long option forms. If {@code null} is returned, then there was no collision.
	 *
	 * @param option
	 *            the option to check against
	 * @return the options that were removed
	 */
	public final Collection<Option> remove(Option option) {
		if (option == null) { throw new IllegalArgumentException("Must provide an option to remove"); }

		final String longOpt = canonicalize(option.getLongOpt());
		final Option oldLong = remove(longOpt);
		final Character shortOpt = canonicalize(option.getShortOpt());
		final Option oldShort = remove(shortOpt);

		return OptionScheme.buildCollection(oldShort, oldLong);
	}

	/**
	 * Remove the option which matches the given long option
	 *
	 * @param longOpt
	 *            the long option
	 * @return the option which matches the given long option, or {@code null} if none matches.
	 */
	public final Option remove(String longOpt) {
		if (longOpt == null) { return null; }
		Option old = this.longKeys.remove(canonicalize(longOpt));
		if (old == null) { return null; }
		final String oldKey = canonicalize(old.getKey());
		this.options.remove(oldKey);
		this.required.remove(oldKey);
		Character shortOpt = canonicalize(old.getShortOpt());
		if (shortOpt != null) {
			this.shortKeys.remove(shortOpt);
		}
		return old;
	}

	/**
	 * Remove the option which matches the given short option
	 *
	 * @param shortOpt
	 *            the short option
	 * @return the option which matches the given short option, or {@code null} if none matches.
	 */
	public final Option remove(Character shortOpt) {
		if (shortOpt == null) { return null; }
		Option old = this.shortKeys.remove(canonicalize(shortOpt));
		if (old == null) { return null; }
		final String oldKey = canonicalize(old.getKey());
		this.options.remove(oldKey);
		this.required.remove(oldKey);
		String longOpt = canonicalize(old.getLongOpt());
		if (longOpt != null) {
			this.longKeys.remove(longOpt);
		}
		return old;
	}

	/**
	 * Returns a {@link Collection} containing the options defined in this scheme. The collection
	 * returned is independent and changes to it will not reflect on this OptionScheme instance.
	 *
	 * @return a {@link Collection} containing the options defined in this scheme
	 */
	public final Collection<Option> getOptions() {
		return new ArrayList<>(this.options.values());
	}

	/**
	 * Returns a collection of the options in this scheme that have required flag set (as determined
	 * by {@link Option#isRequired()}). The collection returned is a copy, and changes to it do not
	 * affect this scheme's contents.
	 *
	 * @return the options in this scheme that have required flag set
	 */
	public final Collection<Option> getRequiredOptions() {
		return new ArrayList<>(this.required.values());
	}

	/**
	 * Returns the number of options in this scheme that have required flag set (as determined by
	 * {@link Option#isRequired()}).
	 *
	 * @return the number of options in this scheme that have required flag set
	 */
	public final int getRequiredOptionCount() {
		return this.required.size();
	}

	/**
	 * Returns the number of options defined in this scheme.
	 *
	 * @return the number of options defined in this scheme.
	 */
	public final int getOptionCount() {
		return this.options.size();
	}

	/**
	 * Returns {@code true} if this scheme contains an option that uses the given short option,
	 * {@code false} otherwise.
	 *
	 * @param shortOpt
	 * @return {@code true} if this scheme contains an option that uses the given short option,
	 *         {@code false} otherwise.
	 */
	public final boolean hasOption(Character shortOpt) {
		return (getOption(shortOpt) != null);
	}

	/**
	 * Returns {@code true} if this scheme contains an option that uses the given long option,
	 * {@code false} otherwise.
	 *
	 * @param longOpt
	 * @return {@code true} if this scheme contains an option that uses the given long option,
	 *         {@code false} otherwise.
	 */
	public final boolean hasOption(String longOpt) {
		return (getOption(longOpt) != null);
	}

	/**
	 * Returns an integer between 0 and 3 where the low bit is the presence indicator for the short
	 * option, and the high bit is the presence indicator for the long option. This method does not
	 * take into account whether both flags are associated to the same option. Use
	 * {@link #findCollisions(Character, String)} for that.
	 *
	 * @param shortOpt
	 * @param longOpt
	 * @return an integer between 0 and 3 where the low bit is the presence indicator for the short
	 *         option, and the high bit is the presence indicator for the long option.
	 */
	public final int hasEitherOption(Character shortOpt, String longOpt) {
		int ret = 0;
		if (hasOption(shortOpt)) {
			ret |= 1;
		}
		if (hasOption(longOpt)) {
			ret |= (1 << 1);
		}
		return ret;
	}

	/**
	 * Returns {@code true} if and only if this option scheme contains an option that equivalent (as
	 * per {@link Option#isEquivalent(Option, Option)}) to the given option, {@code false}
	 * otherwise.
	 *
	 * @param option
	 * @return {@code true} if and only if this option scheme contains an option that exactly
	 *         matches the given option in both long and short options, {@code false} otherwise.
	 *
	 */
	public final boolean hasEquivalentOption(Option option) {
		Collection<Option> collisions = findCollisions(option);
		if ((collisions == null) || (collisions.size() != 1)) { return false; }
		Option current = collisions.iterator().next();
		return Option.isEquivalent(option, current);
	}

	/**
	 * Returns the number of options already in this scheme that would collide with the given option
	 * based on short or long options. This means that only 3 values can be returned: 0, 1, or 2.
	 *
	 * @param option
	 * @return the number of options already in this scheme that would collide with the given option
	 *         based on short or long options
	 */
	public final int countCollisions(Option option) {
		Collection<Option> collisions = findCollisions(option);
		if ((collisions == null) || collisions.isEmpty()) { return 0; }
		return collisions.size();
	}

	/**
	 * Returns the options already in this scheme that would collide with the given option based on
	 * short or long options. If no collisions are found, {@code null} is returned. This means that
	 * the collection may contain either 1 or 2 elements.
	 *
	 * @param option
	 *            the option to check for
	 * @return the options already in this scheme that would collide with the given option based on
	 *         short or long options, or {@code null} if none collide
	 */
	public final Collection<Option> findCollisions(Option option) {
		if (option == null) { throw new IllegalArgumentException("Must provide a non-null option"); }
		return findCollisions(option.getShortOpt(), option.getLongOpt());
	}

	/**
	 * Returns the Option already in this scheme that would collide with the given short or long
	 * options. If no collisions are found, {@code null} is returned. This means that the collection
	 * may contain either 1 or 2 elements.
	 *
	 * @param shortOpt
	 *            the short option to check for
	 * @param longOpt
	 *            the long option to check for
	 * @return the Option already in this scheme that would collide with the given short or long
	 *         options, or {@code null} if none collide
	 */
	public final Collection<Option> findCollisions(Character shortOpt, String longOpt) {
		Option longParam = null;
		if (longOpt != null) {
			longParam = this.longKeys.get(canonicalize(longOpt));
		}
		Option shortParam = null;
		if (shortOpt != null) {
			shortParam = this.shortKeys.get(canonicalize(shortOpt));
		}
		return OptionScheme.buildCollection(longParam, shortParam);
	}

	@Override
	public Iterator<Option> iterator() {
		return getOptions().iterator();
	}
}