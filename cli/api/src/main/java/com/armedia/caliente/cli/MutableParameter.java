package com.armedia.caliente.cli;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public final class MutableParameter extends Parameter implements Cloneable {

	private boolean required = false;
	private String description = null;
	private Character shortOpt = null;
	private String longOpt = null;
	private int minValueCount = 0;
	private int maxValueCount = 0;
	private String valueName = null;
	private Character valueSep = MutableParameter.DEFAULT_VALUE_SEP;
	private final Set<String> allowedValues = new TreeSet<>();
	private final List<String> defaults = new ArrayList<>();

	private String key = null;

	public MutableParameter() {
		this(null);
	}

	public MutableParameter(Parameter other) {
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
	public MutableParameter clone() {
		return new MutableParameter(this);
	}

	public ImmutableParameter freezeCopy() {
		return new ImmutableParameter(this);
	}

	@Override
	public String getKey() {
		return this.key;
	}

	@Override
	public int getMinValueCount() {
		return this.minValueCount;
	}

	public MutableParameter setMinValueCount(int count) {
		this.minValueCount = count;
		return this;
	}

	@Override
	public int getMaxValueCount() {
		return this.maxValueCount;
	}

	public MutableParameter setMaxValueCount(int count) {
		this.maxValueCount = count;
		return this;
	}

	@Override
	public String getValueName() {
		return this.valueName;
	}

	public MutableParameter setValueName(String argName) {
		this.valueName = argName;
		return this;
	}

	@Override
	public boolean isRequired() {
		return this.required;
	}

	public MutableParameter setRequired(boolean required) {
		this.required = required;
		return this;
	}

	@Override
	public String getDescription() {
		return this.description;
	}

	public MutableParameter setDescription(String description) {
		this.description = description;
		return this;
	}

	@Override
	public String getLongOpt() {
		return this.longOpt;
	}

	public MutableParameter setLongOpt(String longOpt) {
		this.longOpt = longOpt;
		this.key = ParameterTools.calculateKey(this);
		return this;
	}

	@Override
	public Character getShortOpt() {
		return this.shortOpt;
	}

	public MutableParameter setShortOpt(Character shortOpt) {
		this.shortOpt = shortOpt;
		this.key = ParameterTools.calculateKey(this);
		return this;
	}

	@Override
	public Character getValueSep() {
		return this.valueSep;
	}

	public MutableParameter setValueSep(Character valueSep) {
		this.valueSep = valueSep;
		return this;
	}

	@Override
	public Set<String> getAllowedValues() {
		return this.allowedValues;
	}

	public MutableParameter setAllowedValues(Collection<String> allowedValues) {
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

	public MutableParameter setAllowedValues(String... allowedValues) {
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

	public MutableParameter setDefault(String value) {
		return setDefaults(value);
	}

	@Override
	public List<String> getDefaults() {
		return this.defaults.isEmpty() ? null : this.defaults;
	}

	public MutableParameter setDefaults(Collection<String> defaults) {
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

	public MutableParameter setDefaults(String... defaults) {
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
			"MutableParameter [key=%s, required=%s, shortOpt=%s, longOpt=%s, description=%s, minValueCount=%d, maxValueCount=%d, valueName=%s, minValueCount=%s, valueSep=%s, defaults=%s]",
			this.key, this.required, this.shortOpt, this.longOpt, this.description, this.minValueCount,
			this.maxValueCount, this.valueName, this.minValueCount, this.valueSep, this.defaults);
	}

	public static <E extends Enum<E>> MutableParameter initOptionName(E e, MutableParameter p) {
		if (p == null) { throw new IllegalArgumentException(
			"Must provide a MutableParameter whose option to initialize"); }
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