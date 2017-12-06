package com.armedia.caliente.cli;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.cli.exception.DuplicateOptionException;
import com.armedia.caliente.cli.exception.DuplicateOptionGroupException;

public class OptionScheme implements Iterable<Option>, OptionGroup, OptionSchemeExtender, PositionalValueSupport {

	private class OptionSchemeGroup extends OptionGroupImpl {

		private OptionSchemeGroup() {
			super();
		}

		private OptionSchemeGroup(String name) {
			super(name);
		}

		@Override
		public OptionSchemeGroup add(Option option) throws DuplicateOptionException {
			OptionScheme.this.aggregate.add(option);
			super.add(option);
			return this;
		}

		@Override
		public Option remove(String longOpt) {
			OptionScheme.this.aggregate.remove(longOpt);
			return super.remove(longOpt);
		}

		@Override
		public Option remove(Character shortOpt) {
			OptionScheme.this.aggregate.remove(shortOpt);
			return super.remove(shortOpt);
		}

		@Override
		public OptionScheme getScheme() {
			return OptionScheme.this;
		}
	}

	private final Map<String, OptionGroup> groups = new LinkedHashMap<>();
	private final Map<Character, OptionGroup> shortGroups = new HashMap<>();
	private final Map<String, OptionGroup> longGroups = new HashMap<>();
	private final OptionGroup aggregate = new OptionGroupImpl();
	private final OptionGroup baseGroup;

	private String argumentName;
	private int minArgs = 0;
	private int maxArgs = -1;

	public OptionScheme(String name) {
		this.baseGroup = new OptionSchemeGroup(name);
	}

	@Override
	public final String getDescription() {
		return this.baseGroup.getDescription();
	}

	@Override
	public final OptionScheme setDescription(String description) {
		this.baseGroup.setDescription(description);
		return this;
	}

	/**
	 * Returns this scheme's given name.
	 *
	 * @return this scheme's given name
	 */
	@Override
	public String getName() {
		return this.baseGroup.getName();
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
	 * Returns {@code true} if this option scheme supports positional arguments, {@code false}
	 * otherwise. an optionScheme supports positional arguments if {@code maxArgs != 0}.
	 *
	 * @return {@code true} if this option scheme supports positional arguments, {@code false}
	 *         otherwise
	 */
	public boolean isRequiresPositionals() {
		return (this.minArgs > 0);
	}

	/**
	 * Returns the minimum number of allowed positional arguments. The number will always be a
	 * positive integer, where 0 means there is no minimum set.
	 *
	 * @return the minimum number of allowed positional arguments
	 */
	@Override
	public final int getMinArguments() {
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
	public final OptionScheme setMinArguments(int minArgs) {
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
	@Override
	public final int getMaxArguments() {
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
	public final OptionScheme setMaxArguments(int maxArgs) {
		this.maxArgs = Math.max(Option.UNBOUNDED_MAX_VALUES, maxArgs);
		if ((this.minArgs > 0) && (this.maxArgs >= 0) && (this.minArgs > this.maxArgs)) {
			this.minArgs = this.maxArgs;
		}
		return this;
	}

	public OptionGroup getGroup(String name) {
		if (name == null) { return getBaseGroup(); }
		return this.groups.get(name);
	}

	public OptionGroup getBaseGroup() {
		return this.baseGroup;
	}

	@Override
	public Iterator<Option> iterator() {
		return new Iterator<Option>() {

			private final Iterator<Option> it = OptionScheme.this.aggregate.iterator();

			@Override
			public boolean hasNext() {
				return this.it.hasNext();
			}

			@Override
			public Option next() {
				return this.it.next();
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	@Override
	public String getArgumentName() {
		return this.argumentName;
	}

	public OptionScheme setArgumentName(String argumentName) {
		this.argumentName = argumentName;
		return this;
	}

	protected static final String canonicalizeGroupName(String name) {
		if (StringUtils
			.isBlank(name)) { throw new IllegalArgumentException("Must provide a non-blank, non-null name"); }
		return name.toLowerCase().trim();
	}

	@Override
	public OptionScheme add(OptionGroup group) {
		if (group == null) { return this; }
		String key = OptionScheme.canonicalizeGroupName(group.getName());
		if (this.groups.containsKey(key)) { throw new DuplicateOptionGroupException(group.getName()); }

		List<Option> added = new LinkedList<>();
		try {
			for (Option o : group) {
				this.aggregate.add(o);
				added.add(o);
			}
		} catch (final DuplicateOptionException e) {
			throw e;
		} finally {
			// Roll back the changes, as they'll be re-done below...
			for (Option o : added) {
				this.aggregate.remove(o);
			}
		}

		// At this point, all the incoming options are OK, and the group name is also unique, so we
		// go ahead and create a copy of the group and add it
		OptionSchemeGroup newGroup = new OptionSchemeGroup(group.getName());
		for (Option o : group) {
			// Also make a copy of the options, so they can't be modified
			o = o.clone();
			newGroup.add(o);
			Character shortOpt = o.getShortOpt();
			if (shortOpt != null) {
				this.shortGroups.put(OptionGroupImpl.canonicalizeOption(shortOpt), newGroup);
			}
			String longOpt = o.getLongOpt();
			if (longOpt != null) {
				this.longGroups.put(OptionGroupImpl.canonicalizeOption(longOpt), newGroup);
			}
		}
		this.groups.put(key, group);
		return this;
	}

	public OptionGroup getGroupFor(Character option) {
		if (option == null) { return null; }
		return this.shortGroups.get(option);
	}

	public OptionGroup getGroupFor(String option) {
		if (option == null) { return null; }
		return this.longGroups.get(option);
	}

	public Set<String> getGroupNames() {
		return new LinkedHashSet<>(this.groups.keySet());
	}

	@Override
	public boolean hasOption(Character option) {
		return this.aggregate.hasOption(option);
	}

	@Override
	public boolean hasOption(String option) {
		return this.aggregate.hasOption(option);
	}

	@Override
	public int hasOption(Option option) {
		return this.aggregate.hasOption(option);
	}

	@Override
	public boolean hasGroup(String name) {
		return ((name != null) && this.groups.containsKey(OptionScheme.canonicalizeGroupName(name)));
	}

	@Override
	public Option getOption(String longOpt) {
		return this.aggregate.getOption(longOpt);
	}

	@Override
	public Option getOption(Character shortOpt) {
		return this.aggregate.getOption(shortOpt);
	}

	@Override
	public final Collection<Option> getOptions() {
		return this.aggregate.getOptions();
	}

	@Override
	public final Collection<Option> getRequiredOptions() {
		return this.aggregate.getRequiredOptions();
	}

	@Override
	public final int getRequiredOptionCount() {
		return this.aggregate.getRequiredOptionCount();
	}

	@Override
	public final int getOptionCount() {
		return this.aggregate.getOptionCount();
	}

	@Override
	public final int hasOption(Character shortOpt, String longOpt) {
		return this.aggregate.hasOption(shortOpt, longOpt);
	}

	@Override
	public final boolean hasEquivalentOption(Option option) {
		return this.aggregate.hasEquivalentOption(option);
	}

	@Override
	public final int countCollisions(Option option) {
		return this.aggregate.countCollisions(option);
	}

	@Override
	public final Collection<Option> findCollisions(Option option) {
		return this.aggregate.findCollisions(option);
	}

	@Override
	public final Collection<Option> findCollisions(Character shortOpt, String longOpt) {
		return this.aggregate.findCollisions(shortOpt, longOpt);
	}

	@Override
	public OptionScheme add(Option option) throws DuplicateOptionException {
		this.baseGroup.add(option);
		return this;
	}

	@Override
	public <O extends Option> OptionScheme add(Collection<O> options) throws DuplicateOptionException {
		this.baseGroup.add(options);
		return this;
	}

	@Override
	public Collection<Option> remove(Option option) {
		return this.baseGroup.remove(option);
	}

	@Override
	public Option remove(String longOpt) {
		return this.baseGroup.remove(longOpt);
	}

	@Override
	public Option remove(Character shortOpt) {
		return this.baseGroup.remove(shortOpt);
	}

	@Override
	public OptionScheme getScheme() {
		return this.baseGroup.getScheme();
	}
}