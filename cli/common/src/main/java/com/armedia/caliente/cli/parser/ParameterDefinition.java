package com.armedia.caliente.cli.parser;

public interface ParameterDefinition {

	public boolean isRequired();

	public String getDescription();

	public String getLongOpt();

	public Character getShortOpt();

	public Character getValueSep();

	public String getValueName();

	public int getValueCount();

	public boolean isValueOptional();

}