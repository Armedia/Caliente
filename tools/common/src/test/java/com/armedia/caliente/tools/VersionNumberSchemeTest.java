package com.armedia.caliente.tools;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.armedia.caliente.tools.AlphaCounter;
import com.armedia.caliente.tools.VersionNumberScheme;

public class VersionNumberSchemeTest {

	@FunctionalInterface
	private static interface Renderer {
		public String render(int value, boolean first);
	}

	private static final String ALNUM_ALPHABET = "0123456789" + AlphaCounter.ALPHABET;

	private static final Renderer RENDER_NUM = (i, f) -> f || (i > 0) ? String.valueOf(i) : "";
	private static final Renderer RENDER_ALPHA = (i, f) -> f || (i > 0) ? AlphaCounter.renderAlpha(i) : "";
	private static final Renderer RENDER_ALNUM = (i,
		f) -> f || (i > 0) ? AlphaCounter.render(i, VersionNumberSchemeTest.ALNUM_ALPHABET) : "";

	private void renderSuffix(Renderer renderer, int dots, int values, List<String> target, String base,
		Character sep) {
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
		Assertions.assertEquals(0, VersionNumberScheme.basicCompare(null, null, true));
		Assertions.assertEquals(0, VersionNumberScheme.basicCompare(null, "", true));
		Assertions.assertEquals(0, VersionNumberScheme.basicCompare("", null, true));
		Assertions.assertEquals(0, VersionNumberScheme.basicCompare("", "", true));
		Assertions.assertEquals(-1, VersionNumberScheme.basicCompare(null, "0", true));
		Assertions.assertEquals(-1, VersionNumberScheme.basicCompare("", "0", true));
		Assertions.assertEquals(1, VersionNumberScheme.basicCompare("0", null, true));
		Assertions.assertEquals(1, VersionNumberScheme.basicCompare("0", "", true));

		Assertions.assertEquals(0, VersionNumberScheme.basicCompare(null, null, false));
		Assertions.assertEquals(0, VersionNumberScheme.basicCompare(null, "", false));
		Assertions.assertEquals(0, VersionNumberScheme.basicCompare("", null, false));
		Assertions.assertEquals(0, VersionNumberScheme.basicCompare("", "", false));
		Assertions.assertEquals(1, VersionNumberScheme.basicCompare(null, "0", false));
		Assertions.assertEquals(1, VersionNumberScheme.basicCompare("", "0", false));
		Assertions.assertEquals(-1, VersionNumberScheme.basicCompare("0", null, false));
		Assertions.assertEquals(-1, VersionNumberScheme.basicCompare("0", "", false));

		Assertions.assertEquals(0, VersionNumberScheme.basicCompare(String.valueOf(0), "0", true));
		Assertions.assertEquals(0, VersionNumberScheme.basicCompare("0", String.valueOf(0), false));

		Assertions.assertNull(VersionNumberScheme.basicCompare("0", "1", true));
		Assertions.assertNull(VersionNumberScheme.basicCompare("1", "0", true));
		Assertions.assertNull(VersionNumberScheme.basicCompare("0", "1", false));
		Assertions.assertNull(VersionNumberScheme.basicCompare("1", "0", false));
	}

	private void test(BiFunction<Character, Boolean, Comparator<String>> comparator, Renderer renderer, Character sep,
		int dots, int values) {
		Comparator<String> c = null;
		List<String> l = null;

		// The happy path
		c = comparator.apply(sep, null);
		l = new ArrayList<>(100);
		renderSuffix(renderer, dots, values, l, null, sep);
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
		c = comparator.apply(sep, true);
		l = new ArrayList<>(100);
		l.add(null);
		renderSuffix(renderer, dots, values, l, null, sep);

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
		c = comparator.apply(sep, false);
		l = new ArrayList<>(100);
		renderSuffix(renderer, dots, values, l, null, sep);
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
	public void testGetNumeric() {
		BiFunction<Character, Boolean, Comparator<String>> c = (s, b) -> {
			if (b == null) { return VersionNumberScheme.getNumeric(s); }
			return VersionNumberScheme.getNumeric(s, b);
		};
		test(c, VersionNumberSchemeTest.RENDER_NUM, null, 0, 10);
		test(c, VersionNumberSchemeTest.RENDER_NUM, '.', 3, 5);
	}

	@Test
	public void testGetAlphabetic() {
		BiFunction<Character, Boolean, Comparator<String>> c = (s, b) -> {
			if (b == null) { return VersionNumberScheme.getAlphabetic(AlphaCounter.ALPHABET, s); }
			return VersionNumberScheme.getAlphabetic(AlphaCounter.ALPHABET, s, b);
		};
		test(c, VersionNumberSchemeTest.RENDER_ALPHA, null, 0, 10);
		test(c, VersionNumberSchemeTest.RENDER_ALPHA, '.', 3, 5);
	}

	@Test
	public void testGetAlphanumeric() {
		BiFunction<Character, Boolean, Comparator<String>> c = (s, b) -> {
			if (b == null) { return VersionNumberScheme.getAlphanumeric(s); }
			return VersionNumberScheme.getAlphanumeric(s, b);
		};
		test(c, VersionNumberSchemeTest.RENDER_ALNUM, null, 0, 10);
		test(c, VersionNumberSchemeTest.RENDER_ALNUM, '.', 3, 5);
	}
}