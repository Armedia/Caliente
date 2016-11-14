package com.armedia.caliente.cli.parser;

import java.util.Set;

public interface ParameterDefinition {

	public String getKey();

	public boolean isRequired();

	public String getDescription();

	public String getLongOpt();

	public Character getShortOpt();

	public Character getValueSep();

	public Set<String> getAllowedValues();

	public String getValueName();

	public int getValueCount();

	public boolean isValueOptional();

	public boolean isEqual(ParameterDefinition other);

}