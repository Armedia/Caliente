package com.armedia.caliente.cli.token;

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
		TokenResolver p = new TokenResolver();
		Assert.assertNotNull(p);
		Assert.assertEquals(TokenResolver.DEFAULT_PARAMETER_MARKER, p.getParameterMarker());
		Assert.assertEquals(TokenResolver.DEFAULT_FILE_MARKER, p.getFileMarker());
		Assert.assertEquals(TokenResolver.DEFAULT_VALUE_SPLITTER, p.getValueSplitter());
	}

	@Test
	public void testParse() throws Exception {
		String[] args = null;

		TokenResolver p = new TokenResolver();

		args = new String[] {
			"-a", "--bb", "XXX", "subcommand", "asdfasdf", "--@@classpath:/test-parameter-file.txt", "--",
			"--@@classpath:/test-parameter-file.txt", "--ff"
		};

		System.out.printf("%s%n", p.identifyTokens(args));
	}
}