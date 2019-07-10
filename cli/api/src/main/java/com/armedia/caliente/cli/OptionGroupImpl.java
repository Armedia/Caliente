/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
package com.armedia.caliente.cli;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.armedia.caliente.cli.exception.DuplicateOptionException;
import com.armedia.commons.utilities.StreamTools;
import com.armedia.commons.utilities.Tools;

public class OptionGroupImpl implements OptionGroup {

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
		this.name = Objects.requireNonNull(name, "Must provide a non-null name").trim();
	}

	private OptionGroupImpl(String altName, OptionGroupImpl other) {
		this.name = Tools.coalesce(altName, other.getName());
		this.required.putAll(other.required);
		this.options.putAll(other.options);
		this.longKeys.putAll(other.longKeys);
		this.shortKeys.putAll(other.shortKeys);
	}

	public OptionGroupImpl getCopy(String name) {
		return new OptionGroupImpl(name, this);
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
	public final OptionGroupImpl setDescription(String description) {
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

		if (!hasShortOpt && !hasLongOpt) {
			throw new IllegalArgumentException("The given option definition has neither a short or a long option");
		}
	}

	@Override
	public OptionGroupImpl add(Option option) throws DuplicateOptionException {
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
			this.longKeys.put(longOpt, option);
		}
		if (shortOpt != null) {
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
	public OptionGroup add(Supplier<Option> option) throws DuplicateOptionException {
		return add(Option.unwrap(option));
	}

	@Override
	public <O extends Option> OptionGroupImpl addFrom(Iterable<O> options) throws DuplicateOptionException {
		if (options != null) {
			Collection<O> added = new LinkedList<>();
			boolean ok = false;
			try {
				Consumer<O> thisAdd = this::add;
				StreamTools.of(options.iterator()).filter(Objects::nonNull).forEach(thisAdd.andThen(added::add));
				ok = true;
			} finally {
				if (!ok) {
					// Roll back the changes...
					added.stream().forEach(this::remove);
				}
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
	public Collection<Option> remove(Supplier<Option> option) {
		return remove(Option.unwrap(option));
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
	public <O extends Option> Collection<Option> findCollisions(Iterable<O> options) {
		if (options == null) { return null; }
		Map<String, Option> ret = new TreeMap<>();
		for (O o : options) {
			Collection<Option> C = findCollisions(o);
			if (C != null) {
				for (Option c : C) {
					ret.put(c.getKey(), c);
				}
			}
		}
		return ret.values();
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