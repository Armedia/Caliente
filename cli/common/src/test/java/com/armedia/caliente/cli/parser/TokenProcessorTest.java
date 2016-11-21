package com.armedia.caliente.cli.parser;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;

public class TokenProcessorTest {

	static {

		ArgumentParser parser = ArgumentParsers.newArgumentParser("Checksum").defaultHelp(true);
		parser.addArgument("a");
	}

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
			"-a", "--bb", "subcommand", "asdfasdf", "@@classpath:/test-parameter-file.txt", "--",
			"@@classpath:/test-parameter-file.txt", "--ff"
		};

		final ParserErrorPolicy errorPolicy = new ParserErrorPolicy() {

			@Override
			public boolean isErrorMissingValues(Token token, Parameter parameter, List<String> values) {
				System.out.printf("MISSING VALUES FOR %s (%d @ %s)%n", parameter.getKey(), token.index,
					token.source != null ? token.source.getKey() : "(root)");
				return false;
			}

			@Override
			public boolean isErrorTooManyValues(Token token, Parameter parameter, List<String> values) {
				System.out.printf("TOO MANY VALUES FOR %s (%d @ %s): %s%n", parameter.getKey(), token.index,
					token.source != null ? token.source.getKey() : "(root)", values);
				return false;
			}

			@Override
			public boolean isErrorUnknownParameterFound(Token token) {
				System.out.printf("UNKNOWN PARAMETER %s (%d @ %s)%n", token.value, token.index,
					token.source != null ? token.source.getKey() : "(root)");
				return false;
			}
		};
		ParameterProcessorSet rootParams = new ParameterProcessorSet("root") {
			@Override
			public ParserErrorPolicy getErrorPolicy() {
				return errorPolicy;
			}
		};
		rootParams.addParameter(new MutableParameter().setShortOpt('a'));
		rootParams.addParameter(new MutableParameter().setLongOpt("bb"));
		rootParams.addSubProcessor("subcommand", new ParameterProcessor("subcommand") {
			@Override
			public ParserErrorPolicy getErrorPolicy() {
				return errorPolicy;
			}
		});

		p.processTokens(rootParams, args);
	}
}