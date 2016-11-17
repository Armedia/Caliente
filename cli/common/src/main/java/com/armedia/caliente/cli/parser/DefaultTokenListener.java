package com.armedia.caliente.cli.parser;

import java.io.File;
import java.util.List;

public class DefaultTokenListener implements TokenListener {

	@Override
	public void positionalParametersFound(List<String> values) {

	}

	@Override
	public void namedParameterFound(Parameter parameter, List<String> values) {
	}

	@Override
	public void terminatorFound() {
	}

	@Override
	public void subCommandFound(String subCommand) {
	}

	@Override
	public void extraArguments(List<String> arguments) {
	}

	@Override
	public boolean missingValues(File sourceFile, int index, Parameter parameter) {
		return true;
	}

	@Override
	public boolean tooManyValues(File sourceFile, int index, Parameter parameter, List<String> values) {
		return true;
	}

	@Override
	public boolean unknownParameterFound(File sourceFile, int index, String value) {
		return true;
	}

	@Override
	public boolean orphanedValueFound(File sourceFile, int index, String value) {
		return true;
	}
}