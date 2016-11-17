package com.armedia.caliente.cli.parser;

import java.io.File;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class ParserTest {

	@Test
	public void testParser() {
		Parser p = new Parser();
		Assert.assertNotNull(p);
		Assert.assertEquals(Parser.DEFAULT_PARAMETER_MARKER, p.getParameterMarker());
		Assert.assertEquals(Parser.DEFAULT_FILE_MARKER, p.getFileMarker());
		Assert.assertEquals(Parser.DEFAULT_VALUE_SPLITTER, p.getValueSplitter());
	}

	@Test
	public void testParse() throws Exception {
		String[] args = null;

		Parser p = new Parser();

		args = new String[] {
			"-a", "--bb", "asdfasdf", "--", "@file", "--ff"
		};

		RootParameterSet rootParams = new RootParameterSet("root");
		TokenListener listener = new TokenListener() {

			@Override
			public void positionalParametersFound(List<String> values) {
				System.out.printf("POSITIONAL: %s", values);
			}

			@Override
			public void namedParameterFound(Parameter parameter, List<String> values) {
				System.out.printf("PARAMETER: %s %s", parameter.getKey(), values);
			}

			@Override
			public void terminatorFound() {
				System.out.printf("TERMINATOR");
			}

			@Override
			public void subCommandFound(String subCommand) {
				System.out.printf("SUBCOMMAND [%s]", subCommand);
			}

			@Override
			public void extraArguments(List<String> arguments) {
				System.out.printf("EXTRA ARGS: %s", arguments);
			}

			@Override
			public boolean missingValues(File sourceFile, int index, Parameter parameter) {
				return false;
			}

			@Override
			public boolean tooManyValues(File sourceFile, int index, Parameter parameter, List<String> values) {
				return false;
			}

			@Override
			public boolean unknownParameterFound(File sourceFile, int index, String value) {
				return false;
			}

			@Override
			public boolean orphanedValueFound(File sourceFile, int index, String value) {
				return false;
			}
		};
		p.processTokens(rootParams, listener, args);
	}

}