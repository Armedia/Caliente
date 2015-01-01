package com.delta.cmsmf.utils;

import java.util.TreeSet;

import org.junit.Assert;
import org.junit.Test;

public class DfBranchFixerTest {

	@Test
	public void testVersionNumber() {
		String[] numbers = {
			"1", "7", "7.1", "8.0", "8.1", "8.1.1.0", "9.0", "9.0.1.2", "9.1.3.4", "9.1.3.6", "9.1.3.9", "10", "10.1"
		};

		TreeSet<DfVersionNumber> s = new TreeSet<DfVersionNumber>();
		for (String str : numbers) {
			s.add(new DfVersionNumber(str));
		}

		Assert.assertEquals(numbers.length, s.size());
		int i = 0;
		for (DfVersionNumber a : s) {
			DfVersionNumber e = new DfVersionNumber(numbers[i]);
			Assert.assertEquals(e, a);
			i++;
		}

		s.clear();
		for (i = 0; i < 10; i++) {
			s.add(new DfVersionNumber("1.0"));
		}
		Assert.assertEquals(1, s.size());

		DfVersionNumber a = null;
		DfVersionNumber b = null;

		a = new DfVersionNumber("1.0");
		b = new DfVersionNumber("2.0");
		Assert.assertNotEquals(a, b);
		Assert.assertEquals(1, b.compareTo(a));
		Assert.assertEquals(-1, a.compareTo(b));

		b = new DfVersionNumber("1.0.1.0");
		Assert.assertNotEquals(a, b);
		Assert.assertEquals(1, b.compareTo(a));
		Assert.assertEquals(-1, a.compareTo(b));

		b = new DfVersionNumber("0.9");
		Assert.assertNotEquals(a, b);
		Assert.assertEquals(-1, b.compareTo(a));
		Assert.assertEquals(1, a.compareTo(b));

		a = new DfVersionNumber("1.0");
		b = new DfVersionNumber("1.1");
		Assert.assertNotEquals(a, b);

		Assert.assertFalse(a.isAntecedentOf(a));
		Assert.assertTrue(a.isAntecedentOf(b));
		Assert.assertFalse(b.isAntecedentOf(a));
		Assert.assertFalse(b.isAntecedentOf(b));

		Assert.assertFalse(a.isSuccessorOf(a));
		Assert.assertFalse(a.isSuccessorOf(b));
		Assert.assertTrue(b.isSuccessorOf(a));
		Assert.assertFalse(b.isSuccessorOf(b));

		Assert.assertFalse(a.isAncestorOf(a));
		Assert.assertFalse(a.isAncestorOf(b));
		Assert.assertFalse(b.isAncestorOf(b));
		Assert.assertFalse(b.isAncestorOf(a));

		Assert.assertFalse(a.isDescendantOf(a));
		Assert.assertFalse(a.isDescendantOf(b));
		Assert.assertFalse(b.isDescendantOf(b));
		Assert.assertFalse(b.isDescendantOf(a));

		a = new DfVersionNumber("1.0");
		b = new DfVersionNumber("3.1");
		Assert.assertNotEquals(a, b);

		Assert.assertTrue(a.isAntecedentOf(b));
		Assert.assertFalse(b.isAntecedentOf(a));

		Assert.assertFalse(a.isSuccessorOf(b));
		Assert.assertTrue(b.isSuccessorOf(a));

		Assert.assertFalse(a.isAncestorOf(b));
		Assert.assertFalse(b.isAncestorOf(a));

		Assert.assertFalse(a.isDescendantOf(b));
		Assert.assertFalse(b.isDescendantOf(a));

		a = new DfVersionNumber("9.8.7.6.5.4.3.2.1.0");
		b = new DfVersionNumber("9.8.7.6.5.4.3.2.1.1");
		Assert.assertEquals(a.getComponentCount() - 1, a.getDepthInCommon(b));
		Assert.assertEquals(b.getComponentCount() - 1, b.getDepthInCommon(a));
		Assert.assertEquals(a.getComponentCount(), a.getDepthInCommon(a));
		Assert.assertEquals(b.getComponentCount(), b.getDepthInCommon(b));
		Assert.assertNotEquals(a, b);

		Assert.assertTrue(a.isAntecedentOf(b));
		Assert.assertFalse(b.isAntecedentOf(a));

		Assert.assertFalse(a.isSuccessorOf(b));
		Assert.assertTrue(b.isSuccessorOf(a));

		Assert.assertFalse(a.isAncestorOf(b));
		Assert.assertFalse(b.isAncestorOf(a));

		Assert.assertFalse(a.isDescendantOf(b));
		Assert.assertFalse(b.isDescendantOf(a));

		a = new DfVersionNumber("1.0.1.2.3.4.5.6.7.8");
		b = new DfVersionNumber("1.0.1.2.3.4.5.6.7.8.5.1");
		Assert.assertEquals(Math.min(a.getComponentCount(), b.getComponentCount()), a.getDepthInCommon(b));
		Assert.assertEquals(Math.min(a.getComponentCount(), b.getComponentCount()), b.getDepthInCommon(a));
		Assert.assertEquals(a.getComponentCount(), a.getDepthInCommon(a));
		Assert.assertEquals(b.getComponentCount(), b.getDepthInCommon(b));
		Assert.assertNotEquals(a, b);

		a = new DfVersionNumber("1.0");
		b = new DfVersionNumber("1.0.5.1");
		Assert.assertEquals(Math.min(a.getComponentCount(), b.getComponentCount()), a.getDepthInCommon(b));
		Assert.assertEquals(Math.min(a.getComponentCount(), b.getComponentCount()), b.getDepthInCommon(a));
		Assert.assertEquals(a.getComponentCount(), a.getDepthInCommon(a));
		Assert.assertEquals(b.getComponentCount(), b.getDepthInCommon(b));
		Assert.assertNotEquals(a, b);

		Assert.assertFalse(a.isAntecedentOf(b));
		Assert.assertFalse(b.isAntecedentOf(a));

		Assert.assertFalse(a.isSuccessorOf(b));
		Assert.assertFalse(b.isSuccessorOf(a));

		Assert.assertTrue(a.isAncestorOf(b));
		Assert.assertFalse(b.isAncestorOf(a));

		Assert.assertFalse(a.isDescendantOf(b));
		Assert.assertTrue(b.isDescendantOf(a));

		a = new DfVersionNumber("1.0.5.1");
		b = new DfVersionNumber("1.0.5.2");
		Assert.assertNotEquals(a, b);

		Assert.assertTrue(a.isAntecedentOf(b));
		Assert.assertFalse(b.isAntecedentOf(a));

		Assert.assertFalse(a.isSuccessorOf(b));
		Assert.assertTrue(b.isSuccessorOf(a));

		Assert.assertFalse(a.isAncestorOf(b));
		Assert.assertFalse(b.isAncestorOf(a));

		Assert.assertFalse(a.isDescendantOf(b));
		Assert.assertFalse(b.isDescendantOf(a));

		a = new DfVersionNumber("1.0.1.2.3.4.5.6.5.1");
		b = new DfVersionNumber("1.0.1.2.3.4.5.6.5.2");
		Assert.assertNotEquals(a, b);

		Assert.assertTrue(a.isAntecedentOf(b));
		Assert.assertFalse(b.isAntecedentOf(a));

		Assert.assertFalse(a.isSuccessorOf(b));
		Assert.assertTrue(b.isSuccessorOf(a));

		Assert.assertFalse(a.isAncestorOf(b));
		Assert.assertFalse(b.isAncestorOf(a));

		Assert.assertFalse(a.isDescendantOf(b));
		Assert.assertFalse(b.isDescendantOf(a));

		a = new DfVersionNumber("1.0.1.2.3.4.5.6.5.1");
		b = new DfVersionNumber("1.0.2.2.3.4.5.6.5.2");
		Assert.assertNotEquals(a, b);

		Assert.assertFalse(a.isAntecedentOf(b));
		Assert.assertFalse(b.isAntecedentOf(a));

		Assert.assertFalse(a.isSuccessorOf(b));
		Assert.assertFalse(b.isSuccessorOf(a));

		Assert.assertFalse(a.isAncestorOf(b));
		Assert.assertFalse(b.isAncestorOf(a));

		Assert.assertFalse(a.isDescendantOf(b));
		Assert.assertFalse(b.isDescendantOf(a));

		a = new DfVersionNumber("1.0.1.2.3.4.5.6.5.1");
		for (i = 1; i < a.getComponentCount(); i++) {
			b = a.getSubset(i);
			Assert.assertNotEquals(b.toString(), a, b);
			Assert.assertNotEquals(b.toString(), a.hashCode(), b.hashCode());
			Assert.assertTrue(b.toString(), a.equals(b, i));
			Assert.assertFalse(b.toString(), a.equals(b, 0));

			Assert.assertFalse(b.toString(), a.isAntecedentOf(b));
			Assert.assertFalse(b.toString(), b.isAntecedentOf(a));

			Assert.assertFalse(b.toString(), a.isSuccessorOf(b));
			Assert.assertFalse(b.toString(), b.isSuccessorOf(a));

			Assert.assertFalse(b.toString(), a.isAncestorOf(b));
			Assert.assertTrue(b.toString(), b.isAncestorOf(a));

			Assert.assertTrue(b.toString(), a.isDescendantOf(b));
			Assert.assertFalse(b.toString(), b.isDescendantOf(a));
		}

		Assert.assertFalse(a.equals(null));
		Assert.assertFalse(a.equals(this));

		Assert.assertTrue(a.compareTo(null) > 0);

		try {
			a.isAncestorOf(null);
			Assert.fail("Did not fail with a null parameter");
		} catch (IllegalArgumentException e) {
			// All is well
		}

		try {
			a.isDescendantOf(null);
			Assert.fail("Did not fail with a null parameter");
		} catch (IllegalArgumentException e) {
			// All is well
		}

		try {
			a.isAntecedentOf(null);
			Assert.fail("Did not fail with a null parameter");
		} catch (IllegalArgumentException e) {
			// All is well
		}

		try {
			a.isSuccessorOf(null);
			Assert.fail("Did not fail with a null parameter");
		} catch (IllegalArgumentException e) {
			// All is well
		}

		try {
			a.getDepthInCommon(null);
			Assert.fail("Did not fail with a null parameter");
		} catch (IllegalArgumentException e) {
			// All is well
		}

		try {
			a.equals(a, -1);
			Assert.fail("Did not fail with a negative length parameter");
		} catch (IllegalArgumentException e) {
			// All is well
		}
		Assert.assertTrue(a.equals(a, 0));

		b = a.getSubset(3);
		Assert.assertNotSame(a, b);
		Assert.assertTrue(b.equals(a, b.getComponentCount()));
		Assert.assertFalse(b.equals(a, 0));
		Assert.assertSame(a, a.getSubset(Integer.MAX_VALUE));
		try {
			a.getSubset(-1);
			Assert.fail("Did not fail with a negative length parameter");
		} catch (IllegalArgumentException e) {
			// All is well
		}

		try {
			a.toString(-1);
			Assert.fail("Did not fail with a negative length parameter");
		} catch (IllegalArgumentException e) {
			// All is well
		}

		try {
			a.getSubset(0);
			Assert.fail("Did not fail with a 0 parameter");
		} catch (IllegalArgumentException e) {
			// All is well
		}

		Assert.assertEquals(a.toString(), a.toString(a.getComponentCount()));
		Assert.assertNotEquals(a.toString(), a.toString(a.getComponentCount() + 1));

		b = new DfVersionNumber(a.toString());
		Assert.assertEquals(a, b);
		Assert.assertNotSame(a, b);
		Assert.assertEquals(a.hashCode(), b.hashCode());
	}
}