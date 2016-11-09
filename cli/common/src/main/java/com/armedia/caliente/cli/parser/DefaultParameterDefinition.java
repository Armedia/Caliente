package com.armedia.caliente.cli.parser;

public class DefaultParameterDefinition implements ParameterDefinition, Cloneable {

	public static final char DEFAULT_VALUE_SEP = ',';

	protected boolean required = false;
	protected String description = null;
	protected Character shortOpt = null;
	protected String longOpt = null;
	protected int valueCount = 0;
	protected String valueName = null;
	protected boolean valueOptional = false;
	protected Character valueSep = DefaultParameterDefinition.DEFAULT_VALUE_SEP;

	private DefaultParameterDefinition(DefaultParameterDefinition other) {
		this.required = other.required;
		this.description = other.description;
		this.shortOpt = other.shortOpt;
		this.longOpt = other.longOpt;
		this.valueCount = other.valueCount;
		this.valueName = other.valueName;
		this.valueOptional = other.valueOptional;
		this.valueSep = other.valueSep;
	}

	public DefaultParameterDefinition() {
	}

	@Override
	public DefaultParameterDefinition clone() {
		return new DefaultParameterDefinition(this);
	}

	@Override
	public int getValueCount() {
		return this.valueCount;
	}

	public void setParameterCount(int parameterCount) {
		this.valueCount = parameterCount;
	}

	@Override
	public String getValueName() {
		return this.valueName;
	}

	public void setArgName(String argName) {
		this.valueName = argName;
	}

	@Override
	public boolean isRequired() {
		return this.required;
	}

	public void setRequired(boolean required) {
		this.required = required;
	}

	@Override
	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public String getLongOpt() {
		return this.longOpt;
	}

	public void setLongOpt(String longOpt) {
		this.longOpt = longOpt;
	}

	@Override
	public Character getShortOpt() {
		return this.shortOpt;
	}

	public void setShortOpt(Character shortOpt) {
		this.shortOpt = shortOpt;
	}

	@Override
	public Character getValueSep() {
		return this.valueSep;
	}

	public void setValueSep(Character valueSep) {
		this.valueSep = valueSep;
	}

	@Override
	public boolean isValueOptional() {
		return this.valueOptional;
	}

	public void setValueOptional(boolean valueOptional) {
		this.valueOptional = valueOptional;
	}

	@Override
	public String toString() {
		return String.format(
			"DefaultParameterDefinition [required=%s, shortOpt=%s, longOpt=%s, description=%s, valueCount=%s, valueName=%s, valueOptional=%s, valueSep=%s]",
			this.required, this.shortOpt, this.longOpt, this.description, this.valueCount, this.valueName,
			this.valueOptional, this.valueSep);
	}
}