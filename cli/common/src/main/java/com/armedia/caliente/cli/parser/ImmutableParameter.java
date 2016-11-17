package com.armedia.caliente.cli.parser;

import java.util.Set;

import com.armedia.commons.utilities.Tools;

public final class ImmutableParameter extends BaseParameter implements Cloneable {

	private final boolean required;
	private final String description;
	private final Character shortOpt;
	private final String longOpt;
	private final int minValueCount;
	private final int maxValueCount;
	private final String valueName;
	private final Character valueSep;
	private final Set<String> allowedValues;

	ImmutableParameter(Parameter other) {
		this.required = other.isRequired();
		this.description = other.getDescription();
		this.shortOpt = other.getShortOpt();
		this.longOpt = other.getLongOpt();
		this.minValueCount = other.getMinValueCount();
		this.maxValueCount = other.getMaxValueCount();
		this.valueName = other.getValueName();
		this.valueSep = other.getValueSep();
		this.allowedValues = Tools.freezeCopy(other.getAllowedValues(), true);
	}

	@Override
	public ImmutableParameter clone() {
		return new ImmutableParameter(this);
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
	public int hashCode() {
		return Tools.hashTool(this, null, this.required, this.description, this.shortOpt, this.longOpt, this.valueName,
			this.maxValueCount, this.valueSep, this.allowedValues);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		ImmutableParameter other = ImmutableParameter.class.cast(obj);
		return isEqual(other);
	}

	@Override
	public boolean isEqual(Parameter other) {
		if (other == null) { return false; }
		if (isRequired() != other.isRequired()) { return false; }
		if (getMinValueCount() != other.getMinValueCount()) { return false; }
		if (getMaxValueCount() != other.getMaxValueCount()) { return false; }
		if (!Tools.equals(getDescription(), other.getDescription())) { return false; }
		if (!Tools.equals(getLongOpt(), other.getLongOpt())) { return false; }
		if (!Tools.equals(getShortOpt(), other.getShortOpt())) { return false; }
		if (!Tools.equals(getValueName(), other.getValueName())) { return false; }
		if (!Tools.equals(getValueSep(), other.getValueSep())) { return false; }
		if (!Tools.equals(getAllowedValues(), other.getAllowedValues())) { return false; }
		return true;
	}

	@Override
	public String toString() {
		return String.format(
			"MutableParameter [required=%s, shortOpt=%s, longOpt=%s, description=%s, minValueCount=%d, maxValueCount=%d, valueName=%s, minValueCount=%s, valueSep=%s]",
			this.required, this.shortOpt, this.longOpt, this.description, this.minValueCount, this.maxValueCount,
			this.valueName, this.minValueCount, this.valueSep);
	}
}