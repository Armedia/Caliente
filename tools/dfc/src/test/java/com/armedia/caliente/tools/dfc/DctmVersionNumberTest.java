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
package com.armedia.caliente.tools.dfc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.armedia.commons.utilities.Tools;

public class DctmVersionNumberTest {

	private static final int majors = 9;
	private static final int minorsPerLevel = 16;
	// private static final int branchLevels = 4;

	private static Set<String> BRANCHED_VERSIONS = Collections.emptySet();
	private static Map<String, Integer> COMPONENT_COUNTERS = Collections.emptyMap();
	private static Set<String> TEST_VERSIONS = Collections.emptySet();

	@BeforeAll
	public static void setUpBeforeClass() throws Exception {
		Set<String> branchedVersions = new TreeSet<>();

		for (int M = 1; M <= DctmVersionNumberTest.majors; M++) {
			for (int m = 0; m < DctmVersionNumberTest.minorsPerLevel; m++) {
				branchedVersions.add(String.format("%s.%s", M, m));
			}
		}

		DctmVersionNumberTest.BRANCHED_VERSIONS = Collections.unmodifiableSet(branchedVersions);

		StringBuilder sb = new StringBuilder();
		Map<String, Integer> componentCounters = new TreeMap<>();
		for (int i = 1; i < 100; i += 2) {
			String part = String.format("%d.%d", i, i + 1);
			sb.append(part);
			String s = sb.toString();
			componentCounters.put(s, i + 1);
			sb.append('.');
		}
		DctmVersionNumberTest.COMPONENT_COUNTERS = Collections.unmodifiableMap(componentCounters);

		Set<String> testVersions = new TreeSet<>();
		testVersions.addAll(DctmVersionNumberTest.BRANCHED_VERSIONS);
		testVersions.addAll(DctmVersionNumberTest.COMPONENT_COUNTERS.keySet());
		DctmVersionNumberTest.TEST_VERSIONS = Collections.unmodifiableSet(testVersions);
	}

	@AfterAll
	public static void tearDownAfterClass() throws Exception {
		DctmVersionNumberTest.BRANCHED_VERSIONS = Collections.emptySet();
		DctmVersionNumberTest.COMPONENT_COUNTERS = Collections.emptyMap();
		DctmVersionNumberTest.TEST_VERSIONS = Collections.emptySet();
	}

	@BeforeEach
	public void setUp() throws Exception {
	}

	@AfterEach
	public void tearDown() throws Exception {
	}

	@Test
	public void testHashCode() {
		for (String s : DctmVersionNumberTest.TEST_VERSIONS) {
			DfcVersionNumber a = new DfcVersionNumber(s);
			DfcVersionNumber b = new DfcVersionNumber(s);
			Assertions.assertEquals(a.hashCode(), b.hashCode(), s);
		}
	}

	@Test
	public void testDctmVersionNumber() {
		try {
			new DfcVersionNumber(null);
			Assertions.fail("Did not fail with a null parameter");
		} catch (IllegalArgumentException e) {
			// all is well
		}
		try {
			new DfcVersionNumber("");
			Assertions.fail("Did not fail with an empty string parameter");
		} catch (IllegalArgumentException e) {
			// all is well
		}
		try {
			new DfcVersionNumber("                     ");
			Assertions.fail("Did not fail with a spaces-only parameter");
		} catch (IllegalArgumentException e) {
			// all is well
		}
	}

	@Test
	public void testGetComponent() {
		for (String s : DctmVersionNumberTest.COMPONENT_COUNTERS.keySet()) {
			DfcVersionNumber vn = new DfcVersionNumber(s);
			// Integer components = DctmVersionNumberTest.COMPONENTS.get(s);
			int cc = vn.getComponentCount();
			for (int i = 1; i <= cc; i++) {
				Assertions.assertEquals(i, vn.getComponent(i - 1), String.format("%s[%d]", s, i - 1));
			}
		}
	}

	@Test
	public void testGetLastComponent() {
		for (String s : DctmVersionNumberTest.COMPONENT_COUNTERS.keySet()) {
			DfcVersionNumber vn = new DfcVersionNumber(s);
			Integer components = DctmVersionNumberTest.COMPONENT_COUNTERS.get(s);
			Assertions.assertEquals(components.intValue(), vn.getLastComponent(), s);
		}
	}

	@Test
	public void testGetComponentCount() {
		for (String s : DctmVersionNumberTest.COMPONENT_COUNTERS.keySet()) {
			DfcVersionNumber vn = new DfcVersionNumber(s);
			Integer components = DctmVersionNumberTest.COMPONENT_COUNTERS.get(s);
			Assertions.assertEquals(components.intValue(), vn.getComponentCount(), s);
		}
	}

	@Test
	public void testToString() {
		for (String s : DctmVersionNumberTest.TEST_VERSIONS) {
			DfcVersionNumber vn = new DfcVersionNumber(s);
			Assertions.assertEquals(s, vn.toString());
		}
	}

	@Test
	public void testToStringInt() {
	}

	@Test
	public void testEqualsObject() {
		for (String a : DctmVersionNumberTest.TEST_VERSIONS) {
			final DfcVersionNumber va = new DfcVersionNumber(a);
			for (String b : DctmVersionNumberTest.TEST_VERSIONS) {
				final DfcVersionNumber vb = new DfcVersionNumber(b);
				if (a.equals(b)) {
					Assertions.assertEquals(va, vb, String.format("[%s] vs [%s]", a, b));
					Assertions.assertEquals(vb, va, String.format("[%s] vs [%s]", b, a));
				} else {
					Assertions.assertNotEquals(va, vb, String.format("[%s] vs [%s]", a, b));
					Assertions.assertNotEquals(vb, va, String.format("[%s] vs [%s]", b, a));
				}
			}
		}
	}

	@Test
	public void testEqualsDctmVersionNumberInt() {
		for (String a : DctmVersionNumberTest.TEST_VERSIONS) {
			final Pattern pA = Pattern.compile(String.format("^\\Q%s\\E(?:\\.\\d+\\.\\d+)*$", a));
			final DfcVersionNumber va = new DfcVersionNumber(a);
			for (String b : DctmVersionNumberTest.TEST_VERSIONS) {
				final DfcVersionNumber vb = new DfcVersionNumber(b);
				final Pattern pB = Pattern.compile(String.format("^\\Q%s\\E(?:\\.\\d+\\.\\d+)*$", b));
				if (a.equals(b)) {
					// Same numbers, so they must match throughout
					final int c = va.getComponentCount();
					for (int i = 1; i <= c; i++) {
						Assertions.assertTrue(va.equals(vb, i), String.format("A-eq-B[%s] vs [%s] @ %d", a, b, i));
						Assertions.assertTrue(vb.equals(va, i), String.format("B-eq-A[%s] vs [%s] @ %d", b, a, i));
					}
					continue;
				}

				// A is prefixed by B
				if (pB.matcher(a).matches()) {
					// b is a prefix of a, so they must match to at least b-components
					final int aC = va.getComponentCount();
					final int bC = vb.getComponentCount();
					for (int i = 1; i <= aC; i++) {
						if (i <= bC) {
							Assertions.assertTrue(va.equals(vb, i),
								String.format("1:A-sw-B[%s] vs [%s] @ %d", a, b, i));
							Assertions.assertTrue(vb.equals(va, i),
								String.format("1:A-sw-B[%s] vs [%s] @ %d", b, a, i));
						} else {
							Assertions.assertFalse(va.equals(vb, i),
								String.format("2:A-sw-B[%s] vs [%s] @ %d", a, b, i));
							Assertions.assertFalse(vb.equals(va, i),
								String.format("2:A-sw-B[%s] vs [%s] @ %d", b, a, i));
						}
					}
					continue;
				}

				// B is prefixed by A
				if (pA.matcher(b).matches()) {
					// a is a prefix of b, so they must match to at least a-components
					final int aC = va.getComponentCount();
					final int bC = vb.getComponentCount();
					for (int i = 1; i <= bC; i++) {
						if (i <= aC) {
							Assertions.assertTrue(va.equals(vb, i),
								String.format("1:B-sw-A[%s] vs [%s] @ %d", a, b, i));
							Assertions.assertTrue(vb.equals(va, i),
								String.format("1:B-sw-A[%s] vs [%s] @ %d", b, a, i));
						} else {
							Assertions.assertFalse(va.equals(vb, i),
								String.format("2:B-sw-A[%s] vs [%s] @ %d", a, b, i));
							Assertions.assertFalse(vb.equals(va, i),
								String.format("2:B-sw-A[%s] vs [%s] @ %d", b, a, i));
						}
					}
					continue;
				}

				String common = StringUtils.getCommonPrefix(a, b);
				if (!StringUtils.isEmpty(common) && !common.endsWith(".")) {
					// Make sure we cut to the last dot, this stops .1 from matching with .10
					common = common.substring(0, common.lastIndexOf('.') + 1);
				}

				if (StringUtils.isEmpty(common)) {
					// No common branch, so they must NOT have any matches at any depth
					int c = va.getComponentCount();
					for (int i = 1; i <= c; i++) {
						Assertions.assertFalse(va.equals(vb, i), String.format("NE[%s] vs [%s] @ %d", a, b, i));
					}
					c = vb.getComponentCount();
					for (int i = 1; i <= c; i++) {
						Assertions.assertFalse(vb.equals(va, i), String.format("NE[%s] vs [%s] @ %d", b, a, i));
					}
				} else {
					// there is a common prefix...figure out how many dots it has, and that's
					// how many components are shared
					final int aC = va.getComponentCount();
					final int bC = vb.getComponentCount();
					final int cC = StringUtils.countMatches(common, ".");
					for (int i = 1; i <= Math.max(aC, bC); i++) {
						if (i <= cC) {
							Assertions.assertTrue(va.equals(vb, i), String.format("1:CMN[%s] vs [%s] @ %d", a, b, i));
							Assertions.assertTrue(vb.equals(va, i), String.format("1:CMN[%s] vs [%s] @ %d", b, a, i));
						} else {
							Assertions.assertFalse(va.equals(vb, i), String.format("2:CMN[%s] vs [%s] @ %d", a, b, i));
							Assertions.assertFalse(vb.equals(va, i), String.format("2:CMN[%s] vs [%s] @ %d", b, a, i));
						}
					}
				}
			}
		}
	}

	@Test
	public void testClone() {
		for (String s : DctmVersionNumberTest.TEST_VERSIONS) {
			DfcVersionNumber a = new DfcVersionNumber(s);
			DfcVersionNumber b = a.clone();
			Assertions.assertNotSame(a, b);
			Assertions.assertEquals(a, b);
			Assertions.assertNotSame(b, a);
			Assertions.assertEquals(b, a);
		}
	}

	@Test
	public void testIsSibling() {
		for (final String a : DctmVersionNumberTest.TEST_VERSIONS) {
			final DfcVersionNumber va = new DfcVersionNumber(a);
			for (final String b : DctmVersionNumberTest.TEST_VERSIONS) {
				final DfcVersionNumber vb = new DfcVersionNumber(b);
				// For A to be a sibling of B, they must have the same number of components,
				// and must match on all components except the last pair
				boolean expected = ((va.getComponentCount() == vb.getComponentCount())
					&& va.equals(vb, va.getComponentCount() - 1) && vb.equals(va, vb.getComponentCount() - 1)
					&& (va.getLastComponent() != vb.getLastComponent()));
				Assertions.assertEquals(expected, va.isSibling(vb), String.format("[%s] vs [%s]", a, b));
			}
		}
	}

	@Test
	public void testIsSuccessorOf() {
		for (final String a : DctmVersionNumberTest.TEST_VERSIONS) {
			final DfcVersionNumber va = new DfcVersionNumber(a);
			for (final String b : DctmVersionNumberTest.TEST_VERSIONS) {
				final DfcVersionNumber vb = new DfcVersionNumber(b);
				// For A to be a successor of B, A must have the same number of components as B,
				// all must match except the last one, and the last component from A must be
				// greater than the last one from B. There is an edge case when the length is
				// 2: A is compared to B in their logical, version number order, and A is a
				// successor of B if A sorts after B
				boolean expected = (((va.getComponentCount() == vb.getComponentCount())
					&& (((va.getComponentCount() == 2) && (va.compareTo(vb) > 0))
						|| ((va.equals(vb, vb.getComponentCount() - 1) && vb.equals(va, va.getComponentCount() - 1))
							&& (va.getLastComponent() > vb.getLastComponent())))));
				Assertions.assertEquals(expected, va.isSuccessorOf(vb), String.format("[%s] vs [%s]", a, b));
			}
		}
	}

	@Test
	public void testIsAntecedentOf() {
		for (final String a : DctmVersionNumberTest.TEST_VERSIONS) {
			final DfcVersionNumber va = new DfcVersionNumber(a);
			for (final String b : DctmVersionNumberTest.TEST_VERSIONS) {
				final DfcVersionNumber vb = new DfcVersionNumber(b);
				// For A to be an antecedent of B, A must have the same number of components as B,
				// all must match except the last one, and the last component from A must be
				// less than the last one from B. There is an edge case when the length is
				// 2: A is compared to B in their logical, version number order, and A is a
				// successor of B if A sorts before B
				boolean expected = (((va.getComponentCount() == vb.getComponentCount())
					&& (((va.getComponentCount() == 2) && (va.compareTo(vb) < 0))
						|| ((va.equals(vb, vb.getComponentCount() - 1) && vb.equals(va, va.getComponentCount() - 1))
							&& (va.getLastComponent() < vb.getLastComponent())))));
				Assertions.assertEquals(expected, va.isAntecedentOf(vb), String.format("[%s] vs [%s]", a, b));
			}
		}
	}

	@Test
	public void testIsAncestorOf() {
		for (final String a : DctmVersionNumberTest.TEST_VERSIONS) {
			final DfcVersionNumber va = new DfcVersionNumber(a);
			for (final String b : DctmVersionNumberTest.TEST_VERSIONS) {
				final DfcVersionNumber vb = new DfcVersionNumber(b);
				// For A to be an ancestor of B, B must have more components than A,
				// and must match with all the components of A
				boolean expected = ((va.getComponentCount() < vb.getComponentCount())
					&& vb.equals(va, va.getComponentCount()));
				Assertions.assertEquals(expected, va.isAncestorOf(vb), String.format("[%s] vs [%s]", a, b));
			}
		}
	}

	@Test
	public void testIsDescendantOf() {
		for (final String a : DctmVersionNumberTest.TEST_VERSIONS) {
			final DfcVersionNumber va = new DfcVersionNumber(a);
			for (final String b : DctmVersionNumberTest.TEST_VERSIONS) {
				final DfcVersionNumber vb = new DfcVersionNumber(b);
				// For A to be a descendant of B, A must have more components than B,
				// and must match with all the components of B
				boolean expected = ((va.getComponentCount() > vb.getComponentCount())
					&& va.equals(vb, vb.getComponentCount()));
				Assertions.assertEquals(expected, va.isDescendantOf(vb), String.format("[%s] vs [%s]", a, b));
			}
		}
	}

	@Test
	public void testGetDepthInCommon() {
		for (String a : DctmVersionNumberTest.TEST_VERSIONS) {
			DfcVersionNumber va = new DfcVersionNumber(a);
			String[] ca = a.split("\\.");
			for (String b : DctmVersionNumberTest.TEST_VERSIONS) {
				DfcVersionNumber vb = new DfcVersionNumber(b);
				String[] cb = b.split("\\.");

				int c = 0;
				while ((c < ca.length) && (c < cb.length) && Tools.equals(ca[c], cb[c])) {
					c++;
				}
				// We've found a mismatch, so check to see if the code does the same thing
				Assertions.assertEquals(c, va.getDepthInCommon(vb), String.format("[%s] vs [%s]", a, b));
			}
		}
	}

	@Test
	public void testCompareTo() {
		List<DfcVersionNumber> l = new ArrayList<>(DctmVersionNumberTest.TEST_VERSIONS.size());
		for (String s : DctmVersionNumberTest.TEST_VERSIONS) {
			DfcVersionNumber va = new DfcVersionNumber(s);
			DfcVersionNumber vb = new DfcVersionNumber(s);
			l.add(va);
			Assertions.assertEquals(0, va.compareTo(vb), s);
			Assertions.assertEquals(0, vb.compareTo(va), s);
		}

		Collections.sort(l);
		DfcVersionNumber a = null;
		for (DfcVersionNumber b : l) {
			if (a != null) {
				Assertions.assertTrue(a.compareTo(b) <= 0, String.format("[%s] vs [%s]", a, b));
			}
			a = b;
		}

		Collections.reverse(l);
		a = null;
		for (DfcVersionNumber b : l) {
			if (a != null) {
				Assertions.assertTrue(a.compareTo(b) >= 0, String.format("[%s] vs [%s]", a, b));
			}
			a = b;
		}
	}

	@Test
	public void testGetSubset() {
		List<DfcVersionNumber> l = new ArrayList<>(DctmVersionNumberTest.COMPONENT_COUNTERS.size());
		for (String s : DctmVersionNumberTest.COMPONENT_COUNTERS.keySet()) {
			l.add(new DfcVersionNumber(s));
		}
		Collections.sort(l);
		Collections.reverse(l);
		DfcVersionNumber big = null;
		for (DfcVersionNumber small : l) {
			if (big != null) {
				int len = big.getComponentCount();
				if (len == 2) {
					continue;
				}
				Assertions.assertEquals(small, big.getSubset(len - 2));
			}
			big = small;
		}
	}

	@Test
	public void testGetAntecedent() {
		// TODO: This is a PITA to test...
	}

	@Test
	public void testGetAntecedentBoolean() {
		// TODO: This is a PITA to test...
	}

	@Test
	public void testGetAllAntecedents() {
		// TODO: This is a PITA to test...
	}

	@Test
	public void testGetAllAntecedentsBoolean() {
		// TODO: This is a PITA to test...
	}
}