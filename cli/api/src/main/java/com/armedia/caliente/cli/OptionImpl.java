package com.armedia.caliente.cli;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public final class OptionImpl extends Option implements Cloneable {

	private boolean required = false;
	private String description = null;
	private Character shortOpt = null;
	private String longOpt = null;
	private int minValueCount = 0;
	private int maxValueCount = 0;
	private String valueName = null;
	private Character valueSep = OptionImpl.DEFAULT_VALUE_SEP;
	private final Set<String> allowedValues = new TreeSet<>();
	private final List<String> defaults = new ArrayList<>();

	private String key = null;

	public OptionImpl() {
		this(null);
	}

	public OptionImpl(Option other) {
		if (other != null) {
			this.required = other.isRequired();
			this.description = other.getDescription();
			this.shortOpt = other.getShortOpt();
			this.longOpt = other.getLongOpt();
			this.minValueCount = other.getMinValueCount();
			this.maxValueCount = other.getMaxValueCount();
			this.valueName = other.getValueName();
			this.valueSep = other.getValueSep();
			Set<String> allowedValues = other.getAllowedValues();
			if (allowedValues != null) {
				this.allowedValues.addAll(allowedValues);
			}
			this.key = other.getKey();
		}
	}

	@Override
	public OptionImpl clone() {
		return new OptionImpl(this);
	}

	@Override
	public String getKey() {
		return this.key;
	}

	@Override
	public int getMinValueCount() {
		return this.minValueCount;
	}

	public OptionImpl setMinValueCount(int count) {
		this.minValueCount = Math.max(0, count);
		if ((this.minValueCount > 0) && (this.maxValueCount >= 0) && (this.minValueCount > this.maxValueCount)) {
			this.maxValueCount = this.minValueCount;
		}
		return this;
	}

	@Override
	public int getMaxValueCount() {
		return this.maxValueCount;
	}

	public OptionImpl setMaxValueCount(int count) {
		this.maxValueCount = Math.max(Option.UNBOUNDED_MAX_VALUES, count);
		if ((this.minValueCount > 0) && (this.maxValueCount >= 0) && (this.minValueCount > this.maxValueCount)) {
			this.minValueCount = this.maxValueCount;
		}
		return this;
	}

	@Override
	public String getValueName() {
		return this.valueName;
	}

	public OptionImpl setValueName(String argName) {
		this.valueName = argName;
		return this;
	}

	@Override
	public boolean isRequired() {
		return this.required;
	}

	public OptionImpl setRequired(boolean required) {
		this.required = required;
		return this;
	}

	@Override
	public String getDescription() {
		return this.description;
	}

	public OptionImpl setDescription(String description) {
		this.description = description;
		return this;
	}

	@Override
	public String getLongOpt() {
		return this.longOpt;
	}

	public OptionImpl setLongOpt(String longOpt) {
		if (longOpt != null) {
			boolean valid = Option.VALID_LONG.matcher(longOpt).matches();
			if (valid) {
				valid &= (longOpt.length() > 1);
			}
			if (!valid) { throw new IllegalArgumentException(
				String.format("The long option value [%s] is not valid", longOpt)); }
		}
		this.longOpt = longOpt;
		this.key = Option.calculateKey(this);
		return this;
	}

	@Override
	public Character getShortOpt() {
		return this.shortOpt;
	}

	public OptionImpl setShortOpt(Character shortOpt) {
		if (shortOpt != null) {
			boolean valid = Option.VALID_SHORT.matcher(shortOpt.toString()).matches();
			if (!valid) { throw new IllegalArgumentException(
				String.format("The short option value [%s] is not valid", shortOpt)); }
		}
		this.shortOpt = shortOpt;
		this.key = Option.calculateKey(this);
		return this;
	}

	@Override
	public Character getValueSep() {
		return this.valueSep;
	}

	public OptionImpl setValueSep(Character valueSep) {
		Objects.requireNonNull(valueSep, "Must provide a non-null value separator");
		this.valueSep = valueSep;
		return this;
	}

	@Override
	public Set<String> getAllowedValues() {
		return this.allowedValues;
	}

	public OptionImpl setAllowedValues(Collection<String> allowedValues) {
		this.allowedValues.clear();
		if (allowedValues != null) {
			for (String s : allowedValues) {
				if (s != null) {
					this.allowedValues.add(s);
				}
			}
		}
		return this;
	}

	public OptionImpl setAllowedValues(String... allowedValues) {
		if (allowedValues != null) {
			setAllowedValues(Arrays.asList(allowedValues));
		} else {
			this.allowedValues.clear();
		}
		return this;
	}

	@Override
	public String getDefault() {
		return this.defaults.isEmpty() ? null : this.defaults.get(0);
	}

	public OptionImpl setDefault(String value) {
		return setDefaults(value);
	}

	@Override
	public List<String> getDefaults() {
		return this.defaults.isEmpty() ? null : this.defaults;
	}

	public OptionImpl setDefaults(Collection<String> defaults) {
		this.defaults.clear();
		if (defaults != null) {
			for (String s : defaults) {
				if (s != null) {
					this.defaults.add(s);
				}
			}
		}
		return this;
	}

	public OptionImpl setDefaults(String... defaults) {
		if (defaults != null) {
			setDefaults(Arrays.asList(defaults));
		} else {
			this.defaults.clear();
		}
		return this;
	}

	@Override
	public String toString() {
		return String.format(
			"OptionImpl [key=%s, required=%s, shortOpt=%s, longOpt=%s, description=%s, minValueCount=%d, maxValueCount=%d, valueName=%s, minValueCount=%s, valueSep=%s, defaults=%s]",
			this.key, this.required, this.shortOpt, this.longOpt, this.description, this.minValueCount,
			this.maxValueCount, this.valueName, this.minValueCount, this.valueSep, this.defaults);
	}

	public static <E extends Enum<E>> OptionImpl initOptionName(E e, OptionImpl p) {
		Objects.requireNonNull(p, "Must provide a OptionImpl whose option to initialize");
		if (e == null) { return p; }
		final String name = e.name();
		if (name.length() == 1) {
			// If we decide that the name of the option will be a single character, we use that
			p.setShortOpt(name.charAt(0));
		} else if (p.getLongOpt() == null) {
			// Otherwise, use the name replacing underscores with dashes
			p.setLongOpt(name.replace('_', '-'));
		}
		return p;
	}
}