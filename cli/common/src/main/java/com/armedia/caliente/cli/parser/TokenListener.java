package com.armedia.caliente.cli.parser;

import java.util.List;

public interface TokenListener {

	public void positionalParametersFound(List<String> values);

	public void namedParameterFound(Parameter parameter, List<String> values);

	public void terminatorFound();

	public void subCommandFound(String subCommand);

	public void extraArguments(List<String> arguments);

	public boolean missingValues(TokenSource source, int index, Parameter parameter);

	public boolean tooManyValues(TokenSource source, int index, Parameter parameter, List<String> values);

	public boolean unknownParameterFound(TokenSource source, int index, String value);

	public boolean orphanedValueFound(TokenSource source, int index, String value);

}