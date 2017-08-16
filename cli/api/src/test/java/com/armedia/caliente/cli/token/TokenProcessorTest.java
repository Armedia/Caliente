package com.armedia.caliente.cli.token;

import java.util.Arrays;
import java.util.Collections;
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
		List<String> l = Collections.emptyList();
		TokenLoader p = new TokenLoader(new TokenConstantSource("primary", l));
		Assert.assertNotNull(p);
		Assert.assertEquals(TokenLoader.DEFAULT_PARAMETER_MARKER, p.getParameterMarker());
		Assert.assertEquals(TokenLoader.DEFAULT_FILE_MARKER, p.getFileMarker());
		Assert.assertEquals(TokenLoader.DEFAULT_VALUE_SPLITTER, p.getValueSplitter());
	}

	@Test
	public void testParse() throws Exception {
		String[] args = null;

		args = new String[] {
			"-a", "--bb", "XXX", "subcommand", "asdfasdf", "--@@classpath:/test-parameter-file.txt", "--",
			"--@@classpath:/test-parameter-file.txt", "--ff"
		};

		for (Token t : new TokenLoader(new TokenConstantSource("primary", Arrays.asList(args)))) {
			System.out.printf("%s%n", t);
		}
	}
}