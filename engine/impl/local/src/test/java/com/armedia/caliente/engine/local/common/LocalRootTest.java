package com.armedia.caliente.engine.local.common;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.armedia.commons.utilities.Tools;

class LocalRootTest {

	private static final File TMP_DIR = Tools.canonicalize(FileUtils.getTempDirectory());

	private static final File NULL_FILE = null;
	private static final String NULL_STRING = null;

	@Test
	void testConstructors() throws IOException {
		Assertions.assertThrows(NullPointerException.class, () -> new LocalRoot(LocalRootTest.NULL_FILE));
		Assertions.assertThrows(IOException.class, () -> new LocalRoot(LocalRootTest.NULL_STRING));

		new LocalRoot(LocalRootTest.TMP_DIR);
		new LocalRoot(LocalRootTest.TMP_DIR.toString());
		new LocalRoot(".");
		new LocalRoot("");

	}

	@Test
	void testNormalize() throws Exception {
		Assertions.assertThrows(IOException.class, () -> LocalRoot.normalize("../.."));
		Assertions.assertThrows(IOException.class, () -> LocalRoot.normalize(".."));
		Assertions.assertThrows(IOException.class, () -> LocalRoot.normalize("dir/../.."));

		String[][] data = {
			{
				"a/b/c/d/e", "a/b/c/d/e"
			}, {
				"a////b/c/d/e", "a/b/c/d/e"
			}, {
				"a/b/../c/d/e", "a/c/d/e"
			}, {
				"a/b/./c/d/../e", "a/b/c/e"
			},
		};
		for (String[] o : data) {
			String src = o[0];
			String tgt = o[1];
			Assertions.assertEquals(tgt, LocalRoot.normalize(src));
		}

	}

	@Test
	void testGetPath() {
	}

	@Test
	void testGetFile() {
	}

	@Test
	void testRelativizeString() {
	}

	@Test
	void testRelativizeFile() {
	}

	@Test
	void testMakeAbsoluteFile() {
	}

	@Test
	void testMakeAbsoluteString() {
	}

	@Test
	void testCompareTo() {
	}

	@Test
	void testEqualsObject() {
	}

	@Test
	void testHashCode() {
	}

	@Test
	void testToString() {
	}

}
