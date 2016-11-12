package com.armedia.caliente.cli.parser;

import com.armedia.commons.utilities.Tools;

public final class MutableParameterDefinition extends BaseParameterDefinition implements Cloneable {

	protected boolean required = false;
	protected String description = null;
	protected Character shortOpt = null;
	protected String longOpt = null;
	protected int valueCount = 0;
	protected String valueName = null;
	protected boolean valueOptional = false;
	protected Character valueSep = MutableParameterDefinition.DEFAULT_VALUE_SEP;

	MutableParameterDefinition(ParameterDefinition other) {
		this.required = other.isRequired();
		this.description = other.getDescription();
		this.shortOpt = other.getShortOpt();
		this.longOpt = other.getLongOpt();
		this.valueCount = other.getValueCount();
		this.valueName = other.getValueName();
		this.valueOptional = other.isValueOptional();
		this.valueSep = other.getValueSep();
	}

	public MutableParameterDefinition() {
	}

	@Override
	public MutableParameterDefinition clone() {
		return new MutableParameterDefinition(this);
	}

	@Override
	public int getValueCount() {
		return this.valueCount;
	}

	public MutableParameterDefinition setValueCount(int parameterCount) {
		this.valueCount = parameterCount;
		return this;
	}

	@Override
	public String getValueName() {
		return this.valueName;
	}

	public MutableParameterDefinition setValueName(String argName) {
		this.valueName = argName;
		return this;
	}

	@Override
	public boolean isRequired() {
		return this.required;
	}

	public MutableParameterDefinition setRequired(boolean required) {
		this.required = required;
		return this;
	}

	@Override
	public String getDescription() {
		return this.description;
	}

	public MutableParameterDefinition setDescription(String description) {
		this.description = description;
		return this;
	}

	@Override
	public String getLongOpt() {
		return this.longOpt;
	}

	public MutableParameterDefinition setLongOpt(String longOpt) {
		this.longOpt = longOpt;
		return this;
	}

	@Override
	public Character getShortOpt() {
		return this.shortOpt;
	}

	public MutableParameterDefinition setShortOpt(Character shortOpt) {
		this.shortOpt = shortOpt;
		return this;
	}

	@Override
	public Character getValueSep() {
		return this.valueSep;
	}

	public MutableParameterDefinition setValueSep(Character valueSep) {
		this.valueSep = valueSep;
		return this;
	}

	@Override
	public boolean isValueOptional() {
		return this.valueOptional;
	}

	public MutableParameterDefinition setValueOptional(boolean valueOptional) {
		this.valueOptional = valueOptional;
		return this;
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.required, this.description, this.shortOpt, this.longOpt, this.valueName,
			this.valueCount, this.valueOptional, this.valueSep);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		MutableParameterDefinition other = MutableParameterDefinition.class.cast(obj);
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
			"MutableParameterDefinition [required=%s, shortOpt=%s, longOpt=%s, description=%s, valueCount=%s, valueName=%s, valueOptional=%s, valueSep=%s]",
			this.required, this.shortOpt, this.longOpt, this.description, this.valueCount, this.valueName,
			this.valueOptional, this.valueSep);
	}
}