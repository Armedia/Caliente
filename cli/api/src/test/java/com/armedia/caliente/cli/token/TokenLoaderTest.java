package com.armedia.caliente.cli.token;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class TokenLoaderTest {

	@Test
	public void testParser() {
		List<String> l = Collections.emptyList();
		TokenLoader p = new TokenLoader(new StaticTokenSource("primary", l));
		Assert.assertNotNull(p);
		Assert.assertEquals(TokenLoader.DEFAULT_VALUE_SEPARATOR, p.getValueSeparator());
	}

	@Test
	public void testParse() throws Exception {
		String[] args = null;

		args = new String[] {
			"-a", "--bb", "XXX", "-MuLtIsHoRt", "subcommand", "asdfasdf", "--@@", "classpath:/test-parameter-file.txt",
			"--", "--@@", "classpath:/test-parameter-file.txt", "--ff"
		};

		for (Token t : new TokenLoader(new StaticTokenSource("primary", Arrays.asList(args)))) {
			System.out.printf("%s%n", t);
		}
	}
}