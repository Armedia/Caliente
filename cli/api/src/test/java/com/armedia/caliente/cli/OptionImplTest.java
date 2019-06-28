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
package com.armedia.caliente.cli;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class OptionImplTest {

	@Test
	public void testConstructor() {
		OptionImpl expected = null;
		OptionImpl actual = null;

		expected = new OptionImpl();
		Assertions.assertNotNull(expected);
		expected.setArgumentName(UUID.randomUUID().toString());
		expected.setDescription(UUID.randomUUID().toString());
		expected.setLongOpt(UUID.randomUUID().toString());
		expected.setShortOpt('x');
		expected.setMaxArguments(UUID.randomUUID().hashCode());
		expected.setRequired(true);
		expected.setMinArguments(0);
		expected.setValueSep('|');

		actual = new OptionImpl(expected);
		Assertions.assertNotNull(actual);
		Assertions.assertEquals(expected.getArgumentName(), actual.getArgumentName(), "ValueName");
		Assertions.assertEquals(expected.getDescription(), actual.getDescription(), "Description");
		Assertions.assertEquals(expected.getLongOpt(), actual.getLongOpt(), "LongOpt");
		Assertions.assertEquals(expected.getShortOpt(), actual.getShortOpt(), "ShortOpt");
		Assertions.assertEquals(expected.getMaxArguments(), actual.getMaxArguments(), "MaxValueCount");
		Assertions.assertEquals(expected.isRequired(), actual.isRequired(), "Required");
		Assertions.assertEquals(expected.getMinArguments(), actual.getMinArguments(), "MinValueCount");
		Assertions.assertEquals(expected.getValueSep(), actual.getValueSep(), "ValueSep");
		Assertions.assertTrue(Option.isIdentical(expected, actual), "equivalence test");
		Assertions.assertTrue(Option.isIdentical(actual, expected), "inverse equivalence test");
		Assertions.assertEquals(expected.getKey(), actual.getKey(), "key");

		expected = new OptionImpl();
		Assertions.assertNotNull(expected);
		expected.setArgumentName(UUID.randomUUID().toString());
		expected.setDescription(UUID.randomUUID().toString());
		expected.setLongOpt(UUID.randomUUID().toString());
		expected.setShortOpt('P');
		expected.setMaxArguments(UUID.randomUUID().hashCode());
		expected.setRequired(false);
		expected.setMinArguments(1);
		expected.setValueSep('$');

		actual = new OptionImpl(expected);
		Assertions.assertNotNull(actual);
		Assertions.assertEquals(expected.getArgumentName(), actual.getArgumentName(), "ValueName");
		Assertions.assertEquals(expected.getDescription(), actual.getDescription(), "Description");
		Assertions.assertEquals(expected.getLongOpt(), actual.getLongOpt(), "LongOpt");
		Assertions.assertEquals(expected.getShortOpt(), actual.getShortOpt(), "ShortOpt");
		Assertions.assertEquals(expected.getMaxArguments(), actual.getMaxArguments(), "MaxValueCount");
		Assertions.assertEquals(expected.isRequired(), actual.isRequired(), "Required");
		Assertions.assertEquals(expected.getMinArguments(), actual.getMinArguments(), "MinValueCount");
		Assertions.assertEquals(expected.getValueSep(), actual.getValueSep(), "ValueSep");
		Assertions.assertTrue(Option.isIdentical(expected, actual), "equivalence test");
		Assertions.assertTrue(Option.isIdentical(actual, expected), "inverse equivalence test");
		Assertions.assertEquals(expected.getKey(), actual.getKey(), "key");

		Assertions.assertNotNull(expected.toString());
	}

	@Test
	public void testEquals() {
		OptionImpl def = null;

		def = new OptionImpl();
		Assertions.assertNotNull(def);
		def.setArgumentName(UUID.randomUUID().toString());
		def.setDescription(UUID.randomUUID().toString());
		def.setLongOpt(UUID.randomUUID().toString());
		def.setShortOpt('x');
		def.setMaxArguments(UUID.randomUUID().hashCode());
		def.setRequired(true);
		def.setMinArguments(0);
		def.setValueSep('|');

		Assertions.assertNotEquals(def, null);
		Assertions.assertNotEquals(def, new Object());
		Assertions.assertNotEquals(def, "");
		Assertions.assertEquals(def, def);
	}

	@Test
	public void testClone() {
		OptionImpl expected = null;
		OptionImpl actual = null;

		expected = new OptionImpl();
		Assertions.assertNotNull(expected);
		expected.setArgumentName(UUID.randomUUID().toString());
		expected.setDescription(UUID.randomUUID().toString());
		expected.setLongOpt(UUID.randomUUID().toString());
		expected.setShortOpt('x');
		expected.setMaxArguments(UUID.randomUUID().hashCode());
		expected.setRequired(true);
		expected.setMinArguments(0);
		expected.setValueSep('|');

		actual = expected.clone();
		Assertions.assertNotNull(actual);
		Assertions.assertEquals(expected.getArgumentName(), actual.getArgumentName(), "ValueName");
		Assertions.assertEquals(expected.getDescription(), actual.getDescription(), "Description");
		Assertions.assertEquals(expected.getLongOpt(), actual.getLongOpt(), "LongOpt");
		Assertions.assertEquals(expected.getShortOpt(), actual.getShortOpt(), "ShortOpt");
		Assertions.assertEquals(expected.getMaxArguments(), actual.getMaxArguments(), "MaxValueCount");
		Assertions.assertEquals(expected.isRequired(), actual.isRequired(), "Required");
		Assertions.assertEquals(expected.getMinArguments(), actual.getMinArguments(), "MinValueCount");
		Assertions.assertEquals(expected.getValueSep(), actual.getValueSep(), "ValueSep");
		Assertions.assertTrue(Option.isIdentical(expected, actual), "equivalence test");
		Assertions.assertTrue(Option.isIdentical(actual, expected), "inverse equivalence test");
		Assertions.assertEquals(expected.getKey(), actual.getKey(), "key");
	}

	@Test
	public void testSetValueCount() {
		OptionImpl expected = null;
		OptionImpl actual = null;

		expected = new OptionImpl();
		Assertions.assertNotNull(expected);
		expected.setArgumentName(UUID.randomUUID().toString());
		expected.setDescription(UUID.randomUUID().toString());
		expected.setLongOpt(UUID.randomUUID().toString());
		expected.setShortOpt('x');
		expected.setMaxArguments(0);
		expected.setRequired(true);
		expected.setMinArguments(0);
		expected.setValueSep('|');

		actual = expected.clone();
		Assertions.assertNotNull(actual);
		Assertions.assertEquals(expected.getArgumentName(), actual.getArgumentName(), "ValueName");
		Assertions.assertEquals(expected.getDescription(), actual.getDescription(), "Description");
		Assertions.assertEquals(expected.getLongOpt(), actual.getLongOpt(), "LongOpt");
		Assertions.assertEquals(expected.getShortOpt(), actual.getShortOpt(), "ShortOpt");
		Assertions.assertEquals(expected.getMaxArguments(), actual.getMaxArguments(), "MaxValueCount");
		Assertions.assertEquals(expected.isRequired(), actual.isRequired(), "Required");
		Assertions.assertEquals(expected.getMinArguments(), actual.getMinArguments(), "MinValueCount");
		Assertions.assertEquals(expected.getValueSep(), actual.getValueSep(), "ValueSep");
		Assertions.assertTrue(Option.isIdentical(expected, actual), "equivalence test");
		Assertions.assertTrue(Option.isIdentical(actual, expected), "inverse equivalence test");
		Assertions.assertEquals(expected.getKey(), actual.getKey(), "key");

		for (int i = 1; i < 100; i++) {
			actual.setMaxArguments(i);
			Assertions.assertEquals(i, actual.getMaxArguments());
			Assertions.assertEquals(expected.getArgumentName(), actual.getArgumentName(), "ValueName");
			Assertions.assertEquals(expected.getDescription(), actual.getDescription(), "Description");
			Assertions.assertEquals(expected.getLongOpt(), actual.getLongOpt(), "LongOpt");
			Assertions.assertEquals(expected.getShortOpt(), actual.getShortOpt(), "ShortOpt");
			Assertions.assertEquals(expected.isRequired(), actual.isRequired(), "Required");
			Assertions.assertEquals(expected.getMinArguments(), actual.getMinArguments(), "MinValueCount");
			Assertions.assertEquals(expected.getValueSep(), actual.getValueSep(), "ValueSep");
			Assertions.assertFalse(Option.isIdentical(expected, actual), "equivalence test");
			Assertions.assertFalse(Option.isIdentical(actual, expected), "inverse equivalence test");
			Assertions.assertEquals(expected.getKey(), actual.getKey(), "key");
		}
	}

	@Test
	public void testSetValueName() {
		OptionImpl expected = null;
		OptionImpl actual = null;

		expected = new OptionImpl();
		Assertions.assertNotNull(expected);
		expected.setArgumentName("test-value-name");
		expected.setDescription(UUID.randomUUID().toString());
		expected.setLongOpt(UUID.randomUUID().toString());
		expected.setShortOpt('x');
		expected.setMaxArguments(UUID.randomUUID().hashCode());
		expected.setRequired(true);
		expected.setMinArguments(0);
		expected.setValueSep('|');

		actual = expected.clone();
		Assertions.assertNotNull(actual);
		Assertions.assertEquals(expected.getArgumentName(), actual.getArgumentName(), "ValueName");
		Assertions.assertEquals(expected.getDescription(), actual.getDescription(), "Description");
		Assertions.assertEquals(expected.getLongOpt(), actual.getLongOpt(), "LongOpt");
		Assertions.assertEquals(expected.getShortOpt(), actual.getShortOpt(), "ShortOpt");
		Assertions.assertEquals(expected.getMaxArguments(), actual.getMaxArguments(), "MaxValueCount");
		Assertions.assertEquals(expected.isRequired(), actual.isRequired(), "Required");
		Assertions.assertEquals(expected.getMinArguments(), actual.getMinArguments(), "MinValueCount");
		Assertions.assertEquals(expected.getValueSep(), actual.getValueSep(), "ValueSep");
		Assertions.assertTrue(Option.isIdentical(expected, actual), "equivalence test");
		Assertions.assertTrue(Option.isIdentical(actual, expected), "inverse equivalence test");
		Assertions.assertEquals(expected.getKey(), actual.getKey(), "key");

		for (int i = 1; i < 10; i++) {
			String v = UUID.randomUUID().toString();
			actual.setArgumentName(v);
			Assertions.assertEquals(v, actual.getArgumentName());
			Assertions.assertNotEquals(expected.getArgumentName(), actual.getArgumentName(), "ValueName");
			Assertions.assertEquals(expected.getDescription(), actual.getDescription(), "Description");
			Assertions.assertEquals(expected.getLongOpt(), actual.getLongOpt(), "LongOpt");
			Assertions.assertEquals(expected.getShortOpt(), actual.getShortOpt(), "ShortOpt");
			Assertions.assertEquals(expected.getMaxArguments(), actual.getMaxArguments(), "MaxValueCount");
			Assertions.assertEquals(expected.isRequired(), actual.isRequired(), "Required");
			Assertions.assertEquals(expected.getMinArguments(), actual.getMinArguments(), "MinValueCount");
			Assertions.assertEquals(expected.getValueSep(), actual.getValueSep(), "ValueSep");
			Assertions.assertFalse(Option.isIdentical(expected, actual), "equivalence test");
			Assertions.assertFalse(Option.isIdentical(actual, expected), "inverse equivalence test");
			Assertions.assertEquals(expected.getKey(), actual.getKey(), "key");
		}
	}

	@Test
	public void testSetDescription() {
		OptionImpl expected = null;
		OptionImpl actual = null;

		expected = new OptionImpl();
		Assertions.assertNotNull(expected);
		expected.setArgumentName(UUID.randomUUID().toString());
		expected.setDescription("test-description");
		expected.setLongOpt(UUID.randomUUID().toString());
		expected.setShortOpt('x');
		expected.setMaxArguments(UUID.randomUUID().hashCode());
		expected.setRequired(true);
		expected.setMinArguments(0);
		expected.setValueSep('|');

		actual = expected.clone();
		Assertions.assertNotNull(actual);
		Assertions.assertEquals(expected.getArgumentName(), actual.getArgumentName(), "ValueName");
		Assertions.assertEquals(expected.getDescription(), actual.getDescription(), "Description");
		Assertions.assertEquals(expected.getLongOpt(), actual.getLongOpt(), "LongOpt");
		Assertions.assertEquals(expected.getShortOpt(), actual.getShortOpt(), "ShortOpt");
		Assertions.assertEquals(expected.getMaxArguments(), actual.getMaxArguments(), "MaxValueCount");
		Assertions.assertEquals(expected.isRequired(), actual.isRequired(), "Required");
		Assertions.assertEquals(expected.getMinArguments(), actual.getMinArguments(), "MinValueCount");
		Assertions.assertEquals(expected.getValueSep(), actual.getValueSep(), "ValueSep");
		Assertions.assertTrue(Option.isIdentical(expected, actual), "equivalence test");
		Assertions.assertTrue(Option.isIdentical(actual, expected), "inverse equivalence test");
		Assertions.assertEquals(expected.getKey(), actual.getKey(), "key");

		for (int i = 1; i < 10; i++) {
			String v = UUID.randomUUID().toString();
			actual.setDescription(v);
			Assertions.assertEquals(v, actual.getDescription());
			Assertions.assertEquals(expected.getArgumentName(), actual.getArgumentName(), "ValueName");
			Assertions.assertNotEquals(expected.getDescription(), actual.getDescription(), "Description");
			Assertions.assertEquals(expected.getLongOpt(), actual.getLongOpt(), "LongOpt");
			Assertions.assertEquals(expected.getShortOpt(), actual.getShortOpt(), "ShortOpt");
			Assertions.assertEquals(expected.getMaxArguments(), actual.getMaxArguments(), "MaxValueCount");
			Assertions.assertEquals(expected.isRequired(), actual.isRequired(), "Required");
			Assertions.assertEquals(expected.getMinArguments(), actual.getMinArguments(), "MinValueCount");
			Assertions.assertEquals(expected.getValueSep(), actual.getValueSep(), "ValueSep");
			Assertions.assertFalse(Option.isIdentical(expected, actual), "equivalence test");
			Assertions.assertFalse(Option.isIdentical(actual, expected), "inverse equivalence test");
			Assertions.assertEquals(expected.getKey(), actual.getKey(), "key");
		}
	}

	@Test
	public void testSetLongOpt() {
		OptionImpl expected = null;
		OptionImpl actual = null;

		expected = new OptionImpl();
		Assertions.assertNotNull(expected);
		expected.setArgumentName(UUID.randomUUID().toString());
		expected.setDescription(UUID.randomUUID().toString());
		expected.setLongOpt("test-description");
		expected.setShortOpt('x');
		expected.setMaxArguments(UUID.randomUUID().hashCode());
		expected.setRequired(true);
		expected.setMinArguments(0);
		expected.setValueSep('|');

		actual = expected.clone();
		Assertions.assertNotNull(actual);
		Assertions.assertEquals(expected.getArgumentName(), actual.getArgumentName(), "ValueName");
		Assertions.assertEquals(expected.getDescription(), actual.getDescription(), "Description");
		Assertions.assertEquals(expected.getLongOpt(), actual.getLongOpt(), "LongOpt");
		Assertions.assertEquals(expected.getShortOpt(), actual.getShortOpt(), "ShortOpt");
		Assertions.assertEquals(expected.getMaxArguments(), actual.getMaxArguments(), "MaxValueCount");
		Assertions.assertEquals(expected.isRequired(), actual.isRequired(), "Required");
		Assertions.assertEquals(expected.getMinArguments(), actual.getMinArguments(), "MinValueCount");
		Assertions.assertEquals(expected.getValueSep(), actual.getValueSep(), "ValueSep");
		Assertions.assertTrue(Option.isIdentical(expected, actual), "equivalence test");
		Assertions.assertTrue(Option.isIdentical(actual, expected), "inverse equivalence test");
		Assertions.assertEquals(expected.getKey(), actual.getKey(), "key");

		for (int i = 1; i < 10; i++) {
			String v = UUID.randomUUID().toString();
			actual.setLongOpt(v);
			Assertions.assertEquals(v, actual.getLongOpt());
			Assertions.assertEquals(expected.getArgumentName(), actual.getArgumentName(), "ValueName");
			Assertions.assertEquals(expected.getDescription(), actual.getDescription(), "Description");
			Assertions.assertNotEquals(expected.getLongOpt(), actual.getLongOpt(), "LongOpt");
			Assertions.assertEquals(expected.getShortOpt(), actual.getShortOpt(), "ShortOpt");
			Assertions.assertEquals(expected.getMaxArguments(), actual.getMaxArguments(), "MaxValueCount");
			Assertions.assertEquals(expected.isRequired(), actual.isRequired(), "Required");
			Assertions.assertEquals(expected.getMinArguments(), actual.getMinArguments(), "MinValueCount");
			Assertions.assertEquals(expected.getValueSep(), actual.getValueSep(), "ValueSep");
			Assertions.assertFalse(Option.isIdentical(expected, actual), "equivalence test");
			Assertions.assertFalse(Option.isIdentical(actual, expected), "inverse equivalence test");
		}
	}

	@Test
	public void testSetShortOpt() {
		OptionImpl expected = null;
		OptionImpl actual = null;

		expected = new OptionImpl();
		Assertions.assertNotNull(expected);
		expected.setArgumentName(UUID.randomUUID().toString());
		expected.setDescription(UUID.randomUUID().toString());
		expected.setLongOpt(UUID.randomUUID().toString());
		expected.setShortOpt('G');
		expected.setMaxArguments(UUID.randomUUID().hashCode());
		expected.setRequired(true);
		expected.setMinArguments(0);
		expected.setValueSep('|');

		actual = expected.clone();
		Assertions.assertNotNull(actual);
		Assertions.assertEquals(expected.getArgumentName(), actual.getArgumentName(), "ValueName");
		Assertions.assertEquals(expected.getDescription(), actual.getDescription(), "Description");
		Assertions.assertEquals(expected.getLongOpt(), actual.getLongOpt(), "LongOpt");
		Assertions.assertEquals(expected.getShortOpt(), actual.getShortOpt(), "ShortOpt");
		Assertions.assertEquals(expected.getMaxArguments(), actual.getMaxArguments(), "MaxValueCount");
		Assertions.assertEquals(expected.isRequired(), actual.isRequired(), "Required");
		Assertions.assertEquals(expected.getMinArguments(), actual.getMinArguments(), "MinValueCount");
		Assertions.assertEquals(expected.getValueSep(), actual.getValueSep(), "ValueSep");
		Assertions.assertTrue(Option.isIdentical(expected, actual), "equivalence test");
		Assertions.assertTrue(Option.isIdentical(actual, expected), "inverse equivalence test");
		Assertions.assertEquals(expected.getKey(), actual.getKey(), "key");

		for (char c : UUID.randomUUID().toString().toCharArray()) {
			if (c == '-') {
				continue;
			}
			actual.setShortOpt(c);
			Assertions.assertEquals(c, actual.getShortOpt().charValue());
			Assertions.assertEquals(expected.getArgumentName(), actual.getArgumentName(), "ValueName");
			Assertions.assertEquals(expected.getDescription(), actual.getDescription(), "Description");
			Assertions.assertEquals(expected.getLongOpt(), actual.getLongOpt(), "LongOpt");
			Assertions.assertNotEquals(expected.getShortOpt(), actual.getShortOpt(), "ShortOpt");
			Assertions.assertEquals(expected.getMaxArguments(), actual.getMaxArguments(), "MaxValueCount");
			Assertions.assertEquals(expected.isRequired(), actual.isRequired(), "Required");
			Assertions.assertEquals(expected.getMinArguments(), actual.getMinArguments(), "MinValueCount");
			Assertions.assertEquals(expected.getValueSep(), actual.getValueSep(), "ValueSep");
			Assertions.assertFalse(Option.isIdentical(expected, actual), "equivalence test");
			Assertions.assertFalse(Option.isIdentical(actual, expected), "inverse equivalence test");
			Assertions.assertNotEquals(expected.hashCode(), actual.hashCode());
		}
	}

	@Test
	public void testSetValueSep() {
		OptionImpl expected = null;
		OptionImpl actual = null;

		expected = new OptionImpl();
		Assertions.assertNotNull(expected);
		expected.setArgumentName(UUID.randomUUID().toString());
		expected.setDescription(UUID.randomUUID().toString());
		expected.setLongOpt(UUID.randomUUID().toString());
		expected.setShortOpt('G');
		expected.setMaxArguments(UUID.randomUUID().hashCode());
		expected.setRequired(true);
		expected.setMinArguments(0);
		expected.setValueSep('|');

		actual = expected.clone();
		Assertions.assertNotNull(actual);
		Assertions.assertEquals(expected.getArgumentName(), actual.getArgumentName(), "ValueName");
		Assertions.assertEquals(expected.getDescription(), actual.getDescription(), "Description");
		Assertions.assertEquals(expected.getLongOpt(), actual.getLongOpt(), "LongOpt");
		Assertions.assertEquals(expected.getShortOpt(), actual.getShortOpt(), "ShortOpt");
		Assertions.assertEquals(expected.getMaxArguments(), actual.getMaxArguments(), "MaxValueCount");
		Assertions.assertEquals(expected.isRequired(), actual.isRequired(), "Required");
		Assertions.assertEquals(expected.getMinArguments(), actual.getMinArguments(), "MinValueCount");
		Assertions.assertEquals(expected.getValueSep(), actual.getValueSep(), "ValueSep");
		Assertions.assertTrue(Option.isIdentical(expected, actual), "equivalence test");
		Assertions.assertTrue(Option.isIdentical(actual, expected), "inverse equivalence test");
		Assertions.assertEquals(expected.getKey(), actual.getKey(), "key");

		for (char c : UUID.randomUUID().toString().toCharArray()) {
			actual.setValueSep(c);
			Assertions.assertEquals(c, actual.getValueSep().charValue());
			Assertions.assertEquals(expected.getArgumentName(), actual.getArgumentName(), "ValueName");
			Assertions.assertEquals(expected.getDescription(), actual.getDescription(), "Description");
			Assertions.assertEquals(expected.getLongOpt(), actual.getLongOpt(), "LongOpt");
			Assertions.assertEquals(expected.getShortOpt(), actual.getShortOpt(), "ShortOpt");
			Assertions.assertEquals(expected.getMaxArguments(), actual.getMaxArguments(), "MaxValueCount");
			Assertions.assertEquals(expected.isRequired(), actual.isRequired(), "Required");
			Assertions.assertEquals(expected.getMinArguments(), actual.getMinArguments(), "MinValueCount");
			Assertions.assertNotEquals(expected.getValueSep(), actual.getValueSep(), "ValueSep");
			Assertions.assertFalse(Option.isIdentical(expected, actual), "equivalence test");
			Assertions.assertFalse(Option.isIdentical(actual, expected), "inverse equivalence test");
			Assertions.assertNotEquals(expected.hashCode(), actual.hashCode());
			Assertions.assertEquals(expected.getKey(), actual.getKey(), "key");
		}
	}

	@Test
	public void testSetValueOptional() {
		OptionImpl expected = null;
		OptionImpl actual = null;

		expected = new OptionImpl();
		Assertions.assertNotNull(expected);
		expected.setArgumentName(UUID.randomUUID().toString());
		expected.setDescription(UUID.randomUUID().toString());
		expected.setLongOpt(UUID.randomUUID().toString());
		expected.setShortOpt('G');
		expected.setMaxArguments(UUID.randomUUID().hashCode());
		expected.setRequired(true);
		expected.setMinArguments(0);
		expected.setValueSep('|');

		actual = expected.clone();
		Assertions.assertNotNull(actual);
		Assertions.assertEquals(expected.getArgumentName(), actual.getArgumentName(), "ValueName");
		Assertions.assertEquals(expected.getDescription(), actual.getDescription(), "Description");
		Assertions.assertEquals(expected.getLongOpt(), actual.getLongOpt(), "LongOpt");
		Assertions.assertEquals(expected.getShortOpt(), actual.getShortOpt(), "ShortOpt");
		Assertions.assertEquals(expected.getMaxArguments(), actual.getMaxArguments(), "MaxValueCount");
		Assertions.assertEquals(expected.isRequired(), actual.isRequired(), "Required");
		Assertions.assertEquals(expected.getMinArguments(), actual.getMinArguments(), "MinValueCount");
		Assertions.assertEquals(expected.getValueSep(), actual.getValueSep(), "ValueSep");
		Assertions.assertTrue(Option.isIdentical(expected, actual), "equivalence test");
		Assertions.assertTrue(Option.isIdentical(actual, expected), "inverse equivalence test");
		Assertions.assertEquals(expected.getKey(), actual.getKey(), "key");

		actual.setMinArguments(1);
		Assertions.assertEquals(expected.getArgumentName(), actual.getArgumentName(), "ValueName");
		Assertions.assertEquals(expected.getDescription(), actual.getDescription(), "Description");
		Assertions.assertEquals(expected.getLongOpt(), actual.getLongOpt(), "LongOpt");
		Assertions.assertEquals(expected.getShortOpt(), actual.getShortOpt(), "ShortOpt");
		Assertions.assertEquals(expected.getMaxArguments(), actual.getMaxArguments(), "MaxValueCount");
		Assertions.assertEquals(expected.isRequired(), actual.isRequired(), "Required");
		Assertions.assertNotEquals(expected.getMinArguments(), actual.getMinArguments(), "MinValueCount");
		Assertions.assertEquals(expected.getValueSep(), actual.getValueSep(), "ValueSep");
		Assertions.assertFalse(Option.isIdentical(expected, actual), "equivalence test");
		Assertions.assertFalse(Option.isIdentical(actual, expected), "inverse equivalence test");
		Assertions.assertNotEquals(expected.hashCode(), actual.hashCode());

		actual.setMinArguments(0);
		Assertions.assertEquals(0, actual.getMinArguments());
		Assertions.assertEquals(expected.getArgumentName(), actual.getArgumentName(), "ValueName");
		Assertions.assertEquals(expected.getDescription(), actual.getDescription(), "Description");
		Assertions.assertEquals(expected.getLongOpt(), actual.getLongOpt(), "LongOpt");
		Assertions.assertEquals(expected.getShortOpt(), actual.getShortOpt(), "ShortOpt");
		Assertions.assertEquals(expected.getMaxArguments(), actual.getMaxArguments(), "MaxValueCount");
		Assertions.assertEquals(expected.isRequired(), actual.isRequired(), "Required");
		Assertions.assertEquals(expected.getValueSep(), actual.getValueSep(), "ValueSep");
		Assertions.assertTrue(Option.isIdentical(expected, actual), "equivalence test");
		Assertions.assertTrue(Option.isIdentical(actual, expected), "inverse equivalence test");
		Assertions.assertEquals(expected.getKey(), actual.getKey(), "key");
	}

	@Test
	public void testSetRequired() {
		OptionImpl expected = null;
		OptionImpl actual = null;

		expected = new OptionImpl();
		Assertions.assertNotNull(expected);
		expected.setArgumentName(UUID.randomUUID().toString());
		expected.setDescription(UUID.randomUUID().toString());
		expected.setLongOpt(UUID.randomUUID().toString());
		expected.setShortOpt('G');
		expected.setMaxArguments(UUID.randomUUID().hashCode());
		expected.setRequired(true);
		expected.setMinArguments(0);
		expected.setValueSep('|');

		actual = expected.clone();
		Assertions.assertNotNull(actual);
		Assertions.assertEquals(expected.getArgumentName(), actual.getArgumentName(), "ValueName");
		Assertions.assertEquals(expected.getDescription(), actual.getDescription(), "Description");
		Assertions.assertEquals(expected.getLongOpt(), actual.getLongOpt(), "LongOpt");
		Assertions.assertEquals(expected.getShortOpt(), actual.getShortOpt(), "ShortOpt");
		Assertions.assertEquals(expected.getMaxArguments(), actual.getMaxArguments(), "MaxValueCount");
		Assertions.assertEquals(expected.isRequired(), actual.isRequired(), "Required");
		Assertions.assertEquals(expected.getMinArguments(), actual.getMinArguments(), "MinValueCount");
		Assertions.assertEquals(expected.getValueSep(), actual.getValueSep(), "ValueSep");
		Assertions.assertTrue(Option.isIdentical(expected, actual), "equivalence test");
		Assertions.assertTrue(Option.isIdentical(actual, expected), "inverse equivalence test");
		Assertions.assertEquals(expected.getKey(), actual.getKey(), "key");

		actual.setRequired(false);
		Assertions.assertFalse(actual.isRequired());
		Assertions.assertEquals(expected.getArgumentName(), actual.getArgumentName(), "ValueName");
		Assertions.assertEquals(expected.getDescription(), actual.getDescription(), "Description");
		Assertions.assertEquals(expected.getLongOpt(), actual.getLongOpt(), "LongOpt");
		Assertions.assertEquals(expected.getShortOpt(), actual.getShortOpt(), "ShortOpt");
		Assertions.assertEquals(expected.getMaxArguments(), actual.getMaxArguments(), "MaxValueCount");
		Assertions.assertEquals(expected.getMinArguments(), actual.getMinArguments(), "MinValueCount");
		Assertions.assertEquals(expected.getValueSep(), actual.getValueSep(), "ValueSep");
		Assertions.assertFalse(Option.isIdentical(expected, actual), "equivalence test");
		Assertions.assertFalse(Option.isIdentical(actual, expected), "inverse equivalence test");
		Assertions.assertEquals(expected.getKey(), actual.getKey(), "key");

		actual.setRequired(true);
		Assertions.assertTrue(actual.isRequired());
		Assertions.assertEquals(expected.getArgumentName(), actual.getArgumentName(), "ValueName");
		Assertions.assertEquals(expected.getDescription(), actual.getDescription(), "Description");
		Assertions.assertEquals(expected.getLongOpt(), actual.getLongOpt(), "LongOpt");
		Assertions.assertEquals(expected.getShortOpt(), actual.getShortOpt(), "ShortOpt");
		Assertions.assertEquals(expected.getMaxArguments(), actual.getMaxArguments(), "MaxValueCount");
		Assertions.assertEquals(expected.getMinArguments(), actual.getMinArguments(), "MinValueCount");
		Assertions.assertEquals(expected.getValueSep(), actual.getValueSep(), "ValueSep");
		Assertions.assertTrue(Option.isIdentical(expected, actual), "equivalence test");
		Assertions.assertTrue(Option.isIdentical(actual, expected), "inverse equivalence test");
		Assertions.assertEquals(expected.getKey(), actual.getKey(), "key");
	}

	@Test
	public void testArgumentLimits() {
		OptionImpl expected = null;

		for (int min = 255; min >= -255; min--) {
			for (int max = 255; max >= -255; max--) {
				expected = new OptionImpl();

				expected.setMinArguments(min);
				if (min <= 0) {
					Assertions.assertEquals(0, expected.getMinArguments(), String.format("min=%d max=%d", min, max));
				} else {
					Assertions.assertEquals(min, expected.getMinArguments(), String.format("min=%d max=%d", min, max));
				}

				expected.setMaxArguments(max);
				if (max < 0) {
					Assertions.assertEquals(-1, expected.getMaxArguments(), String.format("min=%d max=%d", min, max));
				} else {
					Assertions.assertEquals(max, expected.getMaxArguments(), String.format("min=%d max=%d", min, max));
				}

				if (max > 0) {
					if (max < min) {
						Assertions.assertEquals(expected.getMaxArguments(), expected.getMinArguments(),
							String.format("min=%d max=%d", min, max));
					} else {
						Assertions.assertEquals(Math.max(0, min), expected.getMinArguments(),
							String.format("min=%d max=%d", min, max));
					}
				} else if (max < 0) {
					Assertions.assertEquals(Math.max(0, min), expected.getMinArguments(),
						String.format("min=%d max=%d", min, max));
				} else {
					Assertions.assertEquals(0, expected.getMinArguments(), String.format("min=%d max=%d", min, max));
				}
			}
		}

		for (int min = 255; min >= -255; min--) {
			for (int max = 255; max >= -255; max--) {
				expected = new OptionImpl();

				expected.setMaxArguments(max);
				if (max < 0) {
					Assertions.assertEquals(-1, expected.getMaxArguments(), String.format("min=%d max=%d", min, max));
				} else {
					Assertions.assertEquals(max, expected.getMaxArguments(), String.format("min=%d max=%d", min, max));
				}

				expected.setMinArguments(min);
				if (min <= 0) {
					Assertions.assertEquals(0, expected.getMinArguments(), String.format("min=%d max=%d", min, max));
				} else {
					Assertions.assertEquals(min, expected.getMinArguments(), String.format("min=%d max=%d", min, max));
				}

				if (min > 0) {
					if ((min > max) && (max >= 0)) {
						Assertions.assertEquals(expected.getMinArguments(), expected.getMaxArguments(),
							String.format("min=%d max=%d", min, max));
					} else {
						Assertions.assertEquals(Math.max(-1, max), expected.getMaxArguments(),
							String.format("min=%d max=%d", min, max));
					}
				} else {
					Assertions.assertEquals(Math.max(-1, max), expected.getMaxArguments(),
						String.format("min=%d max=%d", min, max));
				}
			}
		}

		for (int min = -255; min <= 255; min++) {
			for (int max = -255; max <= 255; max++) {
				expected = new OptionImpl();

				expected.setMinArguments(min);
				if (min <= 0) {
					Assertions.assertEquals(0, expected.getMinArguments(), String.format("min=%d max=%d", min, max));
				} else {
					Assertions.assertEquals(min, expected.getMinArguments(), String.format("min=%d max=%d", min, max));
				}

				expected.setMaxArguments(max);
				if (max < 0) {
					Assertions.assertEquals(-1, expected.getMaxArguments(), String.format("min=%d max=%d", min, max));
				} else {
					Assertions.assertEquals(max, expected.getMaxArguments(), String.format("min=%d max=%d", min, max));
				}

				if (max > 0) {
					if (max < min) {
						Assertions.assertEquals(expected.getMaxArguments(), expected.getMinArguments(),
							String.format("min=%d max=%d", min, max));
					} else {
						Assertions.assertEquals(Math.max(0, min), expected.getMinArguments(),
							String.format("min=%d max=%d", min, max));
					}
				} else if (max < 0) {
					Assertions.assertEquals(Math.max(0, min), expected.getMinArguments(),
						String.format("min=%d max=%d", min, max));
				} else {
					Assertions.assertEquals(0, expected.getMinArguments(), String.format("min=%d max=%d", min, max));
				}
			}
		}

		for (int min = -255; min <= 255; min++) {
			for (int max = -255; max <= 255; max++) {
				expected = new OptionImpl();

				expected.setMaxArguments(max);
				if (max < 0) {
					Assertions.assertEquals(-1, expected.getMaxArguments(), String.format("min=%d max=%d", min, max));
				} else {
					Assertions.assertEquals(max, expected.getMaxArguments(), String.format("min=%d max=%d", min, max));
				}

				expected.setMinArguments(min);
				if (min <= 0) {
					Assertions.assertEquals(0, expected.getMinArguments(), String.format("min=%d max=%d", min, max));
				} else {
					Assertions.assertEquals(min, expected.getMinArguments(), String.format("min=%d max=%d", min, max));
				}

				if (min > 0) {
					if ((min > max) && (max >= 0)) {
						Assertions.assertEquals(expected.getMinArguments(), expected.getMaxArguments(),
							String.format("min=%d max=%d", min, max));
					} else {
						Assertions.assertEquals(Math.max(-1, max), expected.getMaxArguments(),
							String.format("min=%d max=%d", min, max));
					}
				} else {
					Assertions.assertEquals(Math.max(-1, max), expected.getMaxArguments(),
						String.format("min=%d max=%d", min, max));
				}
			}
		}

		// Reset...
		for (int min = -255; min <= 255; min++) {
			for (int max = -255; max <= 255; max++) {
				expected = new OptionImpl();
				expected.setArgumentLimits(min, max);
				if (min <= 0) {
					Assertions.assertEquals(0, expected.getMinArguments());
				} else {
					Assertions.assertEquals(min, expected.getMinArguments());
				}
				if (max < 0) {
					Assertions.assertEquals(-1, expected.getMaxArguments());
				} else if (min <= max) {
					Assertions.assertEquals(max, expected.getMaxArguments());
				} else {
					Assertions.assertEquals(expected.getMinArguments(), expected.getMaxArguments());
				}
			}
		}

		// Reset...
		for (int max = -255; max <= 255; max++) {
			for (int min = -255; min <= 255; min++) {
				expected = new OptionImpl();
				expected.setArgumentLimits(min, max);
				if (min <= 0) {
					Assertions.assertEquals(0, expected.getMinArguments());
				} else {
					Assertions.assertEquals(min, expected.getMinArguments());
				}
				if (max < 0) {
					Assertions.assertEquals(-1, expected.getMaxArguments());
				} else if (min <= max) {
					Assertions.assertEquals(max, expected.getMaxArguments());
				} else {
					Assertions.assertEquals(expected.getMinArguments(), expected.getMaxArguments());
				}
			}
		}

		// Reset...
		for (int l = -255; l <= 255; l++) {
			expected = new OptionImpl();
			expected.setArgumentLimits(l);
			if (l <= 0) {
				Assertions.assertEquals(0, expected.getMinArguments());
			} else {
				Assertions.assertEquals(l, expected.getMinArguments());
			}
			if (l < 0) {
				Assertions.assertEquals(-1, expected.getMaxArguments());
			} else {
				Assertions.assertEquals(l, expected.getMaxArguments());
			}
		}
	}
}