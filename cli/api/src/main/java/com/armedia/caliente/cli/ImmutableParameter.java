package com.armedia.caliente.cli;

import java.util.List;
import java.util.Set;

import com.armedia.commons.utilities.Tools;

public final class ImmutableParameter extends Parameter {

	private final boolean required;
	private final String description;
	private final Character shortOpt;
	private final String longOpt;
	private final int minValueCount;
	private final int maxValueCount;
	private final String valueName;
	private final Character valueSep;
	private final Set<String> allowedValues;
	private final List<String> defaults;
	private final String key;

	public ImmutableParameter(Parameter other) {
		if (other == null) { throw new IllegalArgumentException("Must provide a parameter definition to copy from"); }
		this.required = other.isRequired();
		this.description = other.getDescription();
		this.shortOpt = other.getShortOpt();
		this.longOpt = other.getLongOpt();
		this.minValueCount = other.getMinValueCount();
		this.maxValueCount = other.getMaxValueCount();
		this.valueName = other.getValueName();
		this.valueSep = other.getValueSep();
		this.allowedValues = Tools.freezeCopy(other.getAllowedValues(), true);
		this.defaults = Tools.freezeCopy(other.getDefaults(), true);
		this.key = other.getKey();
	}

	public MutableParameter thawCopy() {
		return new MutableParameter(this);
	}

	@Override
	public String getKey() {
		return this.key;
	}

	@Override
	public int getMinValueCount() {
		return this.minValueCount;
	}

	@Override
	public int getMaxValueCount() {
		return this.maxValueCount;
	}

	@Override
	public String getValueName() {
		return this.valueName;
	}

	@Override
	public boolean isRequired() {
		return this.required;
	}

	@Override
	public String getDescription() {
		return this.description;
	}

	@Override
	public String getLongOpt() {
		return this.longOpt;
	}

	@Override
	public Character getShortOpt() {
		return this.shortOpt;
	}

	@Override
	public Character getValueSep() {
		return this.valueSep;
	}

	@Override
	public Set<String> getAllowedValues() {
		return this.allowedValues;
	}

	@Override
	public String getDefault() {
		return this.defaults.isEmpty() ? null : this.defaults.get(0);
	}

	@Override
	public List<String> getDefaults() {
		return this.defaults.isEmpty() ? null : this.defaults;
	}

	@Override
	public String toString() {
		return String.format(
			"ImmutableParameter [key=%s, required=%s, shortOpt=%s, longOpt=%s, description=%s, minValueCount=%d, maxValueCount=%d, valueName=%s, minValueCount=%s, valueSep=%s, defaults=%s]",
			this.key, this.required, this.shortOpt, this.longOpt, this.description, this.minValueCount,
			this.maxValueCount, this.valueName, this.minValueCount, this.valueSep, this.defaults);
	}
}