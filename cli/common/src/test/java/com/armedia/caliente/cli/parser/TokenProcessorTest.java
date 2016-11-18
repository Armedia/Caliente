package com.armedia.caliente.cli.parser;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class TokenProcessorTest {

	@Test
	public void testParser() {
		TokenProcessor p = new TokenProcessor();
		Assert.assertNotNull(p);
		Assert.assertEquals(TokenProcessor.DEFAULT_PARAMETER_MARKER, p.getParameterMarker());
		Assert.assertEquals(TokenProcessor.DEFAULT_FILE_MARKER, p.getFileMarker());
		Assert.assertEquals(TokenProcessor.DEFAULT_VALUE_SPLITTER, p.getValueSplitter());
	}

	@Test
	public void testParse() throws Exception {
		String[] args = null;

		TokenProcessor p = new TokenProcessor();

		args = new String[] {
			"-a", "--bb", "subcommand", "asdfasdf", "@@classpath:/test-parameter-file.txt", "--", "@file", "--ff"
		};

		RootParameterSet rootParams = new RootParameterSet("root");
		rootParams.addParameter(new MutableParameter().setShortOpt('a'));
		rootParams.addParameter(new MutableParameter().setLongOpt("bb"));
		rootParams.addSubcommand("subcommand", new ParameterSet("subcommand"));

		TokenListener listener = new TokenListener() {

			@Override
			public void positionalParametersFound(List<String> values) {
				System.out.printf("POSITIONAL: %s%n", values);
			}

			@Override
			public void namedParameterFound(Parameter parameter, List<String> values) {
				System.out.printf("PARAMETER: %s %s%n", parameter.getKey(), values);
			}

			@Override
			public void terminatorFound(TokenSource source, int index) {
				System.out.printf("TERMINATOR (%d @ %s)%n", index, source != null ? source.getKey() : "(root)");
			}

			@Override
			public void subCommandFound(String subCommand) {
				System.out.printf("SUBCOMMAND [%s]%n", subCommand);
			}

			@Override
			public void extraArguments(List<String> arguments) {
				System.out.printf("EXTRA ARGS: %s%n", arguments);
			}

			@Override
			public boolean missingValues(TokenSource source, int index, Parameter parameter) {
				System.out.printf("MISSING VALUES FOR %s (%d @ %s)%n", parameter.getKey(), index,
					source != null ? source.getKey() : "(root)");
				return false;
			}

			@Override
			public boolean tooManyValues(TokenSource source, int index, Parameter parameter, List<String> values) {
				System.out.printf("TOO MANY VALUES FOR %s (%d @ %s): %s%n", parameter.getKey(), index,
					source != null ? source.getKey() : "(root)", values);
				return false;
			}

			@Override
			public boolean unknownParameterFound(TokenSource source, int index, String value) {
				System.out.printf("UNKNOWN PARAMETER %s (%d @ %s)%n", value, index,
					source != null ? source.getKey() : "(root)");
				return false;
			}

			@Override
			public boolean orphanedValueFound(TokenSource source, int index, String value) {
				System.out.printf("ORPHANED VALUE %s (%d @ %s)%n", value, index,
					source != null ? source.getKey() : "(root)");
				return false;
			}
		};
		p.processTokens(rootParams, listener, args);
	}
}