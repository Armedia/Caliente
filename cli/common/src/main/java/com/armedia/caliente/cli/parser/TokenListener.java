package com.armedia.caliente.cli.parser;

import java.io.File;
import java.util.List;

public interface TokenListener {

	public void positionalParametersFound(List<String> values);

	public void namedParameterFound(Parameter parameter, List<String> values);

	public void terminatorFound();

	public void subCommandFound(String subCommand);

	public void extraArguments(List<String> arguments);

	public boolean missingValues(File sourceFile, int index, Parameter parameter);

	public boolean tooManyValues(File sourceFile, int index, Parameter parameter, List<String> values);

	public boolean unknownParameterFound(File sourceFile, int index, String value);

	public boolean orphanedValueFound(File sourceFile, int index, String value);

}