package com.delta.cmsmf.datastore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

import com.armedia.commons.utilities.Tools;
import com.documentum.fc.common.IDfValue;

public class DataPropertyTest {

	private static final IDfValue[] NO_VALUES = new IDfValue[0];

	@Test
	public void testDataPropertyStringDataTypeIDfValueArray() {
		final String name = "attributeName";
		final DataType type = DataType.DF_BOOLEAN;
		DataProperty prop = null;

		try {
			new DataProperty(null, type, DataPropertyTest.NO_VALUES);
			Assert.fail("Did not fail with null name");
		} catch (IllegalArgumentException e) {
			// All is well
		}
		try {
			new DataProperty(name, null, DataPropertyTest.NO_VALUES);
			Assert.fail("Did not fail with null type");
		} catch (IllegalArgumentException e) {
			// All is well
		}

		List<IDfValue> values = new ArrayList<IDfValue>();
		for (int i = 0; i < 10; i++) {
			UUID uuid = UUID.randomUUID();
			values.add(DfValueFactory.newStringValue(uuid.toString()));
		}
		IDfValue[] valueArr = values.toArray(DataPropertyTest.NO_VALUES);

		prop = new DataProperty(name, type, valueArr);
		Assert.assertTrue(prop.isRepeating());
		Assert.assertEquals(valueArr.length, prop.getValueCount());
		Assert.assertEquals(valueArr.length > 0, prop.hasValues());
		for (int i = 0; i < valueArr.length; i++) {
			Assert.assertEquals(valueArr[i], prop.getValue(i));
		}
	}

	@Test
	public void testDataPropertyStringDataTypeCollectionOfIDfValue() {
		final String name = "attributeName";
		final DataType type = DataType.DF_BOOLEAN;
		DataProperty prop = null;

		try {
			new DataProperty(null, type, DataPropertyTest.NO_VALUES);
			Assert.fail("Did not fail with null name");
		} catch (IllegalArgumentException e) {
			// All is well
		}
		try {
			new DataProperty(name, null, DataPropertyTest.NO_VALUES);
			Assert.fail("Did not fail with null type");
		} catch (IllegalArgumentException e) {
			// All is well
		}

		List<IDfValue> values = new ArrayList<IDfValue>();
		for (int i = 0; i < 10; i++) {
			UUID uuid = UUID.randomUUID();
			values.add(DfValueFactory.newStringValue(uuid.toString()));
		}

		prop = new DataProperty(name, type, values);
		Assert.assertTrue(prop.isRepeating());
		Assert.assertEquals(values.size(), prop.getValueCount());
		Assert.assertNotEquals(values.isEmpty(), prop.hasValues());
		for (int i = 0; i < values.size(); i++) {
			Assert.assertEquals(values.get(i), prop.getValue(i));
		}

		values = null;
		prop = new DataProperty(name, type, values);
		Assert.assertTrue(prop.isRepeating());
		Assert.assertEquals(0, prop.getValueCount());
		Assert.assertFalse(prop.hasValues());
	}

	@Test
	public void testDataPropertyStringDataTypeBooleanIDfValueArray() {
		final String name = "attributeName";
		final DataType type = DataType.DF_BOOLEAN;
		DataProperty prop = null;

		try {
			new DataProperty(null, type, false, DataPropertyTest.NO_VALUES);
			Assert.fail("Did not fail with null name");
		} catch (IllegalArgumentException e) {
			// All is well
		}
		try {
			new DataProperty(null, type, true, DataPropertyTest.NO_VALUES);
			Assert.fail("Did not fail with null name");
		} catch (IllegalArgumentException e) {
			// All is well
		}
		try {
			new DataProperty(name, null, false, DataPropertyTest.NO_VALUES);
			Assert.fail("Did not fail with null type");
		} catch (IllegalArgumentException e) {
			// All is well
		}
		try {
			new DataProperty(name, null, true, DataPropertyTest.NO_VALUES);
			Assert.fail("Did not fail with null type");
		} catch (IllegalArgumentException e) {
			// All is well
		}

		List<IDfValue> values = new ArrayList<IDfValue>();
		for (int i = 0; i < 10; i++) {
			UUID uuid = UUID.randomUUID();
			values.add(DfValueFactory.newStringValue(uuid.toString()));
		}
		IDfValue[] valueArr = values.toArray(DataPropertyTest.NO_VALUES);

		prop = new DataProperty(name, type, false, valueArr);
		Assert.assertFalse(prop.isRepeating());
		Assert.assertEquals(1, prop.getValueCount());
		Assert.assertTrue(prop.hasValues());
		Assert.assertEquals(valueArr[0], prop.getValue());

		prop = new DataProperty(name, type, true, valueArr);
		Assert.assertTrue(prop.isRepeating());
		Assert.assertEquals(valueArr.length, prop.getValueCount());
		Assert.assertEquals(valueArr.length > 0, prop.hasValues());
		for (int i = 0; i < valueArr.length; i++) {
			Assert.assertEquals(valueArr[i], prop.getValue(i));
		}
	}

	@Test
	public void testDataPropertyStringDataTypeBooleanCollectionOfIDfValue() {
		final String name = "attributeName";
		final DataType type = DataType.DF_BOOLEAN;
		DataProperty prop = null;

		try {
			new DataProperty(null, type, false, DataPropertyTest.NO_VALUES);
			Assert.fail("Did not fail with null name");
		} catch (IllegalArgumentException e) {
			// All is well
		}
		try {
			new DataProperty(null, type, true, DataPropertyTest.NO_VALUES);
			Assert.fail("Did not fail with null name");
		} catch (IllegalArgumentException e) {
			// All is well
		}
		try {
			new DataProperty(name, null, false, DataPropertyTest.NO_VALUES);
			Assert.fail("Did not fail with null type");
		} catch (IllegalArgumentException e) {
			// All is well
		}
		try {
			new DataProperty(name, null, true, DataPropertyTest.NO_VALUES);
			Assert.fail("Did not fail with null type");
		} catch (IllegalArgumentException e) {
			// All is well
		}

		List<IDfValue> values = new ArrayList<IDfValue>();
		for (int i = 0; i < 10; i++) {
			UUID uuid = UUID.randomUUID();
			values.add(DfValueFactory.newStringValue(uuid.toString()));
		}

		prop = new DataProperty(name, type, false, values);
		Assert.assertFalse(prop.isRepeating());
		Assert.assertEquals(1, prop.getValueCount());
		Assert.assertTrue(prop.hasValues());
		Assert.assertEquals(values.get(0), prop.getValue());

		prop = new DataProperty(name, type, true, values);
		Assert.assertTrue(prop.isRepeating());
		Assert.assertEquals(values.size(), prop.getValueCount());
		Assert.assertNotEquals(values.isEmpty(), prop.hasValues());
		for (int i = 0; i < values.size(); i++) {
			Assert.assertEquals(values.get(i), prop.getValue(i));
		}

		values = null;
		prop = new DataProperty(name, type, false, values);
		Assert.assertFalse(prop.isRepeating());
		Assert.assertEquals(1, prop.getValueCount());
		Assert.assertTrue(prop.hasValues());
		Assert.assertEquals(type.getValue(type.getNullValue()), type.getValue(prop.getValue()));

		prop = new DataProperty(name, type, true, values);
		Assert.assertTrue(prop.isRepeating());
		Assert.assertEquals(0, prop.getValueCount());
		Assert.assertFalse(prop.hasValues());
	}

	@Test
	public void testGetName() {
		final DataType type = DataType.DF_BOOLEAN;
		for (int i = 0; i < 10; i++) {
			UUID uuid = UUID.randomUUID();
			DataProperty prop = new DataProperty(uuid.toString(), type, false);
			Assert.assertEquals(uuid.toString(), prop.getName());
		}
	}

	@Test
	public void testGetType() {
		final String name = "attributeName";
		for (final DataType type : DataType.values()) {
			try {
				DataProperty prop = new DataProperty(name, type, false);
				Assert.assertEquals(type, prop.getType());
			} catch (UnsupportedOperationException e) {
				String msg = e.getMessage();
				Assert.assertTrue((msg != null) && msg.endsWith("DF_UNDEFINED"));
			}
		}
		try {
			new DataProperty(name, null, false);
			Assert.fail("Did not fail with null type");
		} catch (IllegalArgumentException e) {
			// All is well
		}
	}

	@Test
	public void testIsRepeating() {
		final String name = "attributeName";
		final DataType type = DataType.DF_BOOLEAN;
		DataProperty prop = null;
		prop = new DataProperty(name, type, false);
		Assert.assertFalse(prop.isRepeating());
		prop = new DataProperty(name, type, true);
		Assert.assertTrue(prop.isRepeating());
	}

	@Test
	public void testGetValueCount() {
		final String name = "attributeName";
		final DataType type = DataType.DF_INTEGER;
		final List<IDfValue> values = new ArrayList<IDfValue>();
		DataProperty prop = null;
		for (int i = 10; i < 100; i++) {
			values.clear();
			for (int c = 0; c < i; c++) {
				if ((c % 5) == 0) {
					values.add(DfValueFactory.newIntValue((c * 100) + i));
				} else {
					values.add(null);
				}
			}
			// First, try single-valued
			prop = new DataProperty(name, type, false, values);
			Assert.assertEquals(1, prop.getValueCount());

			// Now, try multi-valued
			prop = new DataProperty(name, type, true, values);
			Assert.assertEquals(values.size(), prop.getValueCount());

			prop = new DataProperty(name, type, true, values.toArray(DataPropertyTest.NO_VALUES));
			Assert.assertEquals(values.size(), prop.getValueCount());
		}
	}

	@Test
	public void testHasValues() {
		final String name = "attributeName";
		final DataType type = DataType.DF_INTEGER;
		final List<IDfValue> values = new ArrayList<IDfValue>();
		DataProperty prop = null;
		for (int i = 10; i < 100; i++) {
			values.clear();
			for (int c = 0; c < i; c++) {
				if ((c % 5) == 0) {
					values.add(DfValueFactory.newIntValue((c * 100) + i));
				} else {
					values.add(null);
				}
			}
			// First, try single-valued
			prop = new DataProperty(name, type, false, values);
			Assert.assertTrue(prop.hasValues());

			// Now, try multi-valued
			prop = new DataProperty(name, type, true, values);
			Assert.assertNotEquals(values.isEmpty(), prop.hasValues());

			prop = new DataProperty(name, type, true, values.toArray(DataPropertyTest.NO_VALUES));
			Assert.assertNotEquals(values.isEmpty(), prop.hasValues());
		}
	}

	@Test
	public void testSetValuesIDfValueArray() {
		final String name = "attributeName";
		final DataType type = DataType.DF_INTEGER;
		final IDfValue NULL = type.getNullValue();
		final List<IDfValue> values = new ArrayList<IDfValue>();
		DataProperty prop = null;

		for (int i = 0; i < 100; i++) {
			values.clear();
			for (int c = 0; c < i; c++) {
				values.add(DfValueFactory.newIntValue((c * 100) + i));
			}

			// First, try single-valued
			prop = new DataProperty(name, type, false);
			Assert.assertEquals(NULL, prop.getValue());
			prop.setValues(values.toArray(DataPropertyTest.NO_VALUES));
			Assert.assertEquals(values.isEmpty() ? NULL : values.get(0), prop.getValue());
			prop.setValues(DataPropertyTest.NO_VALUES);
			Assert.assertEquals(NULL, prop.getValue());

			// Now, try multi-valued
			prop = new DataProperty(name, type, true);
			Assert.assertFalse(prop.hasValues());
			Assert.assertEquals(0, prop.getValueCount());
			prop.setValues(values.toArray(DataPropertyTest.NO_VALUES));
			Assert.assertEquals(i > 0, prop.hasValues());
			Assert.assertEquals(values.size(), prop.getValueCount());
			for (int v = 0; v < values.size(); v++) {
				IDfValue pVal = prop.getValue(v);
				IDfValue oVal = Tools.coalesce(values.get(v), NULL);
				Assert.assertNotNull(pVal);
				Assert.assertEquals(oVal, pVal);
			}
			prop.setValues(DataPropertyTest.NO_VALUES);
			Assert.assertFalse(prop.hasValues());
			Assert.assertEquals(0, prop.getValueCount());
		}
	}

	@Test
	public void testSetValuesCollectionOfIDfValue() {
		final String name = "attributeName";
		final DataType type = DataType.DF_INTEGER;
		final IDfValue NULL = type.getNullValue();
		final List<IDfValue> values = new ArrayList<IDfValue>();
		final List<IDfValue> EMPTY = Collections.emptyList();
		final List<IDfValue> NULL_LIST = null;
		DataProperty prop = null;

		for (int i = 0; i < 100; i++) {
			values.clear();
			for (int c = 0; c < i; c++) {
				values.add(DfValueFactory.newIntValue((c * 100) + i));
			}

			// First, try single-valued
			prop = new DataProperty(name, type, false);
			Assert.assertEquals(NULL, prop.getValue());
			prop.setValues(values);
			Assert.assertEquals(values.isEmpty() ? NULL : values.get(0), prop.getValue());
			prop.setValues(EMPTY);
			Assert.assertEquals(NULL, prop.getValue());

			prop.setValues(values);
			Assert.assertEquals(values.isEmpty() ? NULL : values.get(0), prop.getValue());
			prop.setValues(NULL_LIST);
			Assert.assertTrue(prop.hasValues());
			Assert.assertEquals(1, prop.getValueCount());

			// Now, try multi-valued
			prop = new DataProperty(name, type, true);
			Assert.assertFalse(prop.hasValues());
			Assert.assertEquals(0, prop.getValueCount());
			prop.setValues(values);
			Assert.assertEquals(i > 0, prop.hasValues());
			Assert.assertEquals(values.size(), prop.getValueCount());
			for (int v = 0; v < values.size(); v++) {
				IDfValue pVal = prop.getValue(v);
				IDfValue oVal = Tools.coalesce(values.get(v), NULL);
				Assert.assertNotNull(pVal);
				Assert.assertEquals(oVal, pVal);
			}
			prop.setValues(EMPTY);
			Assert.assertFalse(prop.hasValues());
			Assert.assertEquals(0, prop.getValueCount());

			prop.setValues(values);
			Assert.assertEquals(i > 0, prop.hasValues());
			Assert.assertEquals(values.size(), prop.getValueCount());
			prop.setValues(NULL_LIST);
			Assert.assertFalse(prop.hasValues());
			Assert.assertEquals(0, prop.getValueCount());
		}
	}

	@Test
	public void testGetValues() {
		final String name = "attributeName";
		final DataType type = DataType.DF_INTEGER;
		final IDfValue NULL = type.getNullValue();
		final List<IDfValue> values = new ArrayList<IDfValue>();
		DataProperty prop = null;
		List<IDfValue> actualValues = null;

		for (int i = 0; i < 100; i++) {
			values.clear();
			for (int c = 0; c < i; c++) {
				values.add(DfValueFactory.newIntValue((c * 100) + i));
			}

			// First, try single-valued
			prop = new DataProperty(name, type, false, values);
			actualValues = prop.getValues();
			Assert.assertFalse(actualValues.isEmpty());
			Assert.assertEquals(1, actualValues.size());
			Assert.assertEquals(prop.getValueCount(), actualValues.size());
			Assert.assertEquals(values.isEmpty() ? NULL : values.get(0), actualValues.get(0));

			// Now, try multi-valued
			prop = new DataProperty(name, type, true, values);
			actualValues = prop.getValues();
			Assert.assertEquals(i <= 0, actualValues.isEmpty());
			Assert.assertEquals(values.size(), actualValues.size());
			Assert.assertEquals(prop.getValueCount(), actualValues.size());

			for (int v = 0; v < values.size(); v++) {
				IDfValue pVal = actualValues.get(v);
				IDfValue oVal = Tools.coalesce(values.get(v), NULL);
				Assert.assertNotNull(pVal);
				Assert.assertEquals(oVal, pVal);
			}
		}
	}

	@Test
	public void testAddValue() {
		final String name = "attributeName";
		final DataType type = DataType.DF_INTEGER;
		final IDfValue NULL = type.getNullValue();
		DataProperty prop = null;

		prop = new DataProperty(name, type, false);
		try {
			prop.addValue(null);
			Assert.fail("Did not fail when adding value to a single-valued property");
		} catch (UnsupportedOperationException e) {
			// failed
		}
		try {
			prop.addValue(DfValueFactory.newIntValue(0));
			Assert.fail("Did not fail when adding value to a single-valued property");
		} catch (UnsupportedOperationException e) {
			// failed
		}

		prop = new DataProperty(name, type, true);
		for (int i = 0; i < 100; i++) {
			final IDfValue next;
			// Every 3rd value will be null
			if ((i % 3) == 0) {
				next = DfValueFactory.newIntValue(i);
			} else {
				next = null;
			}

			Assert.assertEquals(i > 0, prop.hasValues());
			Assert.assertEquals(i, prop.getValueCount());
			prop.addValue(next);
			Assert.assertTrue(prop.hasValues());
			Assert.assertNotEquals(i, prop.getValueCount());
			Assert.assertEquals(1, prop.getValueCount() - i);
			final IDfValue actual = prop.getValue(i);
			Assert.assertNotNull(actual);
			if (next == null) {
				Assert.assertEquals(NULL, actual);
			} else {
				Assert.assertEquals(next, actual);
			}
		}
	}

	@Test
	public void testSetValue() {
		final String name = "attributeName";
		final DataType type = DataType.DF_INTEGER;
		final IDfValue NULL = type.getNullValue();
		DataProperty prop = null;

		// First, single-valued
		prop = new DataProperty(name, type, false);
		prop.setValue(null);
		Assert.assertNotNull(prop.getValue());
		Assert.assertEquals(NULL, prop.getValue());

		prop.setValue(NULL);
		Assert.assertEquals(NULL, prop.getValue());

		for (int i = 10; i < 100; i++) {
			final IDfValue next = DfValueFactory.newIntValue(i);
			prop.setValue(next);
			Assert.assertEquals(next, prop.getValue());
		}

		// Now, multi-valued
		prop = new DataProperty(name, type, true);
		for (int i = 10; i < 100; i++) {
			prop.clearValue();
			for (int c = 0; c < i; c++) {
				if ((c % 5) == 0) {
					prop.addValue(DfValueFactory.newIntValue((c * 100) + i));
				} else {
					prop.addValue(null);
				}
			}

			IDfValue next = DfValueFactory.newIntValue(System.currentTimeMillis());
			Assert.assertEquals(i > 0, prop.hasValues());
			Assert.assertEquals(i, prop.getValueCount());
			if (i > 0) {
				Assert.assertNotEquals(next, prop.getValue());
			}

			prop.setValue(next);
			Assert.assertTrue(prop.hasValues());
			Assert.assertEquals(next, prop.getValue());
			Assert.assertEquals(1, prop.getValueCount());
		}
	}

	@Test
	public void testClearValue() {
		final String name = "attributeName";
		final DataType type = DataType.DF_INTEGER;
		final IDfValue NULL = type.getNullValue();
		DataProperty prop = null;

		// First, single-valued
		prop = new DataProperty(name, type, false);
		for (int i = 1; i <= 100; i++) {
			final IDfValue next = DfValueFactory.newIntValue(i);
			prop.setValue(next);
			Assert.assertEquals(next, prop.getValue());
			Assert.assertNotEquals(NULL, prop.getValue());
			prop.clearValue();
			Assert.assertEquals(NULL, prop.getValue());
		}

		// Now, multi-valued
		prop = new DataProperty(name, type, true);
		for (int i = 10; i < 100; i++) {
			prop.clearValue();
			for (int c = 0; c < i; c++) {
				prop.addValue(DfValueFactory.newIntValue((c * 100) + i));
			}

			Assert.assertEquals(i > 0, prop.hasValues());
			Assert.assertEquals(i, prop.getValueCount());
			prop.clearValue();
			Assert.assertFalse(prop.hasValues());
			Assert.assertEquals(0, prop.getValueCount());
		}
	}

	@Test
	public void testRemoveValue() {
		final String name = "attributeName";
		final DataType type = DataType.DF_INTEGER;
		final IDfValue NULL = type.getNullValue();
		final List<IDfValue> values = new ArrayList<IDfValue>();
		DataProperty prop = null;

		// First, try single-valued
		prop = new DataProperty(name, type, false, values);
		Assert.assertNotNull(prop.getValue());
		IDfValue value = DfValueFactory.newIntValue(System.currentTimeMillis());
		prop.setValue(value);
		Assert.assertEquals(1, prop.getValueCount());
		Assert.assertTrue(prop.hasValues());
		Assert.assertEquals(value, prop.getValue());
		prop.removeValue(0);
		Assert.assertEquals(1, prop.getValueCount());
		Assert.assertTrue(prop.hasValues());
		Assert.assertEquals(NULL, prop.getValue());
		try {
			prop.removeValue(1);
			Assert.fail("Did not fail with a non-zero index for single-valued");
		} catch (ArrayIndexOutOfBoundsException e) {
			// All is well
		}

		// Now, try multi-valued
		Random r = new Random(System.currentTimeMillis());
		for (int i = 10; i < 100; i++) {
			values.clear();
			for (int c = 0; c < i; c++) {
				if ((c % 5) == 0) {
					values.add(DfValueFactory.newIntValue((c * 100) + i));
				} else {
					values.add(NULL);
				}
			}

			prop = new DataProperty(name, type, true, values);
			// Remove roughly half from both lists, then compare them, they should be identical
			for (int v = 0; v < (i / 2); v++) {
				int idx = r.nextInt(values.size());
				values.remove(idx);
				prop.removeValue(idx);
				if (values.isEmpty()) {
					break;
				}
			}

			Assert.assertEquals(values.size(), prop.getValueCount());
			for (int v = 0; v < values.size(); v++) {
				Assert.assertEquals(prop.getValue(v), Tools.coalesce(values.get(v), NULL));
			}
		}
	}

	@Test
	public void testGetValueInt() {
		final String name = "attributeName";
		final DataType type = DataType.DF_INTEGER;
		final IDfValue NULL = type.getNullValue();
		final List<IDfValue> values = new ArrayList<IDfValue>();
		DataProperty prop = null;
		for (int i = 0; i < 100; i++) {
			values.clear();
			for (int c = 0; c < i; c++) {
				if ((c % 5) == 0) {
					values.add(DfValueFactory.newIntValue((c * 100) + i));
				} else {
					values.add(null);
				}
			}
			// First, try single-valued
			prop = new DataProperty(name, type, false, values);
			IDfValue single = NULL;
			if (!values.isEmpty()) {
				single = Tools.coalesce(values.get(0), NULL);
			}
			Assert.assertNotNull(prop.getValue(0));
			Assert.assertEquals(single, prop.getValue(0));
			Assert.assertEquals(single, prop.getValue(-1)); // test folding to 0
			Assert.assertEquals(single, prop.getValue(-2)); // test folding to 0
			Assert.assertEquals(single, prop.getValue(Integer.MIN_VALUE)); // test folding to 0

			// Now, try multi-valued
			prop = new DataProperty(name, type, true, values);
			for (int v = 0; v < values.size(); v++) {
				IDfValue pVal = prop.getValue(v);
				IDfValue oVal = Tools.coalesce(values.get(v), NULL);
				Assert.assertNotNull(pVal);
				Assert.assertEquals(oVal, pVal);
			}
			try {
				prop.getValue(prop.getValueCount());
				Assert.fail("Did not fail when requesting an out-of-bounds value");
			} catch (ArrayIndexOutOfBoundsException e) {
				// all is well
			}

			prop = new DataProperty(name, type, true, values.toArray(DataPropertyTest.NO_VALUES));
			for (int v = 0; v < values.size(); v++) {
				IDfValue pVal = prop.getValue(v);
				IDfValue oVal = Tools.coalesce(values.get(v), NULL);
				Assert.assertNotNull(pVal);
				Assert.assertEquals(oVal, pVal);
			}
			try {
				prop.getValue(prop.getValueCount());
				Assert.fail("Did not fail when requesting an out-of-bounds value");
			} catch (ArrayIndexOutOfBoundsException e) {
				// all is well
			}
		}
	}

	@Test
	public void testGetValue() {
		final String name = "attributeName";
		final DataType type = DataType.DF_INTEGER;
		final IDfValue NULL = type.getNullValue();
		final List<IDfValue> values = new ArrayList<IDfValue>();
		DataProperty prop = null;
		for (int i = 0; i < 10; i++) {
			values.clear();
			for (int c = 0; c < i; c++) {
				if (((i % 2) == 0) && ((c % 5) == 0)) {
					values.add(DfValueFactory.newIntValue((c * 100) + i));
				} else {
					values.add(null);
				}
			}
			// First, try single-valued
			prop = new DataProperty(name, type, false, values);
			IDfValue single = type.getNullValue();
			if (!values.isEmpty()) {
				single = Tools.coalesce(values.get(0), NULL);
			}
			Assert.assertNotNull(prop.getValue());
			Assert.assertEquals(single, prop.getValue());

			// Now, try multi-valued
			prop = new DataProperty(name, type, true, values);
			for (int v = 0; v < values.size(); v++) {
				IDfValue pVal = prop.getValue();
				IDfValue oVal = Tools.coalesce(values.get(0), NULL);
				Assert.assertNotNull(pVal);
				Assert.assertEquals(oVal, pVal);
			}

			prop = new DataProperty(name, type, true, values.toArray(DataPropertyTest.NO_VALUES));
			for (int v = 0; v < values.size(); v++) {
				IDfValue pVal = prop.getValue();
				IDfValue oVal = Tools.coalesce(values.get(0), NULL);
				Assert.assertNotNull(pVal);
				Assert.assertEquals(oVal, pVal);
			}
		}
	}

	@Test
	public void testIsSame() {
		String[] names = {
			UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString(),
			UUID.randomUUID().toString(), UUID.randomUUID().toString()
		};
		boolean[] repeating = {
			false, true
		};

		for (final String aName : names) {
			for (final DataType aType : DataType.values()) {
				if (aType == DataType.DF_UNDEFINED) {
					continue;
				}
				for (final boolean aRep : repeating) {
					final DataProperty aProp = new DataProperty(aName, aType, aRep,
						DfValueFactory.newStringValue(String.format("A = %s-%s-%s", aName, aType, aRep)));

					Assert.assertFalse(aProp.isSame(null));
					Assert.assertTrue(aProp.isSame(aProp));

					for (final String bName : names) {
						for (final DataType bType : DataType.values()) {
							if (bType == DataType.DF_UNDEFINED) {
								continue;
							}
							for (final boolean bRep : repeating) {
								final DataProperty bProp = new DataProperty(bName, bType, bRep,
									DfValueFactory.newStringValue(String.format("A = %s-%s-%s", bName, bType, bRep)));
								Assert.assertTrue(bProp.isSame(bProp));

								boolean allSame = Tools.equals(aName, bName);
								allSame &= Tools.equals(aType, bType);
								allSame &= Tools.equals(aRep, bRep);

								Assert.assertEquals(allSame, aProp.isSame(bProp));
								Assert.assertEquals(aProp.isSame(bProp), bProp.isSame(aProp));
							}
						}
					}
				}
			}
		}
	}

	@Test
	public void testIterator() {
		final String name = "attributeName";
		final DataType type = DataType.DF_INTEGER;
		final IDfValue NULL = type.getNullValue();
		final List<IDfValue> values = new ArrayList<IDfValue>();
		DataProperty prop = null;

		// First, single-valued
		final IDfValue single = DfValueFactory.newIntValue(System.currentTimeMillis());
		Iterator<IDfValue> singleIt = null;
		prop = new DataProperty(name, type, false);

		singleIt = prop.iterator();
		Assert.assertTrue(singleIt.hasNext());
		try {
			singleIt.remove();
			Assert.fail("Did not fail when invoking remove() before iterating over the single element");
		} catch (IllegalStateException e) {
			// All is well
		}

		Assert.assertEquals(NULL, singleIt.next());
		try {
			singleIt.next();
			Assert.fail("Did not fail when invoking next() after iterating over the single element");
		} catch (NoSuchElementException e) {
			// All is well
		}
		Assert.assertFalse(singleIt.hasNext());

		prop.setValue(single);
		Assert.assertEquals(single, prop.getValue());
		singleIt = prop.iterator();
		Assert.assertTrue(singleIt.hasNext());
		Assert.assertEquals(single, singleIt.next());
		try {
			singleIt.next();
			Assert.fail("Did not fail when invoking next() after iterating over the single element");
		} catch (NoSuchElementException e) {
			// All is well
		}
		Assert.assertFalse(singleIt.hasNext());

		prop.setValue(single);
		Assert.assertEquals(single, prop.getValue());
		singleIt = prop.iterator();
		Assert.assertTrue(singleIt.hasNext());
		Assert.assertEquals(single, singleIt.next());
		singleIt.remove();
		try {
			singleIt.remove();
			Assert.fail("Did not fail when invoking remove() after removing the single element");
		} catch (IllegalStateException e) {
			// All is well
		}
		Assert.assertFalse(singleIt.hasNext());
		Assert.assertEquals(NULL, prop.getValue());

		// Now, try multi-valued
		for (int i = 10; i < 100; i++) {
			values.clear();
			for (int c = 0; c < i; c++) {
				if ((c % 5) == 0) {
					values.add(DfValueFactory.newIntValue((c * 100) + i));
				} else {
					values.add(NULL);
				}
			}

			prop = new DataProperty(name, type, true, values);
			Iterator<IDfValue> a = values.iterator();
			Iterator<IDfValue> b = prop.iterator();
			int j = 0;
			while (true) {
				j++;
				boolean aNext = a.hasNext();
				boolean bNext = b.hasNext();
				Assert.assertEquals(aNext, bNext);
				if (!aNext || !bNext) {
					break;
				}

				// We never get nulls, so we have to defend against that here
				Assert.assertEquals(a.next(), b.next());
				if ((j % 2) == 0) {
					a.remove();
					b.remove();
				}
			}
			Assert.assertEquals(values.size(), prop.getValueCount());
			Assert.assertNotEquals(values.isEmpty(), prop.hasValues());
			Assert.assertEquals(values, prop.getValues());
		}
	}
}