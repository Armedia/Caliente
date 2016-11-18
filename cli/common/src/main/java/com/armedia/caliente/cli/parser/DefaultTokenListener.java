package com.armedia.caliente.cli.parser;

import java.util.List;

public class DefaultTokenListener implements TokenListener {

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
	public boolean missingValues(Token token, Parameter parameter) {
		return false;
	}

	@Override
	public boolean tooManyValues(Token token, Parameter parameter, List<String> values) {
		return false;
	}

	@Override
	public boolean unknownParameterFound(Token token) {
		return false;
	}

	@Override
	public boolean orphanedValueFound(Token token) {
		return false;
	}
}