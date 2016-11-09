package com.armedia.caliente.cli.parser;

public class DefaultParameterDefinition implements ParameterDefinition, Cloneable {

	public static final char DEFAULT_VALUE_SEP = ',';

	protected boolean required = false;
	protected String description = null;
	protected Character shortOpt = null;
	protected String longOpt = null;
	protected int parameterCount = 0;
	protected String argName = null;
	protected boolean optionalArg = false;
	protected Character valueSep = DefaultParameterDefinition.DEFAULT_VALUE_SEP;

	private DefaultParameterDefinition(DefaultParameterDefinition other) {
		this.required = other.required;
		this.description = other.description;
		this.shortOpt = other.shortOpt;
		this.longOpt = other.longOpt;
		this.parameterCount = other.parameterCount;
		this.argName = other.argName;
		this.optionalArg = other.optionalArg;
		this.valueSep = other.valueSep;
	}

	public DefaultParameterDefinition() {
	}

	@Override
	public DefaultParameterDefinition clone() {
		return new DefaultParameterDefinition(this);
	}

	@Override
	public int getParameterCount() {
		return this.parameterCount;
	}

	public void setParameterCount(int parameterCount) {
		this.parameterCount = parameterCount;
	}

	@Override
	public String getArgName() {
		return this.argName;
	}

	public void setArgName(String argName) {
		this.argName = argName;
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
	public boolean isOptionalArg() {
		return this.optionalArg;
	}

	public void setOptionalArg(boolean optionalArg) {
		this.optionalArg = optionalArg;
	}

	@Override
	public String toString() {
		return String.format(
			"DefaultParameterDefinition [required=%s, shortOpt=%s, longOpt=%s, description=%s, parameterCount=%s, argName=%s, optionalArg=%s, valueSep=%s]",
			this.required, this.shortOpt, this.longOpt, this.description, this.parameterCount, this.argName,
			this.optionalArg, this.valueSep);
	}
}