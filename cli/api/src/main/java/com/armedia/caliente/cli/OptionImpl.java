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
	private int minArguments = 0;
	private int maxArguments = 0;
	private String argumentName = null;
	private Character valueSep = OptionImpl.DEFAULT_VALUE_SEP;
	private boolean valuesCaseSensitive = false;
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
			this.minArguments = other.getMinArguments();
			this.maxArguments = other.getMaxArguments();
			this.argumentName = other.getArgumentName();
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
	public int getMinArguments() {
		return this.minArguments;
	}

	public OptionImpl setMinArguments(int count) {
		this.minArguments = Math.max(0, count);
		if ((this.minArguments > 0) && (this.maxArguments >= 0) && (this.minArguments > this.maxArguments)) {
			this.maxArguments = this.minArguments;
		}
		return this;
	}

	@Override
	public int getMaxArguments() {
		return this.maxArguments;
	}

	public OptionImpl setMaxArguments(int count) {
		this.maxArguments = Math.max(Option.UNBOUNDED_MAX_VALUES, count);
		if ((this.minArguments > 0) && (this.maxArguments >= 0) && (this.minArguments > this.maxArguments)) {
			this.minArguments = this.maxArguments;
		}
		return this;
	}

	@Override
	public String getArgumentName() {
		return this.argumentName;
	}

	public OptionImpl setArgumentName(String argName) {
		this.argumentName = argName;
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
	public boolean isValuesCaseSensitive() {
		return this.valuesCaseSensitive;
	}

	public OptionImpl setValuesCaseSensitive(boolean valuesCaseSensitive) {
		this.valuesCaseSensitive = valuesCaseSensitive;
		return this;
	}

	@Override
	public boolean isValueAllowed(String value) {
		if (value == null) { return false; }
		if (this.allowedValues.isEmpty()) { return true; }
		if (this.valuesCaseSensitive) {
			value = value.toUpperCase();
		}
		return this.allowedValues.contains(value);
	}

	@Override
	public String canonicalizeValue(String value) {
		if ((value == null) || this.valuesCaseSensitive) { return value; }
		return value.toUpperCase();
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
					s = canonicalizeValue(s);
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
			"OptionImpl [key=%s, required=%s, shortOpt=%s, longOpt=%s, description=%s, minArguments=%d, maxArguments=%d, argumentName=%s, minArguments=%s, valueSep=%s, defaults=%s]",
			this.key, this.required, this.shortOpt, this.longOpt, this.description, this.minArguments,
			this.maxArguments, this.argumentName, this.minArguments, this.valueSep, this.defaults);
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