package com.armedia.caliente.cli.parser;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

public class MutableParameterDefinitionTest {

	@Test
	public void testConstructor() {
		MutableParameterDefinition expected = null;
		MutableParameterDefinition actual = null;

		expected = new MutableParameterDefinition();
		Assert.assertNotNull(expected);
		expected.setValueName(UUID.randomUUID().toString());
		expected.setDescription(UUID.randomUUID().toString());
		expected.setLongOpt(UUID.randomUUID().toString());
		expected.setShortOpt('x');
		expected.setValueCount(UUID.randomUUID().hashCode());
		expected.setRequired(true);
		expected.setValueOptional(true);
		expected.setValueSep('|');

		actual = new MutableParameterDefinition(expected);
		Assert.assertNotNull(actual);
		Assert.assertEquals("ValueName", expected.getValueName(), actual.getValueName());
		Assert.assertEquals("Description", expected.getDescription(), actual.getDescription());
		Assert.assertEquals("LongOpt", expected.getLongOpt(), actual.getLongOpt());
		Assert.assertEquals("ShortOpt", expected.getShortOpt(), actual.getShortOpt());
		Assert.assertEquals("ValueCount", expected.getValueCount(), actual.getValueCount());
		Assert.assertEquals("Required", expected.isRequired(), actual.isRequired());
		Assert.assertEquals("ValueOptional", expected.isValueOptional(), actual.isValueOptional());
		Assert.assertEquals("ValueSep", expected.getValueSep(), actual.getValueSep());
		Assert.assertTrue("equality test", expected.equals(actual));
		Assert.assertTrue("inverse equality test", actual.equals(expected));
		Assert.assertEquals(expected.hashCode(), actual.hashCode());

		expected = new MutableParameterDefinition();
		Assert.assertNotNull(expected);
		expected.setValueName(UUID.randomUUID().toString());
		expected.setDescription(UUID.randomUUID().toString());
		expected.setLongOpt(UUID.randomUUID().toString());
		expected.setShortOpt('P');
		expected.setValueCount(UUID.randomUUID().hashCode());
		expected.setRequired(false);
		expected.setValueOptional(false);
		expected.setValueSep('$');

		actual = new MutableParameterDefinition(expected);
		Assert.assertNotNull(actual);
		Assert.assertEquals("ValueName", expected.getValueName(), actual.getValueName());
		Assert.assertEquals("Description", expected.getDescription(), actual.getDescription());
		Assert.assertEquals("LongOpt", expected.getLongOpt(), actual.getLongOpt());
		Assert.assertEquals("ShortOpt", expected.getShortOpt(), actual.getShortOpt());
		Assert.assertEquals("ValueCount", expected.getValueCount(), actual.getValueCount());
		Assert.assertEquals("Required", expected.isRequired(), actual.isRequired());
		Assert.assertEquals("ValueOptional", expected.isValueOptional(), actual.isValueOptional());
		Assert.assertEquals("ValueSep", expected.getValueSep(), actual.getValueSep());
		Assert.assertTrue("equality test", expected.equals(actual));
		Assert.assertTrue("inverse equality test", actual.equals(expected));
		Assert.assertEquals(expected.hashCode(), actual.hashCode());
	}

	@Test
	public void testClone() {
		MutableParameterDefinition expected = null;
		MutableParameterDefinition actual = null;

		expected = new MutableParameterDefinition();
		Assert.assertNotNull(expected);
		expected.setValueName(UUID.randomUUID().toString());
		expected.setDescription(UUID.randomUUID().toString());
		expected.setLongOpt(UUID.randomUUID().toString());
		expected.setShortOpt('x');
		expected.setValueCount(UUID.randomUUID().hashCode());
		expected.setRequired(true);
		expected.setValueOptional(true);
		expected.setValueSep('|');

		actual = expected.clone();
		Assert.assertNotNull(actual);
		Assert.assertEquals("ValueName", expected.getValueName(), actual.getValueName());
		Assert.assertEquals("Description", expected.getDescription(), actual.getDescription());
		Assert.assertEquals("LongOpt", expected.getLongOpt(), actual.getLongOpt());
		Assert.assertEquals("ShortOpt", expected.getShortOpt(), actual.getShortOpt());
		Assert.assertEquals("ValueCount", expected.getValueCount(), actual.getValueCount());
		Assert.assertEquals("Required", expected.isRequired(), actual.isRequired());
		Assert.assertEquals("ValueOptional", expected.isValueOptional(), actual.isValueOptional());
		Assert.assertEquals("ValueSep", expected.getValueSep(), actual.getValueSep());
		Assert.assertTrue("equality test", expected.equals(actual));
		Assert.assertTrue("inverse equality test", actual.equals(expected));
		Assert.assertEquals(expected.hashCode(), actual.hashCode());
	}

	@Test
	public void testSetValueCount() {
		MutableParameterDefinition expected = null;
		MutableParameterDefinition actual = null;

		expected = new MutableParameterDefinition();
		Assert.assertNotNull(expected);
		expected.setValueName(UUID.randomUUID().toString());
		expected.setDescription(UUID.randomUUID().toString());
		expected.setLongOpt(UUID.randomUUID().toString());
		expected.setShortOpt('x');
		expected.setValueCount(0);
		expected.setRequired(true);
		expected.setValueOptional(true);
		expected.setValueSep('|');

		actual = expected.clone();
		Assert.assertNotNull(actual);
		Assert.assertEquals("ValueName", expected.getValueName(), actual.getValueName());
		Assert.assertEquals("Description", expected.getDescription(), actual.getDescription());
		Assert.assertEquals("LongOpt", expected.getLongOpt(), actual.getLongOpt());
		Assert.assertEquals("ShortOpt", expected.getShortOpt(), actual.getShortOpt());
		Assert.assertEquals("ValueCount", expected.getValueCount(), actual.getValueCount());
		Assert.assertEquals("Required", expected.isRequired(), actual.isRequired());
		Assert.assertEquals("ValueOptional", expected.isValueOptional(), actual.isValueOptional());
		Assert.assertEquals("ValueSep", expected.getValueSep(), actual.getValueSep());
		Assert.assertTrue("equality test", expected.equals(actual));
		Assert.assertTrue("inverse equality test", actual.equals(expected));
		Assert.assertEquals(expected.hashCode(), actual.hashCode());

		for (int i = 1; i < 100; i++) {
			actual.setValueCount(i);
			Assert.assertEquals(i, actual.getValueCount());
			Assert.assertEquals("ValueName", expected.getValueName(), actual.getValueName());
			Assert.assertEquals("Description", expected.getDescription(), actual.getDescription());
			Assert.assertEquals("LongOpt", expected.getLongOpt(), actual.getLongOpt());
			Assert.assertEquals("ShortOpt", expected.getShortOpt(), actual.getShortOpt());
			Assert.assertNotEquals("ValueCount", expected.getValueCount(), actual.getValueCount());
			Assert.assertEquals("Required", expected.isRequired(), actual.isRequired());
			Assert.assertEquals("ValueOptional", expected.isValueOptional(), actual.isValueOptional());
			Assert.assertEquals("ValueSep", expected.getValueSep(), actual.getValueSep());
			Assert.assertFalse("equality test", expected.equals(actual));
			Assert.assertFalse("inverse equality test", actual.equals(expected));
			Assert.assertNotEquals(expected.hashCode(), actual.hashCode());
		}
	}

	@Test
	public void testSetValueName() {
		MutableParameterDefinition expected = null;
		MutableParameterDefinition actual = null;

		expected = new MutableParameterDefinition();
		Assert.assertNotNull(expected);
		expected.setValueName("test-value-name");
		expected.setDescription(UUID.randomUUID().toString());
		expected.setLongOpt(UUID.randomUUID().toString());
		expected.setShortOpt('x');
		expected.setValueCount(UUID.randomUUID().hashCode());
		expected.setRequired(true);
		expected.setValueOptional(true);
		expected.setValueSep('|');

		actual = expected.clone();
		Assert.assertNotNull(actual);
		Assert.assertEquals("ValueName", expected.getValueName(), actual.getValueName());
		Assert.assertEquals("Description", expected.getDescription(), actual.getDescription());
		Assert.assertEquals("LongOpt", expected.getLongOpt(), actual.getLongOpt());
		Assert.assertEquals("ShortOpt", expected.getShortOpt(), actual.getShortOpt());
		Assert.assertEquals("ValueCount", expected.getValueCount(), actual.getValueCount());
		Assert.assertEquals("Required", expected.isRequired(), actual.isRequired());
		Assert.assertEquals("ValueOptional", expected.isValueOptional(), actual.isValueOptional());
		Assert.assertEquals("ValueSep", expected.getValueSep(), actual.getValueSep());
		Assert.assertTrue("equality test", expected.equals(actual));
		Assert.assertTrue("inverse equality test", actual.equals(expected));
		Assert.assertEquals(expected.hashCode(), actual.hashCode());

		for (int i = 1; i < 10; i++) {
			String v = UUID.randomUUID().toString();
			actual.setValueName(v);
			Assert.assertEquals(v, actual.getValueName());
			Assert.assertNotEquals("ValueName", expected.getValueName(), actual.getValueName());
			Assert.assertEquals("Description", expected.getDescription(), actual.getDescription());
			Assert.assertEquals("LongOpt", expected.getLongOpt(), actual.getLongOpt());
			Assert.assertEquals("ShortOpt", expected.getShortOpt(), actual.getShortOpt());
			Assert.assertEquals("ValueCount", expected.getValueCount(), actual.getValueCount());
			Assert.assertEquals("Required", expected.isRequired(), actual.isRequired());
			Assert.assertEquals("ValueOptional", expected.isValueOptional(), actual.isValueOptional());
			Assert.assertEquals("ValueSep", expected.getValueSep(), actual.getValueSep());
			Assert.assertFalse("equality test", expected.equals(actual));
			Assert.assertFalse("inverse equality test", actual.equals(expected));
			Assert.assertNotEquals(expected.hashCode(), actual.hashCode());
		}
	}

	@Test
	public void testSetDescription() {
		MutableParameterDefinition expected = null;
		MutableParameterDefinition actual = null;

		expected = new MutableParameterDefinition();
		Assert.assertNotNull(expected);
		expected.setValueName(UUID.randomUUID().toString());
		expected.setDescription("test-description");
		expected.setLongOpt(UUID.randomUUID().toString());
		expected.setShortOpt('x');
		expected.setValueCount(UUID.randomUUID().hashCode());
		expected.setRequired(true);
		expected.setValueOptional(true);
		expected.setValueSep('|');

		actual = expected.clone();
		Assert.assertNotNull(actual);
		Assert.assertEquals("ValueName", expected.getValueName(), actual.getValueName());
		Assert.assertEquals("Description", expected.getDescription(), actual.getDescription());
		Assert.assertEquals("LongOpt", expected.getLongOpt(), actual.getLongOpt());
		Assert.assertEquals("ShortOpt", expected.getShortOpt(), actual.getShortOpt());
		Assert.assertEquals("ValueCount", expected.getValueCount(), actual.getValueCount());
		Assert.assertEquals("Required", expected.isRequired(), actual.isRequired());
		Assert.assertEquals("ValueOptional", expected.isValueOptional(), actual.isValueOptional());
		Assert.assertEquals("ValueSep", expected.getValueSep(), actual.getValueSep());
		Assert.assertTrue("equality test", expected.equals(actual));
		Assert.assertTrue("inverse equality test", actual.equals(expected));
		Assert.assertEquals(expected.hashCode(), actual.hashCode());

		for (int i = 1; i < 10; i++) {
			String v = UUID.randomUUID().toString();
			actual.setDescription(v);
			Assert.assertEquals(v, actual.getDescription());
			Assert.assertEquals("ValueName", expected.getValueName(), actual.getValueName());
			Assert.assertNotEquals("Description", expected.getDescription(), actual.getDescription());
			Assert.assertEquals("LongOpt", expected.getLongOpt(), actual.getLongOpt());
			Assert.assertEquals("ShortOpt", expected.getShortOpt(), actual.getShortOpt());
			Assert.assertEquals("ValueCount", expected.getValueCount(), actual.getValueCount());
			Assert.assertEquals("Required", expected.isRequired(), actual.isRequired());
			Assert.assertEquals("ValueOptional", expected.isValueOptional(), actual.isValueOptional());
			Assert.assertEquals("ValueSep", expected.getValueSep(), actual.getValueSep());
			Assert.assertFalse("equality test", expected.equals(actual));
			Assert.assertFalse("inverse equality test", actual.equals(expected));
			Assert.assertNotEquals(expected.hashCode(), actual.hashCode());
		}
	}

	@Test
	public void testSetLongOpt() {
		MutableParameterDefinition expected = null;
		MutableParameterDefinition actual = null;

		expected = new MutableParameterDefinition();
		Assert.assertNotNull(expected);
		expected.setValueName(UUID.randomUUID().toString());
		expected.setDescription(UUID.randomUUID().toString());
		expected.setLongOpt("test-description");
		expected.setShortOpt('x');
		expected.setValueCount(UUID.randomUUID().hashCode());
		expected.setRequired(true);
		expected.setValueOptional(true);
		expected.setValueSep('|');

		actual = expected.clone();
		Assert.assertNotNull(actual);
		Assert.assertEquals("ValueName", expected.getValueName(), actual.getValueName());
		Assert.assertEquals("Description", expected.getDescription(), actual.getDescription());
		Assert.assertEquals("LongOpt", expected.getLongOpt(), actual.getLongOpt());
		Assert.assertEquals("ShortOpt", expected.getShortOpt(), actual.getShortOpt());
		Assert.assertEquals("ValueCount", expected.getValueCount(), actual.getValueCount());
		Assert.assertEquals("Required", expected.isRequired(), actual.isRequired());
		Assert.assertEquals("ValueOptional", expected.isValueOptional(), actual.isValueOptional());
		Assert.assertEquals("ValueSep", expected.getValueSep(), actual.getValueSep());
		Assert.assertTrue("equality test", expected.equals(actual));
		Assert.assertTrue("inverse equality test", actual.equals(expected));
		Assert.assertEquals(expected.hashCode(), actual.hashCode());

		for (int i = 1; i < 10; i++) {
			String v = UUID.randomUUID().toString();
			actual.setLongOpt(v);
			Assert.assertEquals(v, actual.getLongOpt());
			Assert.assertEquals("ValueName", expected.getValueName(), actual.getValueName());
			Assert.assertEquals("Description", expected.getDescription(), actual.getDescription());
			Assert.assertNotEquals("LongOpt", expected.getLongOpt(), actual.getLongOpt());
			Assert.assertEquals("ShortOpt", expected.getShortOpt(), actual.getShortOpt());
			Assert.assertEquals("ValueCount", expected.getValueCount(), actual.getValueCount());
			Assert.assertEquals("Required", expected.isRequired(), actual.isRequired());
			Assert.assertEquals("ValueOptional", expected.isValueOptional(), actual.isValueOptional());
			Assert.assertEquals("ValueSep", expected.getValueSep(), actual.getValueSep());
			Assert.assertFalse("equality test", expected.equals(actual));
			Assert.assertFalse("inverse equality test", actual.equals(expected));
			Assert.assertNotEquals(expected.hashCode(), actual.hashCode());
		}
	}

	@Test
	public void testSetShortOpt() {
		MutableParameterDefinition expected = null;
		MutableParameterDefinition actual = null;

		expected = new MutableParameterDefinition();
		Assert.assertNotNull(expected);
		expected.setValueName(UUID.randomUUID().toString());
		expected.setDescription(UUID.randomUUID().toString());
		expected.setLongOpt(UUID.randomUUID().toString());
		expected.setShortOpt('G');
		expected.setValueCount(UUID.randomUUID().hashCode());
		expected.setRequired(true);
		expected.setValueOptional(true);
		expected.setValueSep('|');

		actual = expected.clone();
		Assert.assertNotNull(actual);
		Assert.assertEquals("ValueName", expected.getValueName(), actual.getValueName());
		Assert.assertEquals("Description", expected.getDescription(), actual.getDescription());
		Assert.assertEquals("LongOpt", expected.getLongOpt(), actual.getLongOpt());
		Assert.assertEquals("ShortOpt", expected.getShortOpt(), actual.getShortOpt());
		Assert.assertEquals("ValueCount", expected.getValueCount(), actual.getValueCount());
		Assert.assertEquals("Required", expected.isRequired(), actual.isRequired());
		Assert.assertEquals("ValueOptional", expected.isValueOptional(), actual.isValueOptional());
		Assert.assertEquals("ValueSep", expected.getValueSep(), actual.getValueSep());
		Assert.assertTrue("equality test", expected.equals(actual));
		Assert.assertTrue("inverse equality test", actual.equals(expected));
		Assert.assertEquals(expected.hashCode(), actual.hashCode());

		for (char c : UUID.randomUUID().toString().toCharArray()) {
			actual.setShortOpt(c);
			Assert.assertEquals(c, actual.getShortOpt().charValue());
			Assert.assertEquals("ValueName", expected.getValueName(), actual.getValueName());
			Assert.assertEquals("Description", expected.getDescription(), actual.getDescription());
			Assert.assertEquals("LongOpt", expected.getLongOpt(), actual.getLongOpt());
			Assert.assertNotEquals("ShortOpt", expected.getShortOpt(), actual.getShortOpt());
			Assert.assertEquals("ValueCount", expected.getValueCount(), actual.getValueCount());
			Assert.assertEquals("Required", expected.isRequired(), actual.isRequired());
			Assert.assertEquals("ValueOptional", expected.isValueOptional(), actual.isValueOptional());
			Assert.assertEquals("ValueSep", expected.getValueSep(), actual.getValueSep());
			Assert.assertFalse("equality test", expected.equals(actual));
			Assert.assertFalse("inverse equality test", actual.equals(expected));
			Assert.assertNotEquals(expected.hashCode(), actual.hashCode());
		}
	}

	@Test
	public void testSetValueSep() {
		MutableParameterDefinition expected = null;
		MutableParameterDefinition actual = null;

		expected = new MutableParameterDefinition();
		Assert.assertNotNull(expected);
		expected.setValueName(UUID.randomUUID().toString());
		expected.setDescription(UUID.randomUUID().toString());
		expected.setLongOpt(UUID.randomUUID().toString());
		expected.setShortOpt('G');
		expected.setValueCount(UUID.randomUUID().hashCode());
		expected.setRequired(true);
		expected.setValueOptional(true);
		expected.setValueSep('|');

		actual = expected.clone();
		Assert.assertNotNull(actual);
		Assert.assertEquals("ValueName", expected.getValueName(), actual.getValueName());
		Assert.assertEquals("Description", expected.getDescription(), actual.getDescription());
		Assert.assertEquals("LongOpt", expected.getLongOpt(), actual.getLongOpt());
		Assert.assertEquals("ShortOpt", expected.getShortOpt(), actual.getShortOpt());
		Assert.assertEquals("ValueCount", expected.getValueCount(), actual.getValueCount());
		Assert.assertEquals("Required", expected.isRequired(), actual.isRequired());
		Assert.assertEquals("ValueOptional", expected.isValueOptional(), actual.isValueOptional());
		Assert.assertEquals("ValueSep", expected.getValueSep(), actual.getValueSep());
		Assert.assertTrue("equality test", expected.equals(actual));
		Assert.assertTrue("inverse equality test", actual.equals(expected));
		Assert.assertEquals(expected.hashCode(), actual.hashCode());

		for (char c : UUID.randomUUID().toString().toCharArray()) {
			actual.setValueSep(c);
			Assert.assertEquals(c, actual.getValueSep().charValue());
			Assert.assertEquals("ValueName", expected.getValueName(), actual.getValueName());
			Assert.assertEquals("Description", expected.getDescription(), actual.getDescription());
			Assert.assertEquals("LongOpt", expected.getLongOpt(), actual.getLongOpt());
			Assert.assertEquals("ShortOpt", expected.getShortOpt(), actual.getShortOpt());
			Assert.assertEquals("ValueCount", expected.getValueCount(), actual.getValueCount());
			Assert.assertEquals("Required", expected.isRequired(), actual.isRequired());
			Assert.assertEquals("ValueOptional", expected.isValueOptional(), actual.isValueOptional());
			Assert.assertNotEquals("ValueSep", expected.getValueSep(), actual.getValueSep());
			Assert.assertFalse("equality test", expected.equals(actual));
			Assert.assertFalse("inverse equality test", actual.equals(expected));
			Assert.assertNotEquals(expected.hashCode(), actual.hashCode());
		}
	}

	@Test
	public void testSetValueOptional() {
		MutableParameterDefinition expected = null;
		MutableParameterDefinition actual = null;

		expected = new MutableParameterDefinition();
		Assert.assertNotNull(expected);
		expected.setValueName(UUID.randomUUID().toString());
		expected.setDescription(UUID.randomUUID().toString());
		expected.setLongOpt(UUID.randomUUID().toString());
		expected.setShortOpt('G');
		expected.setValueCount(UUID.randomUUID().hashCode());
		expected.setRequired(true);
		expected.setValueOptional(true);
		expected.setValueSep('|');

		actual = expected.clone();
		Assert.assertNotNull(actual);
		Assert.assertEquals("ValueName", expected.getValueName(), actual.getValueName());
		Assert.assertEquals("Description", expected.getDescription(), actual.getDescription());
		Assert.assertEquals("LongOpt", expected.getLongOpt(), actual.getLongOpt());
		Assert.assertEquals("ShortOpt", expected.getShortOpt(), actual.getShortOpt());
		Assert.assertEquals("ValueCount", expected.getValueCount(), actual.getValueCount());
		Assert.assertEquals("Required", expected.isRequired(), actual.isRequired());
		Assert.assertEquals("ValueOptional", expected.isValueOptional(), actual.isValueOptional());
		Assert.assertEquals("ValueSep", expected.getValueSep(), actual.getValueSep());
		Assert.assertTrue("equality test", expected.equals(actual));
		Assert.assertTrue("inverse equality test", actual.equals(expected));
		Assert.assertEquals(expected.hashCode(), actual.hashCode());

		actual.setValueOptional(false);
		Assert.assertFalse(actual.isValueOptional());
		Assert.assertEquals("ValueName", expected.getValueName(), actual.getValueName());
		Assert.assertEquals("Description", expected.getDescription(), actual.getDescription());
		Assert.assertEquals("LongOpt", expected.getLongOpt(), actual.getLongOpt());
		Assert.assertEquals("ShortOpt", expected.getShortOpt(), actual.getShortOpt());
		Assert.assertEquals("ValueCount", expected.getValueCount(), actual.getValueCount());
		Assert.assertEquals("Required", expected.isRequired(), actual.isRequired());
		Assert.assertEquals("ValueSep", expected.getValueSep(), actual.getValueSep());
		Assert.assertFalse("equality test", expected.equals(actual));
		Assert.assertFalse("inverse equality test", actual.equals(expected));
		Assert.assertNotEquals(expected.hashCode(), actual.hashCode());

		actual.setValueOptional(true);
		Assert.assertTrue(actual.isValueOptional());
		Assert.assertEquals("ValueName", expected.getValueName(), actual.getValueName());
		Assert.assertEquals("Description", expected.getDescription(), actual.getDescription());
		Assert.assertEquals("LongOpt", expected.getLongOpt(), actual.getLongOpt());
		Assert.assertEquals("ShortOpt", expected.getShortOpt(), actual.getShortOpt());
		Assert.assertEquals("ValueCount", expected.getValueCount(), actual.getValueCount());
		Assert.assertEquals("Required", expected.isRequired(), actual.isRequired());
		Assert.assertEquals("ValueSep", expected.getValueSep(), actual.getValueSep());
		Assert.assertTrue("equality test", expected.equals(actual));
		Assert.assertTrue("inverse equality test", actual.equals(expected));
		Assert.assertEquals(expected.hashCode(), actual.hashCode());
	}

	@Test
	public void testSetRequired() {
		MutableParameterDefinition expected = null;
		MutableParameterDefinition actual = null;

		expected = new MutableParameterDefinition();
		Assert.assertNotNull(expected);
		expected.setValueName(UUID.randomUUID().toString());
		expected.setDescription(UUID.randomUUID().toString());
		expected.setLongOpt(UUID.randomUUID().toString());
		expected.setShortOpt('G');
		expected.setValueCount(UUID.randomUUID().hashCode());
		expected.setRequired(true);
		expected.setValueOptional(true);
		expected.setValueSep('|');

		actual = expected.clone();
		Assert.assertNotNull(actual);
		Assert.assertEquals("ValueName", expected.getValueName(), actual.getValueName());
		Assert.assertEquals("Description", expected.getDescription(), actual.getDescription());
		Assert.assertEquals("LongOpt", expected.getLongOpt(), actual.getLongOpt());
		Assert.assertEquals("ShortOpt", expected.getShortOpt(), actual.getShortOpt());
		Assert.assertEquals("ValueCount", expected.getValueCount(), actual.getValueCount());
		Assert.assertEquals("Required", expected.isRequired(), actual.isRequired());
		Assert.assertEquals("ValueOptional", expected.isValueOptional(), actual.isValueOptional());
		Assert.assertEquals("ValueSep", expected.getValueSep(), actual.getValueSep());
		Assert.assertTrue("equality test", expected.equals(actual));
		Assert.assertTrue("inverse equality test", actual.equals(expected));
		Assert.assertEquals(expected.hashCode(), actual.hashCode());

		actual.setRequired(false);
		Assert.assertFalse(actual.isRequired());
		Assert.assertEquals("ValueName", expected.getValueName(), actual.getValueName());
		Assert.assertEquals("Description", expected.getDescription(), actual.getDescription());
		Assert.assertEquals("LongOpt", expected.getLongOpt(), actual.getLongOpt());
		Assert.assertEquals("ShortOpt", expected.getShortOpt(), actual.getShortOpt());
		Assert.assertEquals("ValueCount", expected.getValueCount(), actual.getValueCount());
		Assert.assertEquals("ValueOptional", expected.isValueOptional(), actual.isValueOptional());
		Assert.assertEquals("ValueSep", expected.getValueSep(), actual.getValueSep());
		Assert.assertFalse("equality test", expected.equals(actual));
		Assert.assertFalse("inverse equality test", actual.equals(expected));
		Assert.assertNotEquals(expected.hashCode(), actual.hashCode());

		actual.setRequired(true);
		Assert.assertTrue(actual.isRequired());
		Assert.assertEquals("ValueName", expected.getValueName(), actual.getValueName());
		Assert.assertEquals("Description", expected.getDescription(), actual.getDescription());
		Assert.assertEquals("LongOpt", expected.getLongOpt(), actual.getLongOpt());
		Assert.assertEquals("ShortOpt", expected.getShortOpt(), actual.getShortOpt());
		Assert.assertEquals("ValueCount", expected.getValueCount(), actual.getValueCount());
		Assert.assertEquals("ValueOptional", expected.isValueOptional(), actual.isValueOptional());
		Assert.assertEquals("ValueSep", expected.getValueSep(), actual.getValueSep());
		Assert.assertTrue("equality test", expected.equals(actual));
		Assert.assertTrue("inverse equality test", actual.equals(expected));
		Assert.assertEquals(expected.hashCode(), actual.hashCode());
	}
}