package com.delta.cmsmf.datastore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

import com.documentum.fc.common.DfId;
import com.documentum.fc.common.DfTime;
import com.documentum.fc.common.IDfValue;

public class DataTypeTest {

	private static final Map<DataType, String> NULL_ENCODING;
	private static final Map<DataType, IDfValue> NULL_VALUE;
	private static final Map<DataType, List<IDfValue>> VALUE_LIST;
	private static final Map<DataType, List<String>> ENCODING_LIST;

	static {
		Map<DataType, String> encodings = new EnumMap<DataType, String>(DataType.class);
		encodings.put(DataType.DF_BOOLEAN, "false");
		encodings.put(DataType.DF_INTEGER, "0");
		encodings.put(DataType.DF_STRING, "");
		encodings.put(DataType.DF_ID, DfId.DF_NULLID_STR);
		encodings.put(DataType.DF_TIME, DfTime.DF_NULLDATE_STR);
		encodings.put(DataType.DF_DOUBLE, "0x0.0p0");
		NULL_ENCODING = Collections.unmodifiableMap(encodings);

		Map<DataType, IDfValue> nulls = new EnumMap<DataType, IDfValue>(DataType.class);
		nulls.put(DataType.DF_BOOLEAN, DfValueFactory.newBooleanValue(false));
		nulls.put(DataType.DF_INTEGER, DfValueFactory.newIntValue(0));
		nulls.put(DataType.DF_STRING, DfValueFactory.newStringValue(""));
		nulls.put(DataType.DF_ID, DfValueFactory.newIdValue(DfId.DF_NULLID));
		nulls.put(DataType.DF_TIME, DfValueFactory.newTimeValue(DfTime.DF_NULLDATE));
		nulls.put(DataType.DF_DOUBLE, DfValueFactory.newDoubleValue(0.0));
		NULL_VALUE = Collections.unmodifiableMap(nulls);

		Map<DataType, List<IDfValue>> lists = new EnumMap<DataType, List<IDfValue>>(DataType.class);

		List<IDfValue> list = null;
		list = Arrays.asList(DfValueFactory.newBooleanValue(false), DfValueFactory.newBooleanValue(true));
		lists.put(DataType.DF_BOOLEAN, Collections.unmodifiableList(list));

		list = new ArrayList<IDfValue>();
		for (int i = -100; i < 100; i++) {
			list.add(DfValueFactory.newIntValue(i));
			list.add(DfValueFactory.newIntValue((i < 0 ? Integer.MIN_VALUE : Integer.MAX_VALUE) - i));
		}
		lists.put(DataType.DF_INTEGER, Collections.unmodifiableList(list));

		list = new ArrayList<IDfValue>();
		for (int i = 0; i < 100; i++) {
			list.add(DfValueFactory.newStringValue(UUID.randomUUID().toString()));
		}
		lists.put(DataType.DF_STRING, Collections.unmodifiableList(list));

		list = new ArrayList<IDfValue>();
		for (int i = -100; i < 100; i++) {
			list.add(DfValueFactory.newIdValue(String.format("%016x", i)));
			long L = (i < 0 ? Long.MIN_VALUE : Long.MAX_VALUE) - i;
			list.add(DfValueFactory.newIdValue(String.format("%016x", L)));
		}
		lists.put(DataType.DF_ID, Collections.unmodifiableList(list));

		list = new ArrayList<IDfValue>();
		for (int i = 0; i < 100; i++) {
			list.add(DfValueFactory.newTimeValue(new Date(i)));
			list.add(DfValueFactory.newTimeValue(new Date(Long.MAX_VALUE - i)));
		}
		lists.put(DataType.DF_TIME, Collections.unmodifiableList(list));

		list = new ArrayList<IDfValue>();
		double[] DOUBLES = {
			0.0, 1.0, 0.1, Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.MIN_VALUE,
			Double.MAX_VALUE
		};
		for (double d : DOUBLES) {
			list.add(DfValueFactory.newDoubleValue(d));
		}
		lists.put(DataType.DF_DOUBLE, Collections.unmodifiableList(list));

		VALUE_LIST = Collections.unmodifiableMap(lists);
		ENCODING_LIST = null;
	}

	@Test
	public void testGetDfConstant() {
		Assert.assertEquals(IDfValue.DF_BOOLEAN, DataType.DF_BOOLEAN.getDfConstant());
		Assert.assertEquals(IDfValue.DF_INTEGER, DataType.DF_INTEGER.getDfConstant());
		Assert.assertEquals(IDfValue.DF_STRING, DataType.DF_STRING.getDfConstant());
		Assert.assertEquals(IDfValue.DF_ID, DataType.DF_ID.getDfConstant());
		Assert.assertEquals(IDfValue.DF_TIME, DataType.DF_TIME.getDfConstant());
		Assert.assertEquals(IDfValue.DF_DOUBLE, DataType.DF_DOUBLE.getDfConstant());
		Assert.assertEquals(IDfValue.DF_UNDEFINED, DataType.DF_UNDEFINED.getDfConstant());
	}

	@Test
	public void testGetNullEncoding() {
		for (DataType type : DataTypeTest.NULL_ENCODING.keySet()) {
			Assert.assertEquals(DataTypeTest.NULL_ENCODING.get(type), type.getNullEncoding());
		}
		try {
			Assert.assertEquals(null, DataType.DF_UNDEFINED.getNullEncoding());
			Assert.fail("Undefined didn't fail an unsupported operation");
		} catch (UnsupportedOperationException e) {
			// All is well
		}
	}

	@Test
	public void testGetNullValue() {
		for (DataType type : DataTypeTest.NULL_VALUE.keySet()) {
			Assert.assertEquals(DataTypeTest.NULL_VALUE.get(type), type.getNullValue());
		}
		try {
			Assert.assertEquals(null, DataType.DF_UNDEFINED.getNullValue());
			Assert.fail("Undefined didn't fail an unsupported operation");
		} catch (UnsupportedOperationException e) {
			// All is well
		}
	}

	@Test
	public void testGetValue() {
		for (DataType type : DataTypeTest.NULL_VALUE.keySet()) {
			IDfValue n = DataTypeTest.NULL_VALUE.get(type);
			final Object N;
			switch (type) {
				case DF_BOOLEAN:
					N = n.asBoolean();
					break;
				case DF_INTEGER:
					N = n.asInteger();
					break;
				case DF_STRING:
					N = n.asString();
					break;
				case DF_ID:
					N = n.asId();
					break;
				case DF_TIME:
					N = n.asTime();
					break;
				case DF_DOUBLE:
					N = n.asDouble();
					break;
				default:
					Assert.fail(String.format("Unsupported type %s being tested", type));
					continue;
			}
			Assert.assertEquals(String.format("Comparing %s nulls", type), N, type.getValue(n));

			for (IDfValue v : DataTypeTest.VALUE_LIST.get(type)) {
				final Object expected;
				switch (type) {
					case DF_BOOLEAN:
						expected = v.asBoolean();
						break;
					case DF_INTEGER:
						expected = v.asInteger();
						break;
					case DF_STRING:
						expected = v.asString();
						break;
					case DF_ID:
						expected = v.asId();
						break;
					case DF_TIME:
						expected = v.asTime();
						break;
					case DF_DOUBLE:
						expected = v.asDouble();
						break;
					default:
						Assert.fail(String.format("Unsupported type %s being tested", type));
						continue;
				}
				Assert.assertEquals(String.format("Comparing %s values", type), expected, type.getValue(v));
			}
		}
	}

	@Test
	public void testFromDfConstant() {
		Assert.assertEquals(DataType.DF_BOOLEAN, DataType.fromDfConstant(IDfValue.DF_BOOLEAN));
		Assert.assertEquals(DataType.DF_INTEGER, DataType.fromDfConstant(IDfValue.DF_INTEGER));
		Assert.assertEquals(DataType.DF_STRING, DataType.fromDfConstant(IDfValue.DF_STRING));
		Assert.assertEquals(DataType.DF_ID, DataType.fromDfConstant(IDfValue.DF_ID));
		Assert.assertEquals(DataType.DF_TIME, DataType.fromDfConstant(IDfValue.DF_TIME));
		Assert.assertEquals(DataType.DF_DOUBLE, DataType.fromDfConstant(IDfValue.DF_DOUBLE));
		Assert.assertEquals(DataType.DF_UNDEFINED, DataType.fromDfConstant(IDfValue.DF_UNDEFINED));
		try {
			DataType.fromDfConstant(-1);
			Assert.fail("Did not fail with an illegal constant value");
		} catch (IllegalArgumentException e) {
			// All is well
		}
		try {
			DataType.fromDfConstant(7);
			Assert.fail("Did not fail with an illegal constant value");
		} catch (IllegalArgumentException e) {
			// All is well
		}
	}

	@Test
	public void testEncode() {
		for (DataType type : DataTypeTest.NULL_VALUE.keySet()) {
			Assert.assertEquals(DataTypeTest.NULL_ENCODING.get(type), type.encode(null));

			for (IDfValue v : DataTypeTest.VALUE_LIST.get(type)) {
				String encoding = type.encode(v);
				// TODO: replace this with a pre-calculated encoding
				IDfValue decoding = type.decode(encoding);
				Assert.assertEquals(String.format("Comparing %s encodings", type), type.getValue(v),
					type.getValue(decoding));
			}
		}
	}

	@Test
	public void testDecode() {
		for (DataType type : DataTypeTest.NULL_VALUE.keySet()) {
			Assert.assertEquals(DataTypeTest.NULL_ENCODING.get(type), type.encode(null));

			for (IDfValue v : DataTypeTest.VALUE_LIST.get(type)) {
				// TODO: replace this with a pre-calculated encoding
				String encoding = type.encode(v);
				IDfValue decoding = type.decode(encoding);
				Assert.assertEquals(String.format("Comparing %s encodings", type), type.getValue(v),
					type.getValue(decoding));
			}
		}
	}
}