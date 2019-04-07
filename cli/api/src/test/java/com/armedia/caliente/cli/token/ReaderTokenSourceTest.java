package com.armedia.caliente.cli.token;

import java.io.StringReader;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
		Assertions.assertEquals(expected.size(), actual.size(), "Token counts");
		for (int i = 0; i < actual.size(); i++) {
			Assertions.assertEquals(expected.get(i), actual.get(i), String.format("Mismatch found at token %d", i));
		}
	}

	@Test
	public void testReadQuoted() throws Exception {
		String str = "abc c d asd fa sdf  fa\\\\ sdf\\ asdf\\r\\n\\r\\n\\t\\f\\\" rest of the stuff ' ' ' \"";
		String expected = "abc c d asd fa sdf  fa\\ sdf\\ asdf\r\n\r\n\t\f\" rest of the stuff ' ' ' ";
		ReaderTokenSource source = new CharacterSequenceTokenSource(str);
		String actual = source.readQuoted(new StringReader(str), '"');
		Assertions.assertEquals(expected, actual);
	}
}