package com.armedia.cmf.engine.documentum;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.armedia.commons.utilities.Tools;

public class DctmVersionNumberTest {

	private static final int majors = 9;
	private static final int minorsPerLevel = 16;
	// private static final int branchLevels = 4;

	private static Set<String> BRANCHED_VERSIONS = Collections.emptySet();
	private static Map<String, Integer> COMPONENT_COUNTERS = Collections.emptyMap();
	private static Set<String> TEST_VERSIONS = Collections.emptySet();

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Set<String> branchedVersions = new TreeSet<String>();

		for (int M = 1; M <= DctmVersionNumberTest.majors; M++) {
			for (int m = 0; m < DctmVersionNumberTest.minorsPerLevel; m++) {
				branchedVersions.add(String.format("%s.%s", M, m));
			}
		}

		DctmVersionNumberTest.BRANCHED_VERSIONS = Collections.unmodifiableSet(branchedVersions);

		StringBuilder sb = new StringBuilder();
		Map<String, Integer> componentCounters = new TreeMap<String, Integer>();
		for (int i = 1; i < 100; i += 2) {
			String part = String.format("%d.%d", i, i + 1);
			sb.append(part);
			String s = sb.toString();
			componentCounters.put(s, i + 1);
			sb.append('.');
		}
		DctmVersionNumberTest.COMPONENT_COUNTERS = Collections.unmodifiableMap(componentCounters);

		Set<String> testVersions = new TreeSet<String>();
		testVersions.addAll(DctmVersionNumberTest.BRANCHED_VERSIONS);
		testVersions.addAll(DctmVersionNumberTest.COMPONENT_COUNTERS.keySet());
		DctmVersionNumberTest.TEST_VERSIONS = Collections.unmodifiableSet(testVersions);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		DctmVersionNumberTest.BRANCHED_VERSIONS = Collections.emptySet();
		DctmVersionNumberTest.COMPONENT_COUNTERS = Collections.emptyMap();
		DctmVersionNumberTest.TEST_VERSIONS = Collections.emptySet();
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testHashCode() {
		for (String s : DctmVersionNumberTest.TEST_VERSIONS) {
			DctmVersionNumber a = new DctmVersionNumber(s);
			DctmVersionNumber b = new DctmVersionNumber(s);
			Assert.assertEquals(s, a.hashCode(), b.hashCode());
		}
	}

	@Test
	public void testDctmVersionNumber() {
		try {
			new DctmVersionNumber(null);
			Assert.fail("Did not fail with a null parameter");
		} catch (IllegalArgumentException e) {
			// all is well
		}
		try {
			new DctmVersionNumber("");
			Assert.fail("Did not fail with an empty string parameter");
		} catch (IllegalArgumentException e) {
			// all is well
		}
		try {
			new DctmVersionNumber("                     ");
			Assert.fail("Did not fail with a spaces-only parameter");
		} catch (IllegalArgumentException e) {
			// all is well
		}
	}

	@Test
	public void testGetComponent() {
		for (String s : DctmVersionNumberTest.COMPONENT_COUNTERS.keySet()) {
			DctmVersionNumber vn = new DctmVersionNumber(s);
			// Integer components = DctmVersionNumberTest.COMPONENTS.get(s);
			int cc = vn.getComponentCount();
			for (int i = 1; i <= cc; i++) {
				Assert.assertEquals(String.format("%s[%d]", s, i - 1), i, vn.getComponent(i - 1));
			}
		}
	}

	@Test
	public void testGetLastComponent() {
		for (String s : DctmVersionNumberTest.COMPONENT_COUNTERS.keySet()) {
			DctmVersionNumber vn = new DctmVersionNumber(s);
			Integer components = DctmVersionNumberTest.COMPONENT_COUNTERS.get(s);
			Assert.assertEquals(s, components.intValue(), vn.getLastComponent());
		}
	}

	@Test
	public void testGetComponentCount() {
		for (String s : DctmVersionNumberTest.COMPONENT_COUNTERS.keySet()) {
			DctmVersionNumber vn = new DctmVersionNumber(s);
			Integer components = DctmVersionNumberTest.COMPONENT_COUNTERS.get(s);
			Assert.assertEquals(s, components.intValue(), vn.getComponentCount());
		}
	}

	@Test
	public void testToString() {
		for (String s : DctmVersionNumberTest.TEST_VERSIONS) {
			DctmVersionNumber vn = new DctmVersionNumber(s);
			Assert.assertEquals(s, vn.toString());
		}
	}

	@Test
	public void testToStringInt() {
	}

	@Test
	public void testEqualsObject() {
		for (String a : DctmVersionNumberTest.TEST_VERSIONS) {
			final DctmVersionNumber va = new DctmVersionNumber(a);
			for (String b : DctmVersionNumberTest.TEST_VERSIONS) {
				final DctmVersionNumber vb = new DctmVersionNumber(b);
				if (a.equals(b)) {
					Assert.assertTrue(String.format("[%s] vs [%s]", a, b), va.equals(vb));
					Assert.assertTrue(String.format("[%s] vs [%s]", b, a), vb.equals(va));
				} else {
					Assert.assertFalse(String.format("[%s] vs [%s]", a, b), va.equals(vb));
					Assert.assertFalse(String.format("[%s] vs [%s]", b, a), vb.equals(va));
				}
			}
		}
	}

	@Test
	public void testEqualsDctmVersionNumberInt() {
		for (String a : DctmVersionNumberTest.TEST_VERSIONS) {
			final Pattern pA = Pattern.compile(String.format("^\\Q%s\\E(?:\\.\\d+\\.\\d+)*$", a));
			final DctmVersionNumber va = new DctmVersionNumber(a);
			for (String b : DctmVersionNumberTest.TEST_VERSIONS) {
				final DctmVersionNumber vb = new DctmVersionNumber(b);
				final Pattern pB = Pattern.compile(String.format("^\\Q%s\\E(?:\\.\\d+\\.\\d+)*$", b));
				if (a.equals(b)) {
					// Same numbers, so they must match throughout
					final int c = va.getComponentCount();
					for (int i = 1; i <= c; i++) {
						Assert.assertTrue(String.format("A-eq-B[%s] vs [%s] @ %d", a, b, i), va.equals(vb, i));
						Assert.assertTrue(String.format("B-eq-A[%s] vs [%s] @ %d", b, a, i), vb.equals(va, i));
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
							Assert.assertTrue(String.format("1:A-sw-B[%s] vs [%s] @ %d", a, b, i), va.equals(vb, i));
							Assert.assertTrue(String.format("1:A-sw-B[%s] vs [%s] @ %d", b, a, i), vb.equals(va, i));
						} else {
							Assert.assertFalse(String.format("2:A-sw-B[%s] vs [%s] @ %d", a, b, i), va.equals(vb, i));
							Assert.assertFalse(String.format("2:A-sw-B[%s] vs [%s] @ %d", b, a, i), vb.equals(va, i));
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
							Assert.assertTrue(String.format("1:B-sw-A[%s] vs [%s] @ %d", a, b, i), va.equals(vb, i));
							Assert.assertTrue(String.format("1:B-sw-A[%s] vs [%s] @ %d", b, a, i), vb.equals(va, i));
						} else {
							Assert.assertFalse(String.format("2:B-sw-A[%s] vs [%s] @ %d", a, b, i), va.equals(vb, i));
							Assert.assertFalse(String.format("2:B-sw-A[%s] vs [%s] @ %d", b, a, i), vb.equals(va, i));
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
						Assert.assertFalse(String.format("NE[%s] vs [%s] @ %d", a, b, i), va.equals(vb, i));
					}
					c = vb.getComponentCount();
					for (int i = 1; i <= c; i++) {
						Assert.assertFalse(String.format("NE[%s] vs [%s] @ %d", b, a, i), vb.equals(va, i));
					}
				} else {
					// there is a common prefix...figure out how many dots it has, and that's
					// how many components are shared
					final int aC = va.getComponentCount();
					final int bC = vb.getComponentCount();
					final int cC = StringUtils.countMatches(common, ".");
					for (int i = 1; i <= Math.max(aC, bC); i++) {
						if (i <= cC) {
							Assert.assertTrue(String.format("1:CMN[%s] vs [%s] @ %d", a, b, i), va.equals(vb, i));
							Assert.assertTrue(String.format("1:CMN[%s] vs [%s] @ %d", b, a, i), vb.equals(va, i));
						} else {
							Assert.assertFalse(String.format("2:CMN[%s] vs [%s] @ %d", a, b, i), va.equals(vb, i));
							Assert.assertFalse(String.format("2:CMN[%s] vs [%s] @ %d", b, a, i), vb.equals(va, i));
						}
					}
				}
			}
		}
	}

	@Test
	public void testClone() {
		for (String s : DctmVersionNumberTest.TEST_VERSIONS) {
			DctmVersionNumber a = new DctmVersionNumber(s);
			DctmVersionNumber b = a.clone();
			Assert.assertNotSame(a, b);
			Assert.assertEquals(a, b);
			Assert.assertNotSame(b, a);
			Assert.assertEquals(b, a);
		}
	}

	@Test
	public void testIsSibling() {
		for (final String a : DctmVersionNumberTest.TEST_VERSIONS) {
			final DctmVersionNumber va = new DctmVersionNumber(a);
			for (final String b : DctmVersionNumberTest.TEST_VERSIONS) {
				final DctmVersionNumber vb = new DctmVersionNumber(b);
				// For A to be a sibling of B, they must have the same number of components,
				// and must match on all components except the last pair
				boolean expected = ((va.getComponentCount() == vb.getComponentCount())
					&& va.equals(vb, va.getComponentCount() - 1) && vb.equals(va, vb.getComponentCount() - 1)
					&& (va.getLastComponent() != vb.getLastComponent()));
				Assert.assertEquals(String.format("[%s] vs [%s]", a, b), expected, va.isSibling(vb));
			}
		}
	}

	@Test
	public void testIsSuccessorOf() {
		for (final String a : DctmVersionNumberTest.TEST_VERSIONS) {
			final DctmVersionNumber va = new DctmVersionNumber(a);
			for (final String b : DctmVersionNumberTest.TEST_VERSIONS) {
				final DctmVersionNumber vb = new DctmVersionNumber(b);
				// For A to be a successor of B, A must have the same number of components as B,
				// all must match except the last one, and the last component from A must be
				// greater than the last one from B. There is an edge case when the length is
				// 2: A is compared to B in their logical, version number order, and A is a
				// successor of B if A sorts after B
				boolean expected = (((va.getComponentCount() == vb.getComponentCount())
					&& (((va.getComponentCount() == 2) && (va.compareTo(vb) > 0))
						|| ((va.equals(vb, vb.getComponentCount() - 1) && vb.equals(va, va.getComponentCount() - 1))
							&& (va.getLastComponent() > vb.getLastComponent())))));
				Assert.assertEquals(String.format("[%s] vs [%s]", a, b), expected, va.isSuccessorOf(vb));
			}
		}
	}

	@Test
	public void testIsAntecedentOf() {
		for (final String a : DctmVersionNumberTest.TEST_VERSIONS) {
			final DctmVersionNumber va = new DctmVersionNumber(a);
			for (final String b : DctmVersionNumberTest.TEST_VERSIONS) {
				final DctmVersionNumber vb = new DctmVersionNumber(b);
				// For A to be an antecedent of B, A must have the same number of components as B,
				// all must match except the last one, and the last component from A must be
				// less than the last one from B. There is an edge case when the length is
				// 2: A is compared to B in their logical, version number order, and A is a
				// successor of B if A sorts before B
				boolean expected = (((va.getComponentCount() == vb.getComponentCount())
					&& (((va.getComponentCount() == 2) && (va.compareTo(vb) < 0))
						|| ((va.equals(vb, vb.getComponentCount() - 1) && vb.equals(va, va.getComponentCount() - 1))
							&& (va.getLastComponent() < vb.getLastComponent())))));
				Assert.assertEquals(String.format("[%s] vs [%s]", a, b), expected, va.isAntecedentOf(vb));
			}
		}
	}

	@Test
	public void testIsAncestorOf() {
		for (final String a : DctmVersionNumberTest.TEST_VERSIONS) {
			final DctmVersionNumber va = new DctmVersionNumber(a);
			for (final String b : DctmVersionNumberTest.TEST_VERSIONS) {
				final DctmVersionNumber vb = new DctmVersionNumber(b);
				// For A to be an ancestor of B, B must have more components than A,
				// and must match with all the components of A
				boolean expected = ((va.getComponentCount() < vb.getComponentCount())
					&& vb.equals(va, va.getComponentCount()));
				Assert.assertEquals(String.format("[%s] vs [%s]", a, b), expected, va.isAncestorOf(vb));
			}
		}
	}

	@Test
	public void testIsDescendantOf() {
		for (final String a : DctmVersionNumberTest.TEST_VERSIONS) {
			final DctmVersionNumber va = new DctmVersionNumber(a);
			for (final String b : DctmVersionNumberTest.TEST_VERSIONS) {
				final DctmVersionNumber vb = new DctmVersionNumber(b);
				// For A to be a descendant of B, A must have more components than B,
				// and must match with all the components of B
				boolean expected = ((va.getComponentCount() > vb.getComponentCount())
					&& va.equals(vb, vb.getComponentCount()));
				Assert.assertEquals(String.format("[%s] vs [%s]", a, b), expected, va.isDescendantOf(vb));
			}
		}
	}

	@Test
	public void testGetDepthInCommon() {
		for (String a : DctmVersionNumberTest.TEST_VERSIONS) {
			DctmVersionNumber va = new DctmVersionNumber(a);
			String[] ca = a.split("\\.");
			for (String b : DctmVersionNumberTest.TEST_VERSIONS) {
				DctmVersionNumber vb = new DctmVersionNumber(b);
				String[] cb = b.split("\\.");

				int c = 0;
				while ((c < ca.length) && (c < cb.length) && Tools.equals(ca[c], cb[c])) {
					c++;
				}
				// We've found a mismatch, so check to see if the code does the same thing
				Assert.assertEquals(String.format("[%s] vs [%s]", a, b), c, va.getDepthInCommon(vb));
			}
		}
	}

	@Test
	public void testCompareTo() {
		List<DctmVersionNumber> l = new ArrayList<DctmVersionNumber>(DctmVersionNumberTest.TEST_VERSIONS.size());
		for (String s : DctmVersionNumberTest.TEST_VERSIONS) {
			DctmVersionNumber va = new DctmVersionNumber(s);
			DctmVersionNumber vb = new DctmVersionNumber(s);
			l.add(va);
			Assert.assertEquals(s, 0, va.compareTo(vb));
			Assert.assertEquals(s, 0, vb.compareTo(va));
		}

		Collections.sort(l);
		DctmVersionNumber a = null;
		for (DctmVersionNumber b : l) {
			if (a != null) {
				Assert.assertTrue(String.format("[%s] vs [%s]", a, b), a.compareTo(b) <= 0);
			}
			a = b;
		}

		Collections.reverse(l);
		a = null;
		for (DctmVersionNumber b : l) {
			if (a != null) {
				Assert.assertTrue(String.format("[%s] vs [%s]", a, b), a.compareTo(b) >= 0);
			}
			a = b;
		}
	}

	@Test
	public void testGetSubset() {
		List<DctmVersionNumber> l = new ArrayList<DctmVersionNumber>(DctmVersionNumberTest.COMPONENT_COUNTERS.size());
		for (String s : DctmVersionNumberTest.COMPONENT_COUNTERS.keySet()) {
			l.add(new DctmVersionNumber(s));
		}
		Collections.sort(l);
		Collections.reverse(l);
		DctmVersionNumber big = null;
		for (DctmVersionNumber small : l) {
			if (big != null) {
				int len = big.getComponentCount();
				if (len == 2) {
					continue;
				}
				Assert.assertEquals(small, big.getSubset(len - 2));
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