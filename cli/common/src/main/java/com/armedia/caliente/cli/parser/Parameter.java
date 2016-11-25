package com.armedia.caliente.cli.parser;

import java.util.List;
import java.util.Set;

public interface Parameter {

	public String getKey();

	public boolean isRequired();

	public String getDescription();

	public String getLongOpt();

	public Character getShortOpt();

	public Character getValueSep();

	public Set<String> getAllowedValues();

	public String getValueName();

	public int getMinValueCount();

	public int getMaxValueCount();

	public String getDefault();

	public List<String> getDefaults();
}