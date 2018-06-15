package com.armedia.caliente.cli.token;

import java.io.StringReader;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class ReaderTokenSourceTest {

	@Test
	public void testTokenize() throws Exception {
		String str = "abc c d asd fa sdf  fa\\\\ sdf\\ asdf\\r\\n\\r\\n\\t\\f\\\" rest					 of the         \\t                      stuff ' single \"quote   ' \"quoted ' stuff\"";
		List<String> expected = Arrays.asList(new String[] {
			"abc", //
			"c", //
			"d", //
			"asd", //
			"fa", //
			"sdf", //
			"fa\\", //
			"sdf asdf\r\n\r\n\t\f\"", //
			"rest", //
			"of", //
			"the", //
			"\t", //
			"stuff", //
			" single \"quote   ", //
			"quoted ' stuff", //
		});
		ReaderTokenSource source = new CharacterSequenceTokenSource(str);
		List<String> actual = source.getTokenStrings();
		Assert.assertEquals("Token counts", expected.size(), actual.size());
		for (int i = 0; i < actual.size(); i++) {
			Assert.assertEquals(String.format("Mismatch found at token %d", i), expected.get(i), actual.get(i));
		}
	}

	@Test
	public void testReadQuoted() throws Exception {
		String str = "abc c d asd fa sdf  fa\\\\ sdf\\ asdf\\r\\n\\r\\n\\t\\f\\\" rest of the stuff ' ' ' \"";
		String expected = "abc c d asd fa sdf  fa\\ sdf\\ asdf\r\n\r\n\t\f\" rest of the stuff ' ' ' ";
		ReaderTokenSource source = new CharacterSequenceTokenSource(str);
		String actual = source.readQuoted(new StringReader(str), '"');
		Assert.assertEquals(expected, actual);
	}
}