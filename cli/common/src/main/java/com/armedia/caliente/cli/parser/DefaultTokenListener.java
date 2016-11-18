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
	public void terminatorFound(TokenSource source, int index) {
	}

	@Override
	public void subCommandFound(String subCommand) {
	}

	@Override
	public void extraArguments(List<String> arguments) {
	}

	@Override
	public boolean missingValues(TokenSource source, int index, Parameter parameter) {
		return true;
	}

	@Override
	public boolean tooManyValues(TokenSource source, int index, Parameter parameter, List<String> values) {
		return true;
	}

	@Override
	public boolean unknownParameterFound(TokenSource source, int index, String value) {
		return true;
	}

	@Override
	public boolean orphanedValueFound(TokenSource source, int index, String value) {
		return true;
	}
}