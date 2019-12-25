package com.armedia.caliente.engine.local.exporter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.armedia.caliente.tools.AlphaCounter;

public class LocalVersionSchemeTest {

	@FunctionalInterface
	private static interface Renderer {
		public String render(int value, boolean first);
	}

	private static final Renderer RENDER_INT = (i, f) -> f || (i > 0) ? String.valueOf(i) : "";
	private static final Renderer RENDER_ALPHA = (i, f) -> f || (i > 0) ? AlphaCounter.renderAlpha(i) : "";

	private void renderSuffix(Renderer renderer, int dots, int values, List<String> target, String base, char sep) {
		if (dots < 0) { return; }
		final String trueBase = (!StringUtils.isEmpty(base) ? base + sep : "");
		final boolean first = StringUtils.isEmpty(trueBase);
		for (int v = 0; v < values; v++) {
			final String s = renderer.render(v, first);
			if (!StringUtils.isEmpty(s)) {
				final String newBase = trueBase + s;
				target.add(newBase);
				renderSuffix(renderer, dots - 1, values, target, newBase, sep);
			}
		}
	}

	@Test
	public void testBasicCompare() {
		Assertions.assertEquals(0, LocalVersionScheme.basicCompare(null, null, true));
		Assertions.assertEquals(0, LocalVersionScheme.basicCompare(null, "", true));
		Assertions.assertEquals(0, LocalVersionScheme.basicCompare("", null, true));
		Assertions.assertEquals(0, LocalVersionScheme.basicCompare("", "", true));
		Assertions.assertEquals(-1, LocalVersionScheme.basicCompare(null, "0", true));
		Assertions.assertEquals(-1, LocalVersionScheme.basicCompare("", "0", true));
		Assertions.assertEquals(1, LocalVersionScheme.basicCompare("0", null, true));
		Assertions.assertEquals(1, LocalVersionScheme.basicCompare("0", "", true));

		Assertions.assertEquals(0, LocalVersionScheme.basicCompare(null, null, false));
		Assertions.assertEquals(0, LocalVersionScheme.basicCompare(null, "", false));
		Assertions.assertEquals(0, LocalVersionScheme.basicCompare("", null, false));
		Assertions.assertEquals(0, LocalVersionScheme.basicCompare("", "", false));
		Assertions.assertEquals(1, LocalVersionScheme.basicCompare(null, "0", false));
		Assertions.assertEquals(1, LocalVersionScheme.basicCompare("", "0", false));
		Assertions.assertEquals(-1, LocalVersionScheme.basicCompare("0", null, false));
		Assertions.assertEquals(-1, LocalVersionScheme.basicCompare("0", "", false));

		Assertions.assertEquals(0, LocalVersionScheme.basicCompare(String.valueOf(0), "0", true));
		Assertions.assertEquals(0, LocalVersionScheme.basicCompare("0", String.valueOf(0), false));

		Assertions.assertNull(LocalVersionScheme.basicCompare("0", "1", true));
		Assertions.assertNull(LocalVersionScheme.basicCompare("1", "0", true));
		Assertions.assertNull(LocalVersionScheme.basicCompare("0", "1", false));
		Assertions.assertNull(LocalVersionScheme.basicCompare("1", "0", false));
	}

	@Test
	public void testGetNumeric() {
		Comparator<String> c = null;
		List<String> l = null;

		// The happy path
		c = LocalVersionScheme.getNumeric();
		l = new ArrayList<>(100);
		renderSuffix(LocalVersionSchemeTest.RENDER_INT, 0, 10, l, null, '.');
		l.add(null);

		for (int a = 0; a < l.size(); a++) {
			final String A = l.get(a);

			Assertions.assertEquals(0, c.compare(A, A));
			if (A == null) {
				Assertions.assertEquals(0, c.compare(null, A));
				Assertions.assertEquals(0, c.compare(A, null));
			} else {
				Assertions.assertEquals(1, c.compare(null, A));
				Assertions.assertEquals(-1, c.compare(A, null));
			}

			for (int b = 0; b < l.size(); b++) {
				final String B = l.get(b);
				final int expected;
				if (a == b) {
					expected = 0;
				} else if (a < b) {
					expected = -1;
				} else {
					expected = 1;
				}
				final int x = a;
				final int y = b;
				Assertions.assertEquals(expected, c.compare(A, B),
					() -> String.format("%d.[%s] vs %d.[%s]", x, A, y, B));
			}
		}

		// The happy path, with emptyIsRoot flag set
		c = LocalVersionScheme.getNumeric(true);
		l = new ArrayList<>(100);
		l.add(null);
		renderSuffix(LocalVersionSchemeTest.RENDER_INT, 0, 10, l, null, '.');

		for (int a = 0; a < l.size(); a++) {
			final String A = l.get(a);

			Assertions.assertEquals(0, c.compare(A, A));
			if (A == null) {
				Assertions.assertEquals(0, c.compare(null, A));
				Assertions.assertEquals(0, c.compare(A, null));
			} else {
				Assertions.assertEquals(-1, c.compare(null, A));
				Assertions.assertEquals(1, c.compare(A, null));
			}

			for (int b = 0; b < l.size(); b++) {
				final String B = l.get(b);
				final int expected;
				if (a == b) {
					expected = 0;
				} else if (a < b) {
					expected = -1;
				} else {
					expected = 1;
				}
				final int x = a;
				final int y = b;
				Assertions.assertEquals(expected, c.compare(A, B),
					() -> String.format("%d.[%s] vs %d.[%s]", x, A, y, B));
			}
		}

		// The happy path with emptyIsRoot unset
		c = LocalVersionScheme.getNumeric(false);
		l = new ArrayList<>(100);
		renderSuffix(LocalVersionSchemeTest.RENDER_INT, 0, 10, l, null, '.');
		l.add(null);

		for (int a = 0; a < l.size(); a++) {
			final String A = l.get(a);

			Assertions.assertEquals(0, c.compare(A, A));
			if (A == null) {
				Assertions.assertEquals(0, c.compare(null, A));
				Assertions.assertEquals(0, c.compare(A, null));
			} else {
				Assertions.assertEquals(1, c.compare(null, A));
				Assertions.assertEquals(-1, c.compare(A, null));
			}

			for (int b = 0; b < l.size(); b++) {
				final String B = l.get(b);
				final int expected;
				if (a == b) {
					expected = 0;
				} else if (a < b) {
					expected = -1;
				} else {
					expected = 1;
				}
				final int x = a;
				final int y = b;
				Assertions.assertEquals(expected, c.compare(A, B),
					() -> String.format("%d.[%s] vs %d.[%s]", x, A, y, B));
			}
		}
	}

	@Test
	public void testGetDottedNumeric() {
		Comparator<String> c = null;
		List<String> l = null;

		// The happy path
		c = LocalVersionScheme.getNumeric('.');
		l = new ArrayList<>(100);
		renderSuffix(LocalVersionSchemeTest.RENDER_INT, 3, 5, l, "", '.');
		l.add(null);

		for (int a = 0; a < l.size(); a++) {
			final String A = l.get(a);

			Assertions.assertEquals(0, c.compare(A, A));
			if (A == null) {
				Assertions.assertEquals(0, c.compare(null, A));
				Assertions.assertEquals(0, c.compare(A, null));
			} else {
				Assertions.assertEquals(1, c.compare(null, A));
				Assertions.assertEquals(-1, c.compare(A, null));
			}

			for (int b = 0; b < l.size(); b++) {
				final String B = l.get(b);
				final int expected;
				if (a == b) {
					expected = 0;
				} else if (a < b) {
					expected = -1;
				} else {
					expected = 1;
				}
				final int x = a;
				final int y = b;
				Assertions.assertEquals(expected, c.compare(A, B),
					() -> String.format("%d.[%s] vs %d.[%s]", x, A, y, B));
			}
		}

		// The happy path, with emptyIsRoot flag set
		c = LocalVersionScheme.getNumeric('.', true);
		l = new ArrayList<>(100);
		l.add(null);
		renderSuffix(LocalVersionSchemeTest.RENDER_INT, 3, 5, l, "", '.');

		for (int a = 0; a < l.size(); a++) {
			final String A = l.get(a);

			Assertions.assertEquals(0, c.compare(A, A));
			if (A == null) {
				Assertions.assertEquals(0, c.compare(null, A));
				Assertions.assertEquals(0, c.compare(A, null));
			} else {
				Assertions.assertEquals(-1, c.compare(null, A));
				Assertions.assertEquals(1, c.compare(A, null));
			}

			for (int b = 0; b < l.size(); b++) {
				final String B = l.get(b);
				final int expected;
				if (a == b) {
					expected = 0;
				} else if (a < b) {
					expected = -1;
				} else {
					expected = 1;
				}
				final int x = a;
				final int y = b;
				Assertions.assertEquals(expected, c.compare(A, B),
					() -> String.format("%d.[%s] vs %d.[%s]", x, A, y, B));
			}
		}

		// The happy path with emptyIsRoot unset
		c = LocalVersionScheme.getNumeric('.', false);
		l = new ArrayList<>(100);
		renderSuffix(LocalVersionSchemeTest.RENDER_INT, 3, 5, l, "", '.');
		l.add(null);

		for (int a = 0; a < l.size(); a++) {
			final String A = l.get(a);

			Assertions.assertEquals(0, c.compare(A, A));
			if (A == null) {
				Assertions.assertEquals(0, c.compare(null, A));
				Assertions.assertEquals(0, c.compare(A, null));
			} else {
				Assertions.assertEquals(1, c.compare(null, A));
				Assertions.assertEquals(-1, c.compare(A, null));
			}

			for (int b = 0; b < l.size(); b++) {
				final String B = l.get(b);
				final int expected;
				if (a == b) {
					expected = 0;
				} else if (a < b) {
					expected = -1;
				} else {
					expected = 1;
				}
				final int x = a;
				final int y = b;
				Assertions.assertEquals(expected, c.compare(A, B),
					() -> String.format("%d.[%s] vs %d.[%s]", x, A, y, B));
			}
		}
	}

	@Test
	public void testGetAlphanumeric() {
		Assertions.fail("Not yet implemented");
	}

	@Test
	public void testGetAlphabetic() {
		Comparator<String> c = null;
		List<String> l = null;

		// The happy path
		c = LocalVersionScheme.getAlphabetic(AlphaCounter.ALPHABET);
		l = new ArrayList<>(100);
		renderSuffix(LocalVersionSchemeTest.RENDER_ALPHA, 0, 10, l, null, '.');
		l.add(null);

		for (int a = 0; a < l.size(); a++) {
			final String A = l.get(a);

			Assertions.assertEquals(0, c.compare(A, A));
			if (A == null) {
				Assertions.assertEquals(0, c.compare(null, A));
				Assertions.assertEquals(0, c.compare(A, null));
			} else {
				Assertions.assertEquals(1, c.compare(null, A));
				Assertions.assertEquals(-1, c.compare(A, null));
			}

			for (int b = 0; b < l.size(); b++) {
				final String B = l.get(b);
				final int expected;
				if (a == b) {
					expected = 0;
				} else if (a < b) {
					expected = -1;
				} else {
					expected = 1;
				}
				final int x = a;
				final int y = b;
				Assertions.assertEquals(expected, c.compare(A, B),
					() -> String.format("%d.[%s] vs %d.[%s]", x, A, y, B));
			}
		}

		// The happy path, with emptyIsRoot flag set
		c = LocalVersionScheme.getAlphabetic(AlphaCounter.ALPHABET, true);
		l = new ArrayList<>(100);
		l.add(null);
		renderSuffix(LocalVersionSchemeTest.RENDER_ALPHA, 0, 10, l, null, '.');

		for (int a = 0; a < l.size(); a++) {
			final String A = l.get(a);

			Assertions.assertEquals(0, c.compare(A, A));
			if (A == null) {
				Assertions.assertEquals(0, c.compare(null, A));
				Assertions.assertEquals(0, c.compare(A, null));
			} else {
				Assertions.assertEquals(-1, c.compare(null, A));
				Assertions.assertEquals(1, c.compare(A, null));
			}

			for (int b = 0; b < l.size(); b++) {
				final String B = l.get(b);
				final int expected;
				if (a == b) {
					expected = 0;
				} else if (a < b) {
					expected = -1;
				} else {
					expected = 1;
				}
				final int x = a;
				final int y = b;
				Assertions.assertEquals(expected, c.compare(A, B),
					() -> String.format("%d.[%s] vs %d.[%s]", x, A, y, B));
			}
		}

		// The happy path with emptyIsRoot unset
		c = LocalVersionScheme.getAlphabetic(AlphaCounter.ALPHABET, false);
		l = new ArrayList<>(100);
		renderSuffix(LocalVersionSchemeTest.RENDER_ALPHA, 0, 10, l, null, '.');
		l.add(null);

		for (int a = 0; a < l.size(); a++) {
			final String A = l.get(a);

			Assertions.assertEquals(0, c.compare(A, A));
			if (A == null) {
				Assertions.assertEquals(0, c.compare(null, A));
				Assertions.assertEquals(0, c.compare(A, null));
			} else {
				Assertions.assertEquals(1, c.compare(null, A));
				Assertions.assertEquals(-1, c.compare(A, null));
			}

			for (int b = 0; b < l.size(); b++) {
				final String B = l.get(b);
				final int expected;
				if (a == b) {
					expected = 0;
				} else if (a < b) {
					expected = -1;
				} else {
					expected = 1;
				}
				final int x = a;
				final int y = b;
				Assertions.assertEquals(expected, c.compare(A, B),
					() -> String.format("%d.[%s] vs %d.[%s]", x, A, y, B));
			}
		}
	}
}