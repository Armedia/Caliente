package com.armedia.caliente.cli.parser;

public interface ParameterDefinition {

	int getParameterCount();

	String getArgName();

	boolean isRequired();

	String getDescription();

	String getLongOpt();

	Character getShortOpt();

	Character getValueSep();

	boolean isOptionalArg();

}