package com.armedia.caliente.cli;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

public class OptionImplTest {

	@Test
	public void testConstructor() {
		OptionImpl expected = null;
		OptionImpl actual = null;

		expected = new OptionImpl();
		Assert.assertNotNull(expected);
		expected.setArgumentName(UUID.randomUUID().toString());
		expected.setDescription(UUID.randomUUID().toString());
		expected.setLongOpt(UUID.randomUUID().toString());
		expected.setShortOpt('x');
		expected.setMaxArguments(UUID.randomUUID().hashCode());
		expected.setRequired(true);
		expected.setMinArguments(0);
		expected.setValueSep('|');

		actual = new OptionImpl(expected);
		Assert.assertNotNull(actual);
		Assert.assertEquals("ValueName", expected.getArgumentName(), actual.getArgumentName());
		Assert.assertEquals("Description", expected.getDescription(), actual.getDescription());
		Assert.assertEquals("LongOpt", expected.getLongOpt(), actual.getLongOpt());
		Assert.assertEquals("ShortOpt", expected.getShortOpt(), actual.getShortOpt());
		Assert.assertEquals("MaxValueCount", expected.getMaxArguments(), actual.getMaxArguments());
		Assert.assertEquals("Required", expected.isRequired(), actual.isRequired());
		Assert.assertEquals("MinValueCount", expected.getMinArguments(), actual.getMinArguments());
		Assert.assertEquals("ValueSep", expected.getValueSep(), actual.getValueSep());
		Assert.assertTrue("equivalence test", Option.isIdentical(expected, actual));
		Assert.assertTrue("inverse equivalence test", Option.isIdentical(actual, expected));
		Assert.assertEquals("key", expected.getKey(), actual.getKey());

		expected = new OptionImpl();
		Assert.assertNotNull(expected);
		expected.setArgumentName(UUID.randomUUID().toString());
		expected.setDescription(UUID.randomUUID().toString());
		expected.setLongOpt(UUID.randomUUID().toString());
		expected.setShortOpt('P');
		expected.setMaxArguments(UUID.randomUUID().hashCode());
		expected.setRequired(false);
		expected.setMinArguments(1);
		expected.setValueSep('$');

		actual = new OptionImpl(expected);
		Assert.assertNotNull(actual);
		Assert.assertEquals("ValueName", expected.getArgumentName(), actual.getArgumentName());
		Assert.assertEquals("Description", expected.getDescription(), actual.getDescription());
		Assert.assertEquals("LongOpt", expected.getLongOpt(), actual.getLongOpt());
		Assert.assertEquals("ShortOpt", expected.getShortOpt(), actual.getShortOpt());
		Assert.assertEquals("MaxValueCount", expected.getMaxArguments(), actual.getMaxArguments());
		Assert.assertEquals("Required", expected.isRequired(), actual.isRequired());
		Assert.assertEquals("MinValueCount", expected.getMinArguments(), actual.getMinArguments());
		Assert.assertEquals("ValueSep", expected.getValueSep(), actual.getValueSep());
		Assert.assertTrue("equivalence test", Option.isIdentical(expected, actual));
		Assert.assertTrue("inverse equivalence test", Option.isIdentical(actual, expected));
		Assert.assertEquals("key", expected.getKey(), actual.getKey());

		Assert.assertNotNull(expected.toString());
	}

	@Test
	public void testEquals() {
		OptionImpl def = null;

		def = new OptionImpl();
		Assert.assertNotNull(def);
		def.setArgumentName(UUID.randomUUID().toString());
		def.setDescription(UUID.randomUUID().toString());
		def.setLongOpt(UUID.randomUUID().toString());
		def.setShortOpt('x');
		def.setMaxArguments(UUID.randomUUID().hashCode());
		def.setRequired(true);
		def.setMinArguments(0);
		def.setValueSep('|');

		Assert.assertNotEquals(def, null);
		Assert.assertNotEquals(def, new Object());
		Assert.assertNotEquals(def, "");
		Assert.assertEquals(def, def);
	}

	@Test
	public void testClone() {
		OptionImpl expected = null;
		OptionImpl actual = null;

		expected = new OptionImpl();
		Assert.assertNotNull(expected);
		expected.setArgumentName(UUID.randomUUID().toString());
		expected.setDescription(UUID.randomUUID().toString());
		expected.setLongOpt(UUID.randomUUID().toString());
		expected.setShortOpt('x');
		expected.setMaxArguments(UUID.randomUUID().hashCode());
		expected.setRequired(true);
		expected.setMinArguments(0);
		expected.setValueSep('|');

		actual = expected.clone();
		Assert.assertNotNull(actual);
		Assert.assertEquals("ValueName", expected.getArgumentName(), actual.getArgumentName());
		Assert.assertEquals("Description", expected.getDescription(), actual.getDescription());
		Assert.assertEquals("LongOpt", expected.getLongOpt(), actual.getLongOpt());
		Assert.assertEquals("ShortOpt", expected.getShortOpt(), actual.getShortOpt());
		Assert.assertEquals("MaxValueCount", expected.getMaxArguments(), actual.getMaxArguments());
		Assert.assertEquals("Required", expected.isRequired(), actual.isRequired());
		Assert.assertEquals("MinValueCount", expected.getMinArguments(), actual.getMinArguments());
		Assert.assertEquals("ValueSep", expected.getValueSep(), actual.getValueSep());
		Assert.assertTrue("equivalence test", Option.isIdentical(expected, actual));
		Assert.assertTrue("inverse equivalence test", Option.isIdentical(actual, expected));
		Assert.assertEquals("key", expected.getKey(), actual.getKey());
	}

	@Test
	public void testSetValueCount() {
		OptionImpl expected = null;
		OptionImpl actual = null;

		expected = new OptionImpl();
		Assert.assertNotNull(expected);
		expected.setArgumentName(UUID.randomUUID().toString());
		expected.setDescription(UUID.randomUUID().toString());
		expected.setLongOpt(UUID.randomUUID().toString());
		expected.setShortOpt('x');
		expected.setMaxArguments(0);
		expected.setRequired(true);
		expected.setMinArguments(0);
		expected.setValueSep('|');

		actual = expected.clone();
		Assert.assertNotNull(actual);
		Assert.assertEquals("ValueName", expected.getArgumentName(), actual.getArgumentName());
		Assert.assertEquals("Description", expected.getDescription(), actual.getDescription());
		Assert.assertEquals("LongOpt", expected.getLongOpt(), actual.getLongOpt());
		Assert.assertEquals("ShortOpt", expected.getShortOpt(), actual.getShortOpt());
		Assert.assertEquals("MaxValueCount", expected.getMaxArguments(), actual.getMaxArguments());
		Assert.assertEquals("Required", expected.isRequired(), actual.isRequired());
		Assert.assertEquals("MinValueCount", expected.getMinArguments(), actual.getMinArguments());
		Assert.assertEquals("ValueSep", expected.getValueSep(), actual.getValueSep());
		Assert.assertTrue("equivalence test", Option.isIdentical(expected, actual));
		Assert.assertTrue("inverse equivalence test", Option.isIdentical(actual, expected));
		Assert.assertEquals("key", expected.getKey(), actual.getKey());

		for (int i = 1; i < 100; i++) {
			actual.setMaxArguments(i);
			Assert.assertEquals(i, actual.getMaxArguments());
			Assert.assertEquals("ValueName", expected.getArgumentName(), actual.getArgumentName());
			Assert.assertEquals("Description", expected.getDescription(), actual.getDescription());
			Assert.assertEquals("LongOpt", expected.getLongOpt(), actual.getLongOpt());
			Assert.assertEquals("ShortOpt", expected.getShortOpt(), actual.getShortOpt());
			Assert.assertNotEquals("MaxValueCount", expected.getMaxArguments(), actual.getMaxArguments());
			Assert.assertEquals("Required", expected.isRequired(), actual.isRequired());
			Assert.assertEquals("MinValueCount", expected.getMinArguments(), actual.getMinArguments());
			Assert.assertEquals("ValueSep", expected.getValueSep(), actual.getValueSep());
			Assert.assertFalse("equivalence test", Option.isIdentical(expected, actual));
			Assert.assertFalse("inverse equivalence test", Option.isIdentical(actual, expected));
			Assert.assertNotEquals(expected.hashCode(), actual.hashCode());
		}
	}

	@Test
	public void testSetValueName() {
		OptionImpl expected = null;
		OptionImpl actual = null;

		expected = new OptionImpl();
		Assert.assertNotNull(expected);
		expected.setArgumentName("test-value-name");
		expected.setDescription(UUID.randomUUID().toString());
		expected.setLongOpt(UUID.randomUUID().toString());
		expected.setShortOpt('x');
		expected.setMaxArguments(UUID.randomUUID().hashCode());
		expected.setRequired(true);
		expected.setMinArguments(0);
		expected.setValueSep('|');

		actual = expected.clone();
		Assert.assertNotNull(actual);
		Assert.assertEquals("ValueName", expected.getArgumentName(), actual.getArgumentName());
		Assert.assertEquals("Description", expected.getDescription(), actual.getDescription());
		Assert.assertEquals("LongOpt", expected.getLongOpt(), actual.getLongOpt());
		Assert.assertEquals("ShortOpt", expected.getShortOpt(), actual.getShortOpt());
		Assert.assertEquals("MaxValueCount", expected.getMaxArguments(), actual.getMaxArguments());
		Assert.assertEquals("Required", expected.isRequired(), actual.isRequired());
		Assert.assertEquals("MinValueCount", expected.getMinArguments(), actual.getMinArguments());
		Assert.assertEquals("ValueSep", expected.getValueSep(), actual.getValueSep());
		Assert.assertTrue("equivalence test", Option.isIdentical(expected, actual));
		Assert.assertTrue("inverse equivalence test", Option.isIdentical(actual, expected));
		Assert.assertEquals("key", expected.getKey(), actual.getKey());

		for (int i = 1; i < 10; i++) {
			String v = UUID.randomUUID().toString();
			actual.setArgumentName(v);
			Assert.assertEquals(v, actual.getArgumentName());
			Assert.assertNotEquals("ValueName", expected.getArgumentName(), actual.getArgumentName());
			Assert.assertEquals("Description", expected.getDescription(), actual.getDescription());
			Assert.assertEquals("LongOpt", expected.getLongOpt(), actual.getLongOpt());
			Assert.assertEquals("ShortOpt", expected.getShortOpt(), actual.getShortOpt());
			Assert.assertEquals("MaxValueCount", expected.getMaxArguments(), actual.getMaxArguments());
			Assert.assertEquals("Required", expected.isRequired(), actual.isRequired());
			Assert.assertEquals("MinValueCount", expected.getMinArguments(), actual.getMinArguments());
			Assert.assertEquals("ValueSep", expected.getValueSep(), actual.getValueSep());
			Assert.assertFalse("equivalence test", Option.isIdentical(expected, actual));
			Assert.assertFalse("inverse equivalence test", Option.isIdentical(actual, expected));
			Assert.assertNotEquals(expected.hashCode(), actual.hashCode());
		}
	}

	@Test
	public void testSetDescription() {
		OptionImpl expected = null;
		OptionImpl actual = null;

		expected = new OptionImpl();
		Assert.assertNotNull(expected);
		expected.setArgumentName(UUID.randomUUID().toString());
		expected.setDescription("test-description");
		expected.setLongOpt(UUID.randomUUID().toString());
		expected.setShortOpt('x');
		expected.setMaxArguments(UUID.randomUUID().hashCode());
		expected.setRequired(true);
		expected.setMinArguments(0);
		expected.setValueSep('|');

		actual = expected.clone();
		Assert.assertNotNull(actual);
		Assert.assertEquals("ValueName", expected.getArgumentName(), actual.getArgumentName());
		Assert.assertEquals("Description", expected.getDescription(), actual.getDescription());
		Assert.assertEquals("LongOpt", expected.getLongOpt(), actual.getLongOpt());
		Assert.assertEquals("ShortOpt", expected.getShortOpt(), actual.getShortOpt());
		Assert.assertEquals("MaxValueCount", expected.getMaxArguments(), actual.getMaxArguments());
		Assert.assertEquals("Required", expected.isRequired(), actual.isRequired());
		Assert.assertEquals("MinValueCount", expected.getMinArguments(), actual.getMinArguments());
		Assert.assertEquals("ValueSep", expected.getValueSep(), actual.getValueSep());
		Assert.assertTrue("equivalence test", Option.isIdentical(expected, actual));
		Assert.assertTrue("inverse equivalence test", Option.isIdentical(actual, expected));
		Assert.assertEquals("key", expected.getKey(), actual.getKey());

		for (int i = 1; i < 10; i++) {
			String v = UUID.randomUUID().toString();
			actual.setDescription(v);
			Assert.assertEquals(v, actual.getDescription());
			Assert.assertEquals("ValueName", expected.getArgumentName(), actual.getArgumentName());
			Assert.assertNotEquals("Description", expected.getDescription(), actual.getDescription());
			Assert.assertEquals("LongOpt", expected.getLongOpt(), actual.getLongOpt());
			Assert.assertEquals("ShortOpt", expected.getShortOpt(), actual.getShortOpt());
			Assert.assertEquals("MaxValueCount", expected.getMaxArguments(), actual.getMaxArguments());
			Assert.assertEquals("Required", expected.isRequired(), actual.isRequired());
			Assert.assertEquals("MinValueCount", expected.getMinArguments(), actual.getMinArguments());
			Assert.assertEquals("ValueSep", expected.getValueSep(), actual.getValueSep());
			Assert.assertFalse("equivalence test", Option.isIdentical(expected, actual));
			Assert.assertFalse("inverse equivalence test", Option.isIdentical(actual, expected));
			Assert.assertNotEquals(expected.hashCode(), actual.hashCode());
		}
	}

	@Test
	public void testSetLongOpt() {
		OptionImpl expected = null;
		OptionImpl actual = null;

		expected = new OptionImpl();
		Assert.assertNotNull(expected);
		expected.setArgumentName(UUID.randomUUID().toString());
		expected.setDescription(UUID.randomUUID().toString());
		expected.setLongOpt("test-description");
		expected.setShortOpt('x');
		expected.setMaxArguments(UUID.randomUUID().hashCode());
		expected.setRequired(true);
		expected.setMinArguments(0);
		expected.setValueSep('|');

		actual = expected.clone();
		Assert.assertNotNull(actual);
		Assert.assertEquals("ValueName", expected.getArgumentName(), actual.getArgumentName());
		Assert.assertEquals("Description", expected.getDescription(), actual.getDescription());
		Assert.assertEquals("LongOpt", expected.getLongOpt(), actual.getLongOpt());
		Assert.assertEquals("ShortOpt", expected.getShortOpt(), actual.getShortOpt());
		Assert.assertEquals("MaxValueCount", expected.getMaxArguments(), actual.getMaxArguments());
		Assert.assertEquals("Required", expected.isRequired(), actual.isRequired());
		Assert.assertEquals("MinValueCount", expected.getMinArguments(), actual.getMinArguments());
		Assert.assertEquals("ValueSep", expected.getValueSep(), actual.getValueSep());
		Assert.assertTrue("equivalence test", Option.isIdentical(expected, actual));
		Assert.assertTrue("inverse equivalence test", Option.isIdentical(actual, expected));
		Assert.assertEquals("key", expected.getKey(), actual.getKey());

		for (int i = 1; i < 10; i++) {
			String v = UUID.randomUUID().toString();
			actual.setLongOpt(v);
			Assert.assertEquals(v, actual.getLongOpt());
			Assert.assertEquals("ValueName", expected.getArgumentName(), actual.getArgumentName());
			Assert.assertEquals("Description", expected.getDescription(), actual.getDescription());
			Assert.assertNotEquals("LongOpt", expected.getLongOpt(), actual.getLongOpt());
			Assert.assertEquals("ShortOpt", expected.getShortOpt(), actual.getShortOpt());
			Assert.assertEquals("MaxValueCount", expected.getMaxArguments(), actual.getMaxArguments());
			Assert.assertEquals("Required", expected.isRequired(), actual.isRequired());
			Assert.assertEquals("MinValueCount", expected.getMinArguments(), actual.getMinArguments());
			Assert.assertEquals("ValueSep", expected.getValueSep(), actual.getValueSep());
			Assert.assertFalse("equivalence test", Option.isIdentical(expected, actual));
			Assert.assertFalse("inverse equivalence test", Option.isIdentical(actual, expected));
			Assert.assertNotEquals(expected.hashCode(), actual.hashCode());
		}
	}

	@Test
	public void testSetShortOpt() {
		OptionImpl expected = null;
		OptionImpl actual = null;

		expected = new OptionImpl();
		Assert.assertNotNull(expected);
		expected.setArgumentName(UUID.randomUUID().toString());
		expected.setDescription(UUID.randomUUID().toString());
		expected.setLongOpt(UUID.randomUUID().toString());
		expected.setShortOpt('G');
		expected.setMaxArguments(UUID.randomUUID().hashCode());
		expected.setRequired(true);
		expected.setMinArguments(0);
		expected.setValueSep('|');

		actual = expected.clone();
		Assert.assertNotNull(actual);
		Assert.assertEquals("ValueName", expected.getArgumentName(), actual.getArgumentName());
		Assert.assertEquals("Description", expected.getDescription(), actual.getDescription());
		Assert.assertEquals("LongOpt", expected.getLongOpt(), actual.getLongOpt());
		Assert.assertEquals("ShortOpt", expected.getShortOpt(), actual.getShortOpt());
		Assert.assertEquals("MaxValueCount", expected.getMaxArguments(), actual.getMaxArguments());
		Assert.assertEquals("Required", expected.isRequired(), actual.isRequired());
		Assert.assertEquals("MinValueCount", expected.getMinArguments(), actual.getMinArguments());
		Assert.assertEquals("ValueSep", expected.getValueSep(), actual.getValueSep());
		Assert.assertTrue("equivalence test", Option.isIdentical(expected, actual));
		Assert.assertTrue("inverse equivalence test", Option.isIdentical(actual, expected));
		Assert.assertEquals("key", expected.getKey(), actual.getKey());

		for (char c : UUID.randomUUID().toString().toCharArray()) {
			if (c == '-') {
				continue;
			}
			actual.setShortOpt(c);
			Assert.assertEquals(c, actual.getShortOpt().charValue());
			Assert.assertEquals("ValueName", expected.getArgumentName(), actual.getArgumentName());
			Assert.assertEquals("Description", expected.getDescription(), actual.getDescription());
			Assert.assertEquals("LongOpt", expected.getLongOpt(), actual.getLongOpt());
			Assert.assertNotEquals("ShortOpt", expected.getShortOpt(), actual.getShortOpt());
			Assert.assertEquals("MaxValueCount", expected.getMaxArguments(), actual.getMaxArguments());
			Assert.assertEquals("Required", expected.isRequired(), actual.isRequired());
			Assert.assertEquals("MinValueCount", expected.getMinArguments(), actual.getMinArguments());
			Assert.assertEquals("ValueSep", expected.getValueSep(), actual.getValueSep());
			Assert.assertFalse("equivalence test", Option.isIdentical(expected, actual));
			Assert.assertFalse("inverse equivalence test", Option.isIdentical(actual, expected));
			Assert.assertNotEquals(expected.hashCode(), actual.hashCode());
		}
	}

	@Test
	public void testSetValueSep() {
		OptionImpl expected = null;
		OptionImpl actual = null;

		expected = new OptionImpl();
		Assert.assertNotNull(expected);
		expected.setArgumentName(UUID.randomUUID().toString());
		expected.setDescription(UUID.randomUUID().toString());
		expected.setLongOpt(UUID.randomUUID().toString());
		expected.setShortOpt('G');
		expected.setMaxArguments(UUID.randomUUID().hashCode());
		expected.setRequired(true);
		expected.setMinArguments(0);
		expected.setValueSep('|');

		actual = expected.clone();
		Assert.assertNotNull(actual);
		Assert.assertEquals("ValueName", expected.getArgumentName(), actual.getArgumentName());
		Assert.assertEquals("Description", expected.getDescription(), actual.getDescription());
		Assert.assertEquals("LongOpt", expected.getLongOpt(), actual.getLongOpt());
		Assert.assertEquals("ShortOpt", expected.getShortOpt(), actual.getShortOpt());
		Assert.assertEquals("MaxValueCount", expected.getMaxArguments(), actual.getMaxArguments());
		Assert.assertEquals("Required", expected.isRequired(), actual.isRequired());
		Assert.assertEquals("MinValueCount", expected.getMinArguments(), actual.getMinArguments());
		Assert.assertEquals("ValueSep", expected.getValueSep(), actual.getValueSep());
		Assert.assertTrue("equivalence test", Option.isIdentical(expected, actual));
		Assert.assertTrue("inverse equivalence test", Option.isIdentical(actual, expected));
		Assert.assertEquals("key", expected.getKey(), actual.getKey());

		for (char c : UUID.randomUUID().toString().toCharArray()) {
			actual.setValueSep(c);
			Assert.assertEquals(c, actual.getValueSep().charValue());
			Assert.assertEquals("ValueName", expected.getArgumentName(), actual.getArgumentName());
			Assert.assertEquals("Description", expected.getDescription(), actual.getDescription());
			Assert.assertEquals("LongOpt", expected.getLongOpt(), actual.getLongOpt());
			Assert.assertEquals("ShortOpt", expected.getShortOpt(), actual.getShortOpt());
			Assert.assertEquals("MaxValueCount", expected.getMaxArguments(), actual.getMaxArguments());
			Assert.assertEquals("Required", expected.isRequired(), actual.isRequired());
			Assert.assertEquals("MinValueCount", expected.getMinArguments(), actual.getMinArguments());
			Assert.assertNotEquals("ValueSep", expected.getValueSep(), actual.getValueSep());
			Assert.assertFalse("equivalence test", Option.isIdentical(expected, actual));
			Assert.assertFalse("inverse equivalence test", Option.isIdentical(actual, expected));
			Assert.assertNotEquals(expected.hashCode(), actual.hashCode());
		}
	}

	@Test
	public void testSetValueOptional() {
		OptionImpl expected = null;
		OptionImpl actual = null;

		expected = new OptionImpl();
		Assert.assertNotNull(expected);
		expected.setArgumentName(UUID.randomUUID().toString());
		expected.setDescription(UUID.randomUUID().toString());
		expected.setLongOpt(UUID.randomUUID().toString());
		expected.setShortOpt('G');
		expected.setMaxArguments(UUID.randomUUID().hashCode());
		expected.setRequired(true);
		expected.setMinArguments(0);
		expected.setValueSep('|');

		actual = expected.clone();
		Assert.assertNotNull(actual);
		Assert.assertEquals("ValueName", expected.getArgumentName(), actual.getArgumentName());
		Assert.assertEquals("Description", expected.getDescription(), actual.getDescription());
		Assert.assertEquals("LongOpt", expected.getLongOpt(), actual.getLongOpt());
		Assert.assertEquals("ShortOpt", expected.getShortOpt(), actual.getShortOpt());
		Assert.assertEquals("MaxValueCount", expected.getMaxArguments(), actual.getMaxArguments());
		Assert.assertEquals("Required", expected.isRequired(), actual.isRequired());
		Assert.assertEquals("MinValueCount", expected.getMinArguments(), actual.getMinArguments());
		Assert.assertEquals("ValueSep", expected.getValueSep(), actual.getValueSep());
		Assert.assertTrue("equivalence test", Option.isIdentical(expected, actual));
		Assert.assertTrue("inverse equivalence test", Option.isIdentical(actual, expected));
		Assert.assertEquals("key", expected.getKey(), actual.getKey());

		actual.setMinArguments(1);
		Assert.assertEquals(1, actual.getMinArguments());
		Assert.assertEquals("ValueName", expected.getArgumentName(), actual.getArgumentName());
		Assert.assertEquals("Description", expected.getDescription(), actual.getDescription());
		Assert.assertEquals("LongOpt", expected.getLongOpt(), actual.getLongOpt());
		Assert.assertEquals("ShortOpt", expected.getShortOpt(), actual.getShortOpt());
		Assert.assertEquals("MaxValueCount", expected.getMaxArguments(), actual.getMaxArguments());
		Assert.assertEquals("Required", expected.isRequired(), actual.isRequired());
		Assert.assertEquals("ValueSep", expected.getValueSep(), actual.getValueSep());
		Assert.assertFalse("equivalence test", Option.isIdentical(expected, actual));
		Assert.assertFalse("inverse equivalence test", Option.isIdentical(actual, expected));
		Assert.assertNotEquals(expected.hashCode(), actual.hashCode());

		actual.setMinArguments(0);
		Assert.assertEquals(0, actual.getMinArguments());
		Assert.assertEquals("ValueName", expected.getArgumentName(), actual.getArgumentName());
		Assert.assertEquals("Description", expected.getDescription(), actual.getDescription());
		Assert.assertEquals("LongOpt", expected.getLongOpt(), actual.getLongOpt());
		Assert.assertEquals("ShortOpt", expected.getShortOpt(), actual.getShortOpt());
		Assert.assertEquals("MaxValueCount", expected.getMaxArguments(), actual.getMaxArguments());
		Assert.assertEquals("Required", expected.isRequired(), actual.isRequired());
		Assert.assertEquals("ValueSep", expected.getValueSep(), actual.getValueSep());
		Assert.assertTrue("equivalence test", Option.isIdentical(expected, actual));
		Assert.assertTrue("inverse equivalence test", Option.isIdentical(actual, expected));
		Assert.assertEquals("key", expected.getKey(), actual.getKey());
	}

	@Test
	public void testSetRequired() {
		OptionImpl expected = null;
		OptionImpl actual = null;

		expected = new OptionImpl();
		Assert.assertNotNull(expected);
		expected.setArgumentName(UUID.randomUUID().toString());
		expected.setDescription(UUID.randomUUID().toString());
		expected.setLongOpt(UUID.randomUUID().toString());
		expected.setShortOpt('G');
		expected.setMaxArguments(UUID.randomUUID().hashCode());
		expected.setRequired(true);
		expected.setMinArguments(0);
		expected.setValueSep('|');

		actual = expected.clone();
		Assert.assertNotNull(actual);
		Assert.assertEquals("ValueName", expected.getArgumentName(), actual.getArgumentName());
		Assert.assertEquals("Description", expected.getDescription(), actual.getDescription());
		Assert.assertEquals("LongOpt", expected.getLongOpt(), actual.getLongOpt());
		Assert.assertEquals("ShortOpt", expected.getShortOpt(), actual.getShortOpt());
		Assert.assertEquals("MaxValueCount", expected.getMaxArguments(), actual.getMaxArguments());
		Assert.assertEquals("Required", expected.isRequired(), actual.isRequired());
		Assert.assertEquals("MinValueCount", expected.getMinArguments(), actual.getMinArguments());
		Assert.assertEquals("ValueSep", expected.getValueSep(), actual.getValueSep());
		Assert.assertTrue("equivalence test", Option.isIdentical(expected, actual));
		Assert.assertTrue("inverse equivalence test", Option.isIdentical(actual, expected));
		Assert.assertEquals("key", expected.getKey(), actual.getKey());

		actual.setRequired(false);
		Assert.assertFalse(actual.isRequired());
		Assert.assertEquals("ValueName", expected.getArgumentName(), actual.getArgumentName());
		Assert.assertEquals("Description", expected.getDescription(), actual.getDescription());
		Assert.assertEquals("LongOpt", expected.getLongOpt(), actual.getLongOpt());
		Assert.assertEquals("ShortOpt", expected.getShortOpt(), actual.getShortOpt());
		Assert.assertEquals("MaxValueCount", expected.getMaxArguments(), actual.getMaxArguments());
		Assert.assertEquals("MinValueCount", expected.getMinArguments(), actual.getMinArguments());
		Assert.assertEquals("ValueSep", expected.getValueSep(), actual.getValueSep());
		Assert.assertFalse("equivalence test", Option.isIdentical(expected, actual));
		Assert.assertFalse("inverse equivalence test", Option.isIdentical(actual, expected));
		Assert.assertNotEquals(expected.hashCode(), actual.hashCode());

		actual.setRequired(true);
		Assert.assertTrue(actual.isRequired());
		Assert.assertEquals("ValueName", expected.getArgumentName(), actual.getArgumentName());
		Assert.assertEquals("Description", expected.getDescription(), actual.getDescription());
		Assert.assertEquals("LongOpt", expected.getLongOpt(), actual.getLongOpt());
		Assert.assertEquals("ShortOpt", expected.getShortOpt(), actual.getShortOpt());
		Assert.assertEquals("MaxValueCount", expected.getMaxArguments(), actual.getMaxArguments());
		Assert.assertEquals("MinValueCount", expected.getMinArguments(), actual.getMinArguments());
		Assert.assertEquals("ValueSep", expected.getValueSep(), actual.getValueSep());
		Assert.assertTrue("equivalence test", Option.isIdentical(expected, actual));
		Assert.assertTrue("inverse equivalence test", Option.isIdentical(actual, expected));
		Assert.assertEquals("key", expected.getKey(), actual.getKey());
	}

	@Test
	public void testArgumentLimits() {
		OptionImpl expected = null;

		for (int min = 255; min >= -255; min--) {
			for (int max = 255; max >= -255; max--) {
				expected = new OptionImpl();

				expected.setMinArguments(min);
				if (min <= 0) {
					Assert.assertEquals(String.format("min=%d max=%d", min, max), 0, expected.getMinArguments());
				} else {
					Assert.assertEquals(String.format("min=%d max=%d", min, max), min, expected.getMinArguments());
				}

				expected.setMaxArguments(max);
				if (max < 0) {
					Assert.assertEquals(String.format("min=%d max=%d", min, max), -1, expected.getMaxArguments());
				} else {
					Assert.assertEquals(String.format("min=%d max=%d", min, max), max, expected.getMaxArguments());
				}

				if (max > 0) {
					if (max < min) {
						Assert.assertEquals(String.format("min=%d max=%d", min, max), expected.getMaxArguments(),
							expected.getMinArguments());
					} else {
						Assert.assertEquals(String.format("min=%d max=%d", min, max), Math.max(0, min),
							expected.getMinArguments());
					}
				} else if (max < 0) {
					Assert.assertEquals(String.format("min=%d max=%d", min, max), Math.max(0, min),
						expected.getMinArguments());
				} else {
					Assert.assertEquals(String.format("min=%d max=%d", min, max), 0, expected.getMinArguments());
				}
			}
		}

		for (int min = 255; min >= -255; min--) {
			for (int max = 255; max >= -255; max--) {
				expected = new OptionImpl();

				expected.setMaxArguments(max);
				if (max < 0) {
					Assert.assertEquals(String.format("min=%d max=%d", min, max), -1, expected.getMaxArguments());
				} else {
					Assert.assertEquals(String.format("min=%d max=%d", min, max), max, expected.getMaxArguments());
				}

				expected.setMinArguments(min);
				if (min <= 0) {
					Assert.assertEquals(String.format("min=%d max=%d", min, max), 0, expected.getMinArguments());
				} else {
					Assert.assertEquals(String.format("min=%d max=%d", min, max), min, expected.getMinArguments());
				}

				if (min > 0) {
					if ((min > max) && (max >= 0)) {
						Assert.assertEquals(String.format("min=%d max=%d", min, max), expected.getMinArguments(),
							expected.getMaxArguments());
					} else {
						Assert.assertEquals(String.format("min=%d max=%d", min, max), Math.max(-1, max),
							expected.getMaxArguments());
					}
				} else {
					Assert.assertEquals(String.format("min=%d max=%d", min, max), Math.max(-1, max),
						expected.getMaxArguments());
				}
			}
		}

		for (int min = -255; min <= 255; min++) {
			for (int max = -255; max <= 255; max++) {
				expected = new OptionImpl();

				expected.setMinArguments(min);
				if (min <= 0) {
					Assert.assertEquals(String.format("min=%d max=%d", min, max), 0, expected.getMinArguments());
				} else {
					Assert.assertEquals(String.format("min=%d max=%d", min, max), min, expected.getMinArguments());
				}

				expected.setMaxArguments(max);
				if (max < 0) {
					Assert.assertEquals(String.format("min=%d max=%d", min, max), -1, expected.getMaxArguments());
				} else {
					Assert.assertEquals(String.format("min=%d max=%d", min, max), max, expected.getMaxArguments());
				}

				if (max > 0) {
					if (max < min) {
						Assert.assertEquals(String.format("min=%d max=%d", min, max), expected.getMaxArguments(),
							expected.getMinArguments());
					} else {
						Assert.assertEquals(String.format("min=%d max=%d", min, max), Math.max(0, min),
							expected.getMinArguments());
					}
				} else if (max < 0) {
					Assert.assertEquals(String.format("min=%d max=%d", min, max), Math.max(0, min),
						expected.getMinArguments());
				} else {
					Assert.assertEquals(String.format("min=%d max=%d", min, max), 0, expected.getMinArguments());
				}
			}
		}

		for (int min = -255; min <= 255; min++) {
			for (int max = -255; max <= 255; max++) {
				expected = new OptionImpl();

				expected.setMaxArguments(max);
				if (max < 0) {
					Assert.assertEquals(String.format("min=%d max=%d", min, max), -1, expected.getMaxArguments());
				} else {
					Assert.assertEquals(String.format("min=%d max=%d", min, max), max, expected.getMaxArguments());
				}

				expected.setMinArguments(min);
				if (min <= 0) {
					Assert.assertEquals(String.format("min=%d max=%d", min, max), 0, expected.getMinArguments());
				} else {
					Assert.assertEquals(String.format("min=%d max=%d", min, max), min, expected.getMinArguments());
				}

				if (min > 0) {
					if ((min > max) && (max >= 0)) {
						Assert.assertEquals(String.format("min=%d max=%d", min, max), expected.getMinArguments(),
							expected.getMaxArguments());
					} else {
						Assert.assertEquals(String.format("min=%d max=%d", min, max), Math.max(-1, max),
							expected.getMaxArguments());
					}
				} else {
					Assert.assertEquals(String.format("min=%d max=%d", min, max), Math.max(-1, max),
						expected.getMaxArguments());
				}
			}
		}

		// Reset...
		for (int min = -255; min <= 255; min++) {
			for (int max = -255; max <= 255; max++) {
				expected = new OptionImpl();
				expected.setArgumentLimits(min, max);
				if (min <= 0) {
					Assert.assertEquals(0, expected.getMinArguments());
				} else {
					Assert.assertEquals(min, expected.getMinArguments());
				}
				if (max < 0) {
					Assert.assertEquals(-1, expected.getMaxArguments());
				} else if (min <= max) {
					Assert.assertEquals(max, expected.getMaxArguments());
				} else {
					Assert.assertEquals(expected.getMinArguments(), expected.getMaxArguments());
				}
			}
		}

		// Reset...
		for (int max = -255; max <= 255; max++) {
			for (int min = -255; min <= 255; min++) {
				expected = new OptionImpl();
				expected.setArgumentLimits(min, max);
				if (min <= 0) {
					Assert.assertEquals(0, expected.getMinArguments());
				} else {
					Assert.assertEquals(min, expected.getMinArguments());
				}
				if (max < 0) {
					Assert.assertEquals(-1, expected.getMaxArguments());
				} else if (min <= max) {
					Assert.assertEquals(max, expected.getMaxArguments());
				} else {
					Assert.assertEquals(expected.getMinArguments(), expected.getMaxArguments());
				}
			}
		}

		// Reset...
		for (int l = -255; l <= 255; l++) {
			expected = new OptionImpl();
			expected.setArgumentLimits(l);
			if (l <= 0) {
				Assert.assertEquals(0, expected.getMinArguments());
			} else {
				Assert.assertEquals(l, expected.getMinArguments());
			}
			if (l < 0) {
				Assert.assertEquals(-1, expected.getMaxArguments());
			} else {
				Assert.assertEquals(l, expected.getMaxArguments());
			}
		}
	}
}