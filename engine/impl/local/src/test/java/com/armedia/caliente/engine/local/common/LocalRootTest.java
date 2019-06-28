/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
 * %%
 * This file is part of the Caliente software. 
 *  
 * If the software was purchased under a paid Caliente license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *   
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
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
