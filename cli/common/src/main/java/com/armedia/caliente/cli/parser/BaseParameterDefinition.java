package com.armedia.caliente.cli.parser;

import com.armedia.commons.utilities.Tools;

public class BaseParameterDefinition implements ParameterDefinition, Cloneable {

	public static final char DEFAULT_VALUE_SEP = ',';

	protected boolean required = false;
	protected String description = null;
	protected Character shortOpt = null;
	protected String longOpt = null;
	protected int valueCount = 0;
	protected String valueName = null;
	protected boolean valueOptional = false;
	protected Character valueSep = BaseParameterDefinition.DEFAULT_VALUE_SEP;

	BaseParameterDefinition(ParameterDefinition other) {
		this.required = other.isRequired();
		this.description = other.getDescription();
		this.shortOpt = other.getShortOpt();
		this.longOpt = other.getLongOpt();
		this.valueCount = other.getValueCount();
		this.valueName = other.getValueName();
		this.valueOptional = other.isValueOptional();
		this.valueSep = other.getValueSep();
	}

	public BaseParameterDefinition() {
	}

	@Override
	public BaseParameterDefinition clone() {
		return new BaseParameterDefinition(this);
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
	public int hashCode() {
		return Tools.hashTool(this, null, this.required, this.description, this.shortOpt, this.longOpt, this.valueName,
			this.valueCount, this.valueOptional, this.valueSep);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		BaseParameterDefinition other = BaseParameterDefinition.class.cast(obj);
		if (this.isRequired() != other.isRequired()) { return false; }
		if (this.isValueOptional() != other.isValueOptional()) { return false; }
		if (!Tools.equals(this.getDescription(), other.getDescription())) { return false; }
		if (!Tools.equals(this.getLongOpt(), other.getLongOpt())) { return false; }
		if (!Tools.equals(this.getShortOpt(), other.getShortOpt())) { return false; }
		if (this.getValueCount() != other.getValueCount()) { return false; }
		if (!Tools.equals(this.getValueName(), other.getValueName())) { return false; }
		if (!Tools.equals(this.getValueSep(), other.getValueSep())) { return false; }
		return true;
	}

	@Override
	public String toString() {
		return String.format(
			"BaseParameterDefinition [required=%s, shortOpt=%s, longOpt=%s, description=%s, valueCount=%s, valueName=%s, valueOptional=%s, valueSep=%s]",
			this.required, this.shortOpt, this.longOpt, this.description, this.valueCount, this.valueName,
			this.valueOptional, this.valueSep);
	}
}