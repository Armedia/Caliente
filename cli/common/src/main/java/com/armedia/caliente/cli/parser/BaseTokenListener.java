package com.armedia.caliente.cli.parser;

import java.util.List;

public class BaseTokenListener implements TokenListener {

	@Override
	public void positionalParametersFound(List<String> values) {
	}

	@Override
	public void namedParameterFound(Parameter parameter, List<String> values) {
	}

	@Override
	public void terminatorFound(Token token) {
	}

	@Override
	public void subCommandFound(String subCommand) {
	}

	@Override
	public void extraArguments(List<String> arguments) {
	}

	@Override
	public boolean isErrorMissingValues(Token token, Parameter parameter, List<String> values) {
		return true;
	}

	@Override
	public boolean isErrorTooManyValues(Token token, Parameter parameter, List<String> values) {
		return true;
	}

	@Override
	public boolean isErrorUnknownParameterFound(Token token) {
		return true;
	}

	@Override
	public boolean isErrorOrphanedValueFound(Token token) {
		return true;
	}
}