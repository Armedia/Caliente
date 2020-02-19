/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.armedia.commons.utilities.Tools;

public class LocalRootTest {

	private static final Path TMP_DIR = Tools.canonicalize(FileUtils.getTempDirectory()).toPath();

	private static final Path NULL_PATH = null;
	private static final String NULL_STRING = null;

	@Test
	public void testConstructors() throws IOException {
		Assertions.assertThrows(NullPointerException.class, () -> new LocalRoot(LocalRootTest.NULL_PATH));
		Assertions.assertThrows(IOException.class, () -> new LocalRoot(LocalRootTest.NULL_STRING));

		new LocalRoot(LocalRootTest.TMP_DIR);
		new LocalRoot(LocalRootTest.TMP_DIR.toString());
		new LocalRoot(".");
		new LocalRoot("");

	}

	@Test
	public void testNormalize() throws IOException {
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
	public void testRelativize() throws IOException {
		LocalRoot root = new LocalRoot(LocalRootTest.TMP_DIR);
		Path p = null;
		String s = null;

		s = "a/b/c/../../../d/e";
		p = LocalRootTest.TMP_DIR.resolve(s).normalize();
		Assertions.assertEquals(LocalRootTest.TMP_DIR.relativize(p), root.relativize(p));
		s = LocalRoot.normalize(s);
		Assertions.assertEquals(s, root.relativize(s));

		s = null;
		p = null;
		Assertions.assertEquals(LocalRoot.ROOT, root.relativize(p));
		Assertions.assertEquals(File.separator, root.relativize(s));

		s = "/";
		p = LocalRoot.ROOT;
		Assertions.assertEquals(LocalRoot.ROOT, root.relativize(p));
		Assertions.assertEquals(File.separator, root.relativize(s));

		s = "/";
		p = null;
		Assertions.assertEquals(LocalRoot.ROOT, root.relativize(p));
		Assertions.assertEquals(File.separator, root.relativize(s));

		s = null;
		p = LocalRoot.ROOT;
		Assertions.assertEquals(LocalRoot.ROOT, root.relativize(p));
		Assertions.assertEquals(File.separator, root.relativize(s));

		Assertions.assertThrows(IOException.class, () -> root.relativize(Paths.get("/1/2/3/4/5")));
	}

	@Test
	public void testGetPath() throws IOException {
		Path p = Paths.get("/");

		for (int i = 0; i < 10; i++) {
			p = p.resolve(String.valueOf(i));
			LocalRoot root = new LocalRoot(p);
			Assertions.assertEquals(p, root.getPath());
		}
	}

	@Test
	public void testComparisons() throws IOException {
		Path p = Paths.get("/");

		List<LocalRoot> l = new ArrayList<>(10);
		for (int i = 0; i < 10; i++) {
			p = p.resolve(String.valueOf(i));
			l.add(new LocalRoot(p));
		}

		for (int a = 0; a < l.size(); a++) {
			final LocalRoot A = l.get(a);
			Assertions.assertEquals(A, A);
			Assertions.assertNotEquals(null, A);
			Assertions.assertNotEquals(A, null);
			Assertions.assertEquals(0, A.compareTo(A));
			Assertions.assertEquals(1, A.compareTo(null));
			for (int b = 0; b < l.size(); b++) {
				final LocalRoot B = l.get(b);
				if (a == b) {
					Assertions.assertEquals(A, B);
					Assertions.assertEquals(A.hashCode(), B.hashCode());
					Assertions.assertEquals(0, A.compareTo(B));
					Assertions.assertEquals(0, B.compareTo(A));
				} else {
					Assertions.assertNotEquals(A, B);
					Assertions.assertNotEquals(B, A);
					Assertions.assertNotEquals(A.hashCode(), B.hashCode());
					if (a < b) {
						Assertions.assertTrue(A.compareTo(B) < 0);
						Assertions.assertTrue(B.compareTo(A) > 0);
					} else {
						Assertions.assertTrue(A.compareTo(B) > 0);
						Assertions.assertTrue(B.compareTo(A) < 0);
					}
				}
			}
		}
	}

	@Test
	public void testRelativizeFile() {
	}

	@Test
	public void testMakeAbsoluteFile() {
	}

	@Test
	public void testMakeAbsoluteString() {
	}

	@Test
	public void testCompareTo() {
	}

	@Test
	public void testEqualsObject() {
	}

	@Test
	public void testHashCode() {
	}

	@Test
	public void testToString() {
	}

}
