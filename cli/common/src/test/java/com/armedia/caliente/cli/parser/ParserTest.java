package com.armedia.caliente.cli.parser;

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
	public void testGetParameterMarker() {
	}

	@Test
	public void testGetFileMarker() {
	}

	@Test
	public void testGetValueSplitter() {
	}

	@Test
	public void testParse() {
	}

}