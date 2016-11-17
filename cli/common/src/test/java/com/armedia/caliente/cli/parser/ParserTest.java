package com.armedia.caliente.cli.parser;

import org.junit.Assert;
import org.junit.Test;

import com.armedia.caliente.cli.parser.Parser.ParameterSet;

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
			"-a", "--bb", "asdfasdf", "@file", "--", "@file", "--ff"
		};

		ParameterSet parameters = null;
		ParserListener listener = null;

		p.parse(parameters, listener, args);
	}

}