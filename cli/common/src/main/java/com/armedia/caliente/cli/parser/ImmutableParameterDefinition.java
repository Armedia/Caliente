package com.armedia.caliente.cli.parser;

import com.armedia.commons.utilities.Tools;

public final class ImmutableParameterDefinition extends BaseParameterDefinition implements Cloneable {

	private final boolean required;
	private final String description;
	private final Character shortOpt;
	private final String longOpt;
	private final int valueCount;
	private final String valueName;
	private final boolean valueOptional;
	private final Character valueSep;

	public ImmutableParameterDefinition(ParameterDefinition other) {
		this.required = other.isRequired();
		this.description = other.getDescription();
		this.shortOpt = other.getShortOpt();
		this.longOpt = other.getLongOpt();
		this.valueCount = other.getValueCount();
		this.valueName = other.getValueName();
		this.valueOptional = other.isValueOptional();
		this.valueSep = other.getValueSep();
	}

	@Override
	public ImmutableParameterDefinition clone() {
		return new ImmutableParameterDefinition(this);
	}

	@Override
	public int getValueCount() {
		return this.valueCount;
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
	public boolean isValueOptional() {
		return this.valueOptional;
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.required, this.description, this.shortOpt, this.longOpt, this.valueName,
			this.valueCount, this.valueOptional, this.valueSep);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		ImmutableParameterDefinition other = ImmutableParameterDefinition.class.cast(obj);
		return isEqual(other);
	}

	@Override
	public boolean isEqual(ParameterDefinition other) {
		if (other == null) { return false; }
		if (isRequired() != other.isRequired()) { return false; }
		if (isValueOptional() != other.isValueOptional()) { return false; }
		if (!Tools.equals(getDescription(), other.getDescription())) { return false; }
		if (!Tools.equals(getLongOpt(), other.getLongOpt())) { return false; }
		if (!Tools.equals(getShortOpt(), other.getShortOpt())) { return false; }
		if (getValueCount() != other.getValueCount()) { return false; }
		if (!Tools.equals(getValueName(), other.getValueName())) { return false; }
		if (!Tools.equals(getValueSep(), other.getValueSep())) { return false; }
		return true;
	}

	@Override
	public String toString() {
		return String.format(
			"ImmutableParameterDefinition [required=%s, shortOpt=%s, longOpt=%s, description=%s, valueCount=%s, valueName=%s, valueOptional=%s, valueSep=%s]",
			this.required, this.shortOpt, this.longOpt, this.description, this.valueCount, this.valueName,
			this.valueOptional, this.valueSep);
	}
}