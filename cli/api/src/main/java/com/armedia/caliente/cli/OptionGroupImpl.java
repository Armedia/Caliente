package com.armedia.caliente.cli;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import com.armedia.caliente.cli.exception.DuplicateOptionException;

public class OptionGroupImpl implements OptionGroup, Cloneable {

	private String name;
	private Map<String, Option> required = new TreeMap<>();
	private Map<String, Option> options = new TreeMap<>();
	private Map<String, Option> longKeys = new HashMap<>();
	private Map<Character, Option> shortKeys = new HashMap<>();

	private String description = null;

	OptionGroupImpl() {
		this.name = null;
	}

	public OptionGroupImpl(String name) {
		this.name = name.trim();
	}

	private OptionGroupImpl(OptionGroupImpl other) {
		this.name = other.getName();
		this.required.putAll(other.required);
		this.options.putAll(other.options);
		this.longKeys.putAll(other.longKeys);
		this.shortKeys.putAll(other.shortKeys);
	}

	@Override
	public OptionGroup clone() {
		return new OptionGroupImpl(this);
	}

	/* (non-Javadoc)
	 * @see com.armedia.caliente.cli.OptionGroup#getDescription()
	 */
	@Override
	public final String getDescription() {
		return this.description;
	}

	/* (non-Javadoc)
	 * @see com.armedia.caliente.cli.OptionGroup#setDescription(java.lang.String)
	 */
	@Override
	public final OptionGroup setDescription(String description) {
		this.description = description;
		return this;
	}

	protected static final String canonicalizeOption(String str) {
		if (str == null) { return null; }
		// long options are case-insensitive
		return str.toLowerCase();
	}

	protected static final Character canonicalizeOption(Character c) {
		// We do nothing for characters, as they're case sensitive
		return c;
	}

	@Override
	public final String getName() {
		return this.name;
	}

	private static Collection<Option> buildCollection(Option a, Option b) {
		if ((a == null) && (b == null)) { return null; }
		if ((a == null) || (b == null)) { return Collections.unmodifiableCollection(Arrays.asList(a != null ? a : b)); }
		if (a == b) { return Collections.unmodifiableCollection(Collections.singleton(a)); }
		return Collections.unmodifiableCollection(Arrays.asList(a, b));
	}

	@Override
	public final Option getOption(String longOpt) {
		if (longOpt == null) { return null; }
		return this.longKeys.get(OptionGroupImpl.canonicalizeOption(longOpt));
	}

	@Override
	public final Option getOption(Character shortOpt) {
		if (shortOpt == null) { return null; }
		return this.shortKeys.get(OptionGroupImpl.canonicalizeOption(shortOpt));
	}

	private void assertValid(Option def) {
		Objects.requireNonNull(def, "Must provide a non-null option");

		final boolean hasShortOpt = (def.getShortOpt() != null);
		final boolean hasLongOpt = (def.getLongOpt() != null);

		if (!hasShortOpt && !hasLongOpt) { throw new IllegalArgumentException(
			"The given option definition has neither a short or a long option"); }
	}

	@Override
	public OptionGroup add(Option option) throws DuplicateOptionException {
		assertValid(option);

		final String longOpt = OptionGroupImpl.canonicalizeOption(option.getLongOpt());
		final Character shortOpt = OptionGroupImpl.canonicalizeOption(option.getShortOpt());

		final Option oldLong = getOption(longOpt);
		final Option oldShort = getOption(shortOpt);

		if ((oldLong != null) || (oldShort != null)) {
			Option existing = null;
			if ((oldLong != null) && (oldShort != null)) {
				existing = OptionGroupImpl.buildCollection(oldShort, oldLong).iterator().next();
			} else if (oldLong != null) {
				existing = oldLong;
			} else {
				existing = oldShort;
			}
			if (option == existing) { return this; }
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
		final String key = OptionGroupImpl.canonicalizeOption(option.getKey());
		this.options.put(key, option);
		if (option.isRequired()) {
			this.required.put(key, option);
		}
		return this;
	}

	@Override
	public <O extends Option> OptionGroup add(Collection<O> options) throws DuplicateOptionException {
		if ((options != null) && !options.isEmpty()) {
			Collection<O> added = new ArrayList<>();
			try {
				for (O o : options) {
					if (o != null) {
						add(o);
						added.add(o);
					}
				}
			} catch (final DuplicateOptionException e) {
				// Roll back the changes...
				for (O o : added) {
					remove(o);
				}
				throw e;
			}
		}
		return this;
	}

	@Override
	public Collection<Option> remove(Option option) {
		if (option == null) { throw new IllegalArgumentException("Must provide an option to remove"); }

		final String longOpt = OptionGroupImpl.canonicalizeOption(option.getLongOpt());
		final Option oldLong = remove(longOpt);
		final Character shortOpt = OptionGroupImpl.canonicalizeOption(option.getShortOpt());
		final Option oldShort = remove(shortOpt);

		return OptionGroupImpl.buildCollection(oldShort, oldLong);
	}

	@Override
	public Option remove(String longOpt) {
		if (longOpt == null) { return null; }
		Option old = this.longKeys.remove(OptionGroupImpl.canonicalizeOption(longOpt));
		if (old == null) { return null; }
		final String oldKey = OptionGroupImpl.canonicalizeOption(old.getKey());
		this.options.remove(oldKey);
		this.required.remove(oldKey);
		Character shortOpt = OptionGroupImpl.canonicalizeOption(old.getShortOpt());
		if (shortOpt != null) {
			this.shortKeys.remove(shortOpt);
		}
		return old;
	}

	@Override
	public Option remove(Character shortOpt) {
		if (shortOpt == null) { return null; }
		Option old = this.shortKeys.remove(OptionGroupImpl.canonicalizeOption(shortOpt));
		if (old == null) { return null; }
		final String oldKey = OptionGroupImpl.canonicalizeOption(old.getKey());
		this.options.remove(oldKey);
		this.required.remove(oldKey);
		String longOpt = OptionGroupImpl.canonicalizeOption(old.getLongOpt());
		if (longOpt != null) {
			this.longKeys.remove(longOpt);
		}
		return old;
	}

	@Override
	public final Collection<Option> getOptions() {
		return new ArrayList<>(this.options.values());
	}

	@Override
	public final Collection<Option> getRequiredOptions() {
		return new ArrayList<>(this.required.values());
	}

	@Override
	public final int getRequiredOptionCount() {
		return this.required.size();
	}

	@Override
	public final int getOptionCount() {
		return this.options.size();
	}

	@Override
	public final boolean hasOption(Character shortOpt) {
		return ((shortOpt != null) && (getOption(shortOpt) != null));
	}

	@Override
	public final boolean hasOption(String longOpt) {
		return ((longOpt != null) && (getOption(longOpt) != null));
	}

	@Override
	public final int hasOption(Option option) {
		if (option == null) { return 0; }
		int ret = 0;
		if (hasOption(option.getShortOpt())) {
			ret |= 1;
		}
		if (hasOption(option.getLongOpt())) {
			ret |= (1 << 1);
		}
		return ret;
	}

	@Override
	public final int hasOption(Character shortOpt, String longOpt) {
		int ret = 0;
		if (hasOption(shortOpt)) {
			ret |= 1;
		}
		if (hasOption(longOpt)) {
			ret |= (1 << 1);
		}
		return ret;
	}

	@Override
	public final boolean hasEquivalentOption(Option option) {
		Collection<Option> collisions = findCollisions(option);
		if ((collisions == null) || (collisions.size() != 1)) { return false; }
		Option current = collisions.iterator().next();
		return Option.isEquivalent(option, current);
	}

	@Override
	public final int countCollisions(Option option) {
		Collection<Option> collisions = findCollisions(option);
		if ((collisions == null) || collisions.isEmpty()) { return 0; }
		return collisions.size();
	}

	@Override
	public final Collection<Option> findCollisions(Option option) {
		assertValid(option);
		return findCollisions(option.getShortOpt(), option.getLongOpt());
	}

	@Override
	public final Collection<Option> findCollisions(Character shortOpt, String longOpt) {
		Option longParam = null;
		if (longOpt != null) {
			longParam = this.longKeys.get(OptionGroupImpl.canonicalizeOption(longOpt));
		}
		Option shortParam = null;
		if (shortOpt != null) {
			shortParam = this.shortKeys.get(OptionGroupImpl.canonicalizeOption(shortOpt));
		}
		return OptionGroupImpl.buildCollection(longParam, shortParam);
	}

	@Override
	public final Iterator<Option> iterator() {
		return getOptions().iterator();
	}

	@Override
	public OptionScheme getScheme() {
		return null;
	}
}