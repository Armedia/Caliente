package com.armedia.caliente.cli.parser;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

import com.armedia.caliente.cli.Parameter;
import com.armedia.caliente.cli.ParameterDefinition;

public class MutableParameterDefinitionTest {

	@Test
	public void testConstructor() {
		Parameter expected = null;
		Parameter actual = null;

		expected = new Parameter();
		Assert.assertNotNull(expected);
		expected.setValueName(UUID.randomUUID().toString());
		expected.setDescription(UUID.randomUUID().toString());
		expected.setLongOpt(UUID.randomUUID().toString());
		expected.setShortOpt('x');
		expected.setMaxValueCount(UUID.randomUUID().hashCode());
		expected.setRequired(true);
		expected.setMinValueCount(0);
		expected.setValueSep('|');

		actual = new Parameter(expected);
		Assert.assertNotNull(actual);
		Assert.assertEquals("ValueName", expected.getValueName(), actual.getValueName());
		Assert.assertEquals("Description", expected.getDescription(), actual.getDescription());
		Assert.assertEquals("LongOpt", expected.getLongOpt(), actual.getLongOpt());
		Assert.assertEquals("ShortOpt", expected.getShortOpt(), actual.getShortOpt());
		Assert.assertEquals("MaxValueCount", expected.getMaxValueCount(), actual.getMaxValueCount());
		Assert.assertEquals("Required", expected.isRequired(), actual.isRequired());
		Assert.assertEquals("MinValueCount", expected.getMinValueCount(), actual.getMinValueCount());
		Assert.assertEquals("ValueSep", expected.getValueSep(), actual.getValueSep());
		Assert.assertTrue("equivalence test", ParameterDefinition.isIdentical(expected, actual));
		Assert.assertTrue("inverse equivalence test", ParameterDefinition.isIdentical(actual, expected));
		Assert.assertEquals("key", expected.getKey(), actual.getKey());

		expected = new Parameter();
		Assert.assertNotNull(expected);
		expected.setValueName(UUID.randomUUID().toString());
		expected.setDescription(UUID.randomUUID().toString());
		expected.setLongOpt(UUID.randomUUID().toString());
		expected.setShortOpt('P');
		expected.setMaxValueCount(UUID.randomUUID().hashCode());
		expected.setRequired(false);
		expected.setMinValueCount(1);
		expected.setValueSep('$');

		actual = new Parameter(expected);
		Assert.assertNotNull(actual);
		Assert.assertEquals("ValueName", expected.getValueName(), actual.getValueName());
		Assert.assertEquals("Description", expected.getDescription(), actual.getDescription());
		Assert.assertEquals("LongOpt", expected.getLongOpt(), actual.getLongOpt());
		Assert.assertEquals("ShortOpt", expected.getShortOpt(), actual.getShortOpt());
		Assert.assertEquals("MaxValueCount", expected.getMaxValueCount(), actual.getMaxValueCount());
		Assert.assertEquals("Required", expected.isRequired(), actual.isRequired());
		Assert.assertEquals("MinValueCount", expected.getMinValueCount(), actual.getMinValueCount());
		Assert.assertEquals("ValueSep", expected.getValueSep(), actual.getValueSep());
		Assert.assertTrue("equivalence test", ParameterDefinition.isIdentical(expected, actual));
		Assert.assertTrue("inverse equivalence test", ParameterDefinition.isIdentical(actual, expected));
		Assert.assertEquals("key", expected.getKey(), actual.getKey());

		Assert.assertNotNull(expected.toString());
	}

	@Test
	public void testEquals() {
		Parameter def = null;

		def = new Parameter();
		Assert.assertNotNull(def);
		def.setValueName(UUID.randomUUID().toString());
		def.setDescription(UUID.randomUUID().toString());
		def.setLongOpt(UUID.randomUUID().toString());
		def.setShortOpt('x');
		def.setMaxValueCount(UUID.randomUUID().hashCode());
		def.setRequired(true);
		def.setMinValueCount(0);
		def.setValueSep('|');

		Assert.assertNotEquals(def, null);
		Assert.assertNotEquals(def, new Object());
		Assert.assertNotEquals(def, "");
		Assert.assertEquals(def, def);
	}

	@Test
	public void testClone() {
		Parameter expected = null;
		Parameter actual = null;

		expected = new Parameter();
		Assert.assertNotNull(expected);
		expected.setValueName(UUID.randomUUID().toString());
		expected.setDescription(UUID.randomUUID().toString());
		expected.setLongOpt(UUID.randomUUID().toString());
		expected.setShortOpt('x');
		expected.setMaxValueCount(UUID.randomUUID().hashCode());
		expected.setRequired(true);
		expected.setMinValueCount(0);
		expected.setValueSep('|');

		actual = expected.clone();
		Assert.assertNotNull(actual);
		Assert.assertEquals("ValueName", expected.getValueName(), actual.getValueName());
		Assert.assertEquals("Description", expected.getDescription(), actual.getDescription());
		Assert.assertEquals("LongOpt", expected.getLongOpt(), actual.getLongOpt());
		Assert.assertEquals("ShortOpt", expected.getShortOpt(), actual.getShortOpt());
		Assert.assertEquals("MaxValueCount", expected.getMaxValueCount(), actual.getMaxValueCount());
		Assert.assertEquals("Required", expected.isRequired(), actual.isRequired());
		Assert.assertEquals("MinValueCount", expected.getMinValueCount(), actual.getMinValueCount());
		Assert.assertEquals("ValueSep", expected.getValueSep(), actual.getValueSep());
		Assert.assertTrue("equivalence test", ParameterDefinition.isIdentical(expected, actual));
		Assert.assertTrue("inverse equivalence test", ParameterDefinition.isIdentical(actual, expected));
		Assert.assertEquals("key", expected.getKey(), actual.getKey());
	}

	@Test
	public void testSetValueCount() {
		Parameter expected = null;
		Parameter actual = null;

		expected = new Parameter();
		Assert.assertNotNull(expected);
		expected.setValueName(UUID.randomUUID().toString());
		expected.setDescription(UUID.randomUUID().toString());
		expected.setLongOpt(UUID.randomUUID().toString());
		expected.setShortOpt('x');
		expected.setMaxValueCount(0);
		expected.setRequired(true);
		expected.setMinValueCount(0);
		expected.setValueSep('|');

		actual = expected.clone();
		Assert.assertNotNull(actual);
		Assert.assertEquals("ValueName", expected.getValueName(), actual.getValueName());
		Assert.assertEquals("Description", expected.getDescription(), actual.getDescription());
		Assert.assertEquals("LongOpt", expected.getLongOpt(), actual.getLongOpt());
		Assert.assertEquals("ShortOpt", expected.getShortOpt(), actual.getShortOpt());
		Assert.assertEquals("MaxValueCount", expected.getMaxValueCount(), actual.getMaxValueCount());
		Assert.assertEquals("Required", expected.isRequired(), actual.isRequired());
		Assert.assertEquals("MinValueCount", expected.getMinValueCount(), actual.getMinValueCount());
		Assert.assertEquals("ValueSep", expected.getValueSep(), actual.getValueSep());
		Assert.assertTrue("equivalence test", ParameterDefinition.isIdentical(expected, actual));
		Assert.assertTrue("inverse equivalence test", ParameterDefinition.isIdentical(actual, expected));
		Assert.assertEquals("key", expected.getKey(), actual.getKey());

		for (int i = 1; i < 100; i++) {
			actual.setMaxValueCount(i);
			Assert.assertEquals(i, actual.getMaxValueCount());
			Assert.assertEquals("ValueName", expected.getValueName(), actual.getValueName());
			Assert.assertEquals("Description", expected.getDescription(), actual.getDescription());
			Assert.assertEquals("LongOpt", expected.getLongOpt(), actual.getLongOpt());
			Assert.assertEquals("ShortOpt", expected.getShortOpt(), actual.getShortOpt());
			Assert.assertNotEquals("MaxValueCount", expected.getMaxValueCount(), actual.getMaxValueCount());
			Assert.assertEquals("Required", expected.isRequired(), actual.isRequired());
			Assert.assertEquals("MinValueCount", expected.getMinValueCount(), actual.getMinValueCount());
			Assert.assertEquals("ValueSep", expected.getValueSep(), actual.getValueSep());
			Assert.assertFalse("equivalence test", ParameterDefinition.isIdentical(expected, actual));
			Assert.assertFalse("inverse equivalence test", ParameterDefinition.isIdentical(actual, expected));
			Assert.assertNotEquals(expected.hashCode(), actual.hashCode());
		}
	}

	@Test
	public void testSetValueName() {
		Parameter expected = null;
		Parameter actual = null;

		expected = new Parameter();
		Assert.assertNotNull(expected);
		expected.setValueName("test-value-name");
		expected.setDescription(UUID.randomUUID().toString());
		expected.setLongOpt(UUID.randomUUID().toString());
		expected.setShortOpt('x');
		expected.setMaxValueCount(UUID.randomUUID().hashCode());
		expected.setRequired(true);
		expected.setMinValueCount(0);
		expected.setValueSep('|');

		actual = expected.clone();
		Assert.assertNotNull(actual);
		Assert.assertEquals("ValueName", expected.getValueName(), actual.getValueName());
		Assert.assertEquals("Description", expected.getDescription(), actual.getDescription());
		Assert.assertEquals("LongOpt", expected.getLongOpt(), actual.getLongOpt());
		Assert.assertEquals("ShortOpt", expected.getShortOpt(), actual.getShortOpt());
		Assert.assertEquals("MaxValueCount", expected.getMaxValueCount(), actual.getMaxValueCount());
		Assert.assertEquals("Required", expected.isRequired(), actual.isRequired());
		Assert.assertEquals("MinValueCount", expected.getMinValueCount(), actual.getMinValueCount());
		Assert.assertEquals("ValueSep", expected.getValueSep(), actual.getValueSep());
		Assert.assertTrue("equivalence test", ParameterDefinition.isIdentical(expected, actual));
		Assert.assertTrue("inverse equivalence test", ParameterDefinition.isIdentical(actual, expected));
		Assert.assertEquals("key", expected.getKey(), actual.getKey());

		for (int i = 1; i < 10; i++) {
			String v = UUID.randomUUID().toString();
			actual.setValueName(v);
			Assert.assertEquals(v, actual.getValueName());
			Assert.assertNotEquals("ValueName", expected.getValueName(), actual.getValueName());
			Assert.assertEquals("Description", expected.getDescription(), actual.getDescription());
			Assert.assertEquals("LongOpt", expected.getLongOpt(), actual.getLongOpt());
			Assert.assertEquals("ShortOpt", expected.getShortOpt(), actual.getShortOpt());
			Assert.assertEquals("MaxValueCount", expected.getMaxValueCount(), actual.getMaxValueCount());
			Assert.assertEquals("Required", expected.isRequired(), actual.isRequired());
			Assert.assertEquals("MinValueCount", expected.getMinValueCount(), actual.getMinValueCount());
			Assert.assertEquals("ValueSep", expected.getValueSep(), actual.getValueSep());
			Assert.assertFalse("equivalence test", ParameterDefinition.isIdentical(expected, actual));
			Assert.assertFalse("inverse equivalence test", ParameterDefinition.isIdentical(actual, expected));
			Assert.assertNotEquals(expected.hashCode(), actual.hashCode());
		}
	}

	@Test
	public void testSetDescription() {
		Parameter expected = null;
		Parameter actual = null;

		expected = new Parameter();
		Assert.assertNotNull(expected);
		expected.setValueName(UUID.randomUUID().toString());
		expected.setDescription("test-description");
		expected.setLongOpt(UUID.randomUUID().toString());
		expected.setShortOpt('x');
		expected.setMaxValueCount(UUID.randomUUID().hashCode());
		expected.setRequired(true);
		expected.setMinValueCount(0);
		expected.setValueSep('|');

		actual = expected.clone();
		Assert.assertNotNull(actual);
		Assert.assertEquals("ValueName", expected.getValueName(), actual.getValueName());
		Assert.assertEquals("Description", expected.getDescription(), actual.getDescription());
		Assert.assertEquals("LongOpt", expected.getLongOpt(), actual.getLongOpt());
		Assert.assertEquals("ShortOpt", expected.getShortOpt(), actual.getShortOpt());
		Assert.assertEquals("MaxValueCount", expected.getMaxValueCount(), actual.getMaxValueCount());
		Assert.assertEquals("Required", expected.isRequired(), actual.isRequired());
		Assert.assertEquals("MinValueCount", expected.getMinValueCount(), actual.getMinValueCount());
		Assert.assertEquals("ValueSep", expected.getValueSep(), actual.getValueSep());
		Assert.assertTrue("equivalence test", ParameterDefinition.isIdentical(expected, actual));
		Assert.assertTrue("inverse equivalence test", ParameterDefinition.isIdentical(actual, expected));
		Assert.assertEquals("key", expected.getKey(), actual.getKey());

		for (int i = 1; i < 10; i++) {
			String v = UUID.randomUUID().toString();
			actual.setDescription(v);
			Assert.assertEquals(v, actual.getDescription());
			Assert.assertEquals("ValueName", expected.getValueName(), actual.getValueName());
			Assert.assertNotEquals("Description", expected.getDescription(), actual.getDescription());
			Assert.assertEquals("LongOpt", expected.getLongOpt(), actual.getLongOpt());
			Assert.assertEquals("ShortOpt", expected.getShortOpt(), actual.getShortOpt());
			Assert.assertEquals("MaxValueCount", expected.getMaxValueCount(), actual.getMaxValueCount());
			Assert.assertEquals("Required", expected.isRequired(), actual.isRequired());
			Assert.assertEquals("MinValueCount", expected.getMinValueCount(), actual.getMinValueCount());
			Assert.assertEquals("ValueSep", expected.getValueSep(), actual.getValueSep());
			Assert.assertFalse("equivalence test", ParameterDefinition.isIdentical(expected, actual));
			Assert.assertFalse("inverse equivalence test", ParameterDefinition.isIdentical(actual, expected));
			Assert.assertNotEquals(expected.hashCode(), actual.hashCode());
		}
	}

	@Test
	public void testSetLongOpt() {
		Parameter expected = null;
		Parameter actual = null;

		expected = new Parameter();
		Assert.assertNotNull(expected);
		expected.setValueName(UUID.randomUUID().toString());
		expected.setDescription(UUID.randomUUID().toString());
		expected.setLongOpt("test-description");
		expected.setShortOpt('x');
		expected.setMaxValueCount(UUID.randomUUID().hashCode());
		expected.setRequired(true);
		expected.setMinValueCount(0);
		expected.setValueSep('|');

		actual = expected.clone();
		Assert.assertNotNull(actual);
		Assert.assertEquals("ValueName", expected.getValueName(), actual.getValueName());
		Assert.assertEquals("Description", expected.getDescription(), actual.getDescription());
		Assert.assertEquals("LongOpt", expected.getLongOpt(), actual.getLongOpt());
		Assert.assertEquals("ShortOpt", expected.getShortOpt(), actual.getShortOpt());
		Assert.assertEquals("MaxValueCount", expected.getMaxValueCount(), actual.getMaxValueCount());
		Assert.assertEquals("Required", expected.isRequired(), actual.isRequired());
		Assert.assertEquals("MinValueCount", expected.getMinValueCount(), actual.getMinValueCount());
		Assert.assertEquals("ValueSep", expected.getValueSep(), actual.getValueSep());
		Assert.assertTrue("equivalence test", ParameterDefinition.isIdentical(expected, actual));
		Assert.assertTrue("inverse equivalence test", ParameterDefinition.isIdentical(actual, expected));
		Assert.assertEquals("key", expected.getKey(), actual.getKey());

		for (int i = 1; i < 10; i++) {
			String v = UUID.randomUUID().toString();
			actual.setLongOpt(v);
			Assert.assertEquals(v, actual.getLongOpt());
			Assert.assertEquals("ValueName", expected.getValueName(), actual.getValueName());
			Assert.assertEquals("Description", expected.getDescription(), actual.getDescription());
			Assert.assertNotEquals("LongOpt", expected.getLongOpt(), actual.getLongOpt());
			Assert.assertEquals("ShortOpt", expected.getShortOpt(), actual.getShortOpt());
			Assert.assertEquals("MaxValueCount", expected.getMaxValueCount(), actual.getMaxValueCount());
			Assert.assertEquals("Required", expected.isRequired(), actual.isRequired());
			Assert.assertEquals("MinValueCount", expected.getMinValueCount(), actual.getMinValueCount());
			Assert.assertEquals("ValueSep", expected.getValueSep(), actual.getValueSep());
			Assert.assertFalse("equivalence test", ParameterDefinition.isIdentical(expected, actual));
			Assert.assertFalse("inverse equivalence test", ParameterDefinition.isIdentical(actual, expected));
			Assert.assertNotEquals(expected.hashCode(), actual.hashCode());
		}
	}

	@Test
	public void testSetShortOpt() {
		Parameter expected = null;
		Parameter actual = null;

		expected = new Parameter();
		Assert.assertNotNull(expected);
		expected.setValueName(UUID.randomUUID().toString());
		expected.setDescription(UUID.randomUUID().toString());
		expected.setLongOpt(UUID.randomUUID().toString());
		expected.setShortOpt('G');
		expected.setMaxValueCount(UUID.randomUUID().hashCode());
		expected.setRequired(true);
		expected.setMinValueCount(0);
		expected.setValueSep('|');

		actual = expected.clone();
		Assert.assertNotNull(actual);
		Assert.assertEquals("ValueName", expected.getValueName(), actual.getValueName());
		Assert.assertEquals("Description", expected.getDescription(), actual.getDescription());
		Assert.assertEquals("LongOpt", expected.getLongOpt(), actual.getLongOpt());
		Assert.assertEquals("ShortOpt", expected.getShortOpt(), actual.getShortOpt());
		Assert.assertEquals("MaxValueCount", expected.getMaxValueCount(), actual.getMaxValueCount());
		Assert.assertEquals("Required", expected.isRequired(), actual.isRequired());
		Assert.assertEquals("MinValueCount", expected.getMinValueCount(), actual.getMinValueCount());
		Assert.assertEquals("ValueSep", expected.getValueSep(), actual.getValueSep());
		Assert.assertTrue("equivalence test", ParameterDefinition.isIdentical(expected, actual));
		Assert.assertTrue("inverse equivalence test", ParameterDefinition.isIdentical(actual, expected));
		Assert.assertEquals("key", expected.getKey(), actual.getKey());

		for (char c : UUID.randomUUID().toString().toCharArray()) {
			actual.setShortOpt(c);
			Assert.assertEquals(c, actual.getShortOpt().charValue());
			Assert.assertEquals("ValueName", expected.getValueName(), actual.getValueName());
			Assert.assertEquals("Description", expected.getDescription(), actual.getDescription());
			Assert.assertEquals("LongOpt", expected.getLongOpt(), actual.getLongOpt());
			Assert.assertNotEquals("ShortOpt", expected.getShortOpt(), actual.getShortOpt());
			Assert.assertEquals("MaxValueCount", expected.getMaxValueCount(), actual.getMaxValueCount());
			Assert.assertEquals("Required", expected.isRequired(), actual.isRequired());
			Assert.assertEquals("MinValueCount", expected.getMinValueCount(), actual.getMinValueCount());
			Assert.assertEquals("ValueSep", expected.getValueSep(), actual.getValueSep());
			Assert.assertFalse("equivalence test", ParameterDefinition.isIdentical(expected, actual));
			Assert.assertFalse("inverse equivalence test", ParameterDefinition.isIdentical(actual, expected));
			Assert.assertNotEquals(expected.hashCode(), actual.hashCode());
		}
	}

	@Test
	public void testSetValueSep() {
		Parameter expected = null;
		Parameter actual = null;

		expected = new Parameter();
		Assert.assertNotNull(expected);
		expected.setValueName(UUID.randomUUID().toString());
		expected.setDescription(UUID.randomUUID().toString());
		expected.setLongOpt(UUID.randomUUID().toString());
		expected.setShortOpt('G');
		expected.setMaxValueCount(UUID.randomUUID().hashCode());
		expected.setRequired(true);
		expected.setMinValueCount(0);
		expected.setValueSep('|');

		actual = expected.clone();
		Assert.assertNotNull(actual);
		Assert.assertEquals("ValueName", expected.getValueName(), actual.getValueName());
		Assert.assertEquals("Description", expected.getDescription(), actual.getDescription());
		Assert.assertEquals("LongOpt", expected.getLongOpt(), actual.getLongOpt());
		Assert.assertEquals("ShortOpt", expected.getShortOpt(), actual.getShortOpt());
		Assert.assertEquals("MaxValueCount", expected.getMaxValueCount(), actual.getMaxValueCount());
		Assert.assertEquals("Required", expected.isRequired(), actual.isRequired());
		Assert.assertEquals("MinValueCount", expected.getMinValueCount(), actual.getMinValueCount());
		Assert.assertEquals("ValueSep", expected.getValueSep(), actual.getValueSep());
		Assert.assertTrue("equivalence test", ParameterDefinition.isIdentical(expected, actual));
		Assert.assertTrue("inverse equivalence test", ParameterDefinition.isIdentical(actual, expected));
		Assert.assertEquals("key", expected.getKey(), actual.getKey());

		for (char c : UUID.randomUUID().toString().toCharArray()) {
			actual.setValueSep(c);
			Assert.assertEquals(c, actual.getValueSep().charValue());
			Assert.assertEquals("ValueName", expected.getValueName(), actual.getValueName());
			Assert.assertEquals("Description", expected.getDescription(), actual.getDescription());
			Assert.assertEquals("LongOpt", expected.getLongOpt(), actual.getLongOpt());
			Assert.assertEquals("ShortOpt", expected.getShortOpt(), actual.getShortOpt());
			Assert.assertEquals("MaxValueCount", expected.getMaxValueCount(), actual.getMaxValueCount());
			Assert.assertEquals("Required", expected.isRequired(), actual.isRequired());
			Assert.assertEquals("MinValueCount", expected.getMinValueCount(), actual.getMinValueCount());
			Assert.assertNotEquals("ValueSep", expected.getValueSep(), actual.getValueSep());
			Assert.assertFalse("equivalence test", ParameterDefinition.isIdentical(expected, actual));
			Assert.assertFalse("inverse equivalence test", ParameterDefinition.isIdentical(actual, expected));
			Assert.assertNotEquals(expected.hashCode(), actual.hashCode());
		}
	}

	@Test
	public void testSetValueOptional() {
		Parameter expected = null;
		Parameter actual = null;

		expected = new Parameter();
		Assert.assertNotNull(expected);
		expected.setValueName(UUID.randomUUID().toString());
		expected.setDescription(UUID.randomUUID().toString());
		expected.setLongOpt(UUID.randomUUID().toString());
		expected.setShortOpt('G');
		expected.setMaxValueCount(UUID.randomUUID().hashCode());
		expected.setRequired(true);
		expected.setMinValueCount(0);
		expected.setValueSep('|');

		actual = expected.clone();
		Assert.assertNotNull(actual);
		Assert.assertEquals("ValueName", expected.getValueName(), actual.getValueName());
		Assert.assertEquals("Description", expected.getDescription(), actual.getDescription());
		Assert.assertEquals("LongOpt", expected.getLongOpt(), actual.getLongOpt());
		Assert.assertEquals("ShortOpt", expected.getShortOpt(), actual.getShortOpt());
		Assert.assertEquals("MaxValueCount", expected.getMaxValueCount(), actual.getMaxValueCount());
		Assert.assertEquals("Required", expected.isRequired(), actual.isRequired());
		Assert.assertEquals("MinValueCount", expected.getMinValueCount(), actual.getMinValueCount());
		Assert.assertEquals("ValueSep", expected.getValueSep(), actual.getValueSep());
		Assert.assertTrue("equivalence test", ParameterDefinition.isIdentical(expected, actual));
		Assert.assertTrue("inverse equivalence test", ParameterDefinition.isIdentical(actual, expected));
		Assert.assertEquals("key", expected.getKey(), actual.getKey());

		actual.setMinValueCount(1);
		Assert.assertEquals(1, actual.getMinValueCount());
		Assert.assertEquals("ValueName", expected.getValueName(), actual.getValueName());
		Assert.assertEquals("Description", expected.getDescription(), actual.getDescription());
		Assert.assertEquals("LongOpt", expected.getLongOpt(), actual.getLongOpt());
		Assert.assertEquals("ShortOpt", expected.getShortOpt(), actual.getShortOpt());
		Assert.assertEquals("MaxValueCount", expected.getMaxValueCount(), actual.getMaxValueCount());
		Assert.assertEquals("Required", expected.isRequired(), actual.isRequired());
		Assert.assertEquals("ValueSep", expected.getValueSep(), actual.getValueSep());
		Assert.assertFalse("equivalence test", ParameterDefinition.isIdentical(expected, actual));
		Assert.assertFalse("inverse equivalence test", ParameterDefinition.isIdentical(actual, expected));
		Assert.assertNotEquals(expected.hashCode(), actual.hashCode());

		actual.setMinValueCount(0);
		Assert.assertEquals(0, actual.getMinValueCount());
		Assert.assertEquals("ValueName", expected.getValueName(), actual.getValueName());
		Assert.assertEquals("Description", expected.getDescription(), actual.getDescription());
		Assert.assertEquals("LongOpt", expected.getLongOpt(), actual.getLongOpt());
		Assert.assertEquals("ShortOpt", expected.getShortOpt(), actual.getShortOpt());
		Assert.assertEquals("MaxValueCount", expected.getMaxValueCount(), actual.getMaxValueCount());
		Assert.assertEquals("Required", expected.isRequired(), actual.isRequired());
		Assert.assertEquals("ValueSep", expected.getValueSep(), actual.getValueSep());
		Assert.assertTrue("equivalence test", ParameterDefinition.isIdentical(expected, actual));
		Assert.assertTrue("inverse equivalence test", ParameterDefinition.isIdentical(actual, expected));
		Assert.assertEquals("key", expected.getKey(), actual.getKey());
	}

	@Test
	public void testSetRequired() {
		Parameter expected = null;
		Parameter actual = null;

		expected = new Parameter();
		Assert.assertNotNull(expected);
		expected.setValueName(UUID.randomUUID().toString());
		expected.setDescription(UUID.randomUUID().toString());
		expected.setLongOpt(UUID.randomUUID().toString());
		expected.setShortOpt('G');
		expected.setMaxValueCount(UUID.randomUUID().hashCode());
		expected.setRequired(true);
		expected.setMinValueCount(0);
		expected.setValueSep('|');

		actual = expected.clone();
		Assert.assertNotNull(actual);
		Assert.assertEquals("ValueName", expected.getValueName(), actual.getValueName());
		Assert.assertEquals("Description", expected.getDescription(), actual.getDescription());
		Assert.assertEquals("LongOpt", expected.getLongOpt(), actual.getLongOpt());
		Assert.assertEquals("ShortOpt", expected.getShortOpt(), actual.getShortOpt());
		Assert.assertEquals("MaxValueCount", expected.getMaxValueCount(), actual.getMaxValueCount());
		Assert.assertEquals("Required", expected.isRequired(), actual.isRequired());
		Assert.assertEquals("MinValueCount", expected.getMinValueCount(), actual.getMinValueCount());
		Assert.assertEquals("ValueSep", expected.getValueSep(), actual.getValueSep());
		Assert.assertTrue("equivalence test", ParameterDefinition.isIdentical(expected, actual));
		Assert.assertTrue("inverse equivalence test", ParameterDefinition.isIdentical(actual, expected));
		Assert.assertEquals("key", expected.getKey(), actual.getKey());

		actual.setRequired(false);
		Assert.assertFalse(actual.isRequired());
		Assert.assertEquals("ValueName", expected.getValueName(), actual.getValueName());
		Assert.assertEquals("Description", expected.getDescription(), actual.getDescription());
		Assert.assertEquals("LongOpt", expected.getLongOpt(), actual.getLongOpt());
		Assert.assertEquals("ShortOpt", expected.getShortOpt(), actual.getShortOpt());
		Assert.assertEquals("MaxValueCount", expected.getMaxValueCount(), actual.getMaxValueCount());
		Assert.assertEquals("MinValueCount", expected.getMinValueCount(), actual.getMinValueCount());
		Assert.assertEquals("ValueSep", expected.getValueSep(), actual.getValueSep());
		Assert.assertFalse("equivalence test", ParameterDefinition.isIdentical(expected, actual));
		Assert.assertFalse("inverse equivalence test", ParameterDefinition.isIdentical(actual, expected));
		Assert.assertNotEquals(expected.hashCode(), actual.hashCode());

		actual.setRequired(true);
		Assert.assertTrue(actual.isRequired());
		Assert.assertEquals("ValueName", expected.getValueName(), actual.getValueName());
		Assert.assertEquals("Description", expected.getDescription(), actual.getDescription());
		Assert.assertEquals("LongOpt", expected.getLongOpt(), actual.getLongOpt());
		Assert.assertEquals("ShortOpt", expected.getShortOpt(), actual.getShortOpt());
		Assert.assertEquals("MaxValueCount", expected.getMaxValueCount(), actual.getMaxValueCount());
		Assert.assertEquals("MinValueCount", expected.getMinValueCount(), actual.getMinValueCount());
		Assert.assertEquals("ValueSep", expected.getValueSep(), actual.getValueSep());
		Assert.assertTrue("equivalence test", ParameterDefinition.isIdentical(expected, actual));
		Assert.assertTrue("inverse equivalence test", ParameterDefinition.isIdentical(actual, expected));
		Assert.assertEquals("key", expected.getKey(), actual.getKey());
	}
}