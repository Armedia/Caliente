package com.armedia.caliente.cli.parser;

import java.util.List;

public interface TokenListener {

	public void positionalParametersFound(List<String> values);

	public void namedParameterFound(Parameter parameter, List<String> values);

	public void terminatorFound(Token token);

	public void subCommandFound(String subCommand);

	public void extraArguments(List<String> arguments);

	public boolean isErrorMissingValues(Token token, Parameter parameter, List<String> values);

	public boolean isErrorTooManyValues(Token token, Parameter parameter, List<String> values);

	public boolean isErrorUnknownParameterFound(Token token);

	public boolean isErrorOrphanedValueFound(Token token);

}