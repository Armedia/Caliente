package com.delta.cmsmf.datastore.cms;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
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

import com.documentum.fc.client.impl.typeddata.Attribute;
import com.documentum.fc.common.DfId;
import com.documentum.fc.common.DfTime;
import com.documentum.fc.common.IDfValue;

public class CmsDataTypeTest {

	private static final Map<CmsDataType, String> NULL_ENCODING;
	private static final Map<CmsDataType, IDfValue> NULL_VALUE;
	private static final Map<CmsDataType, List<IDfValue>> VALUE_LIST;
	private static final Map<CmsDataType, List<String>> ENCODING_LIST;

	static {
		Map<CmsDataType, String> nullEncodings = new EnumMap<CmsDataType, String>(CmsDataType.class);
		nullEncodings.put(CmsDataType.DF_BOOLEAN, "false");
		nullEncodings.put(CmsDataType.DF_INTEGER, "0");
		nullEncodings.put(CmsDataType.DF_STRING, "");
		nullEncodings.put(CmsDataType.DF_ID, DfId.DF_NULLID_STR);
		nullEncodings.put(CmsDataType.DF_TIME, DfTime.DF_NULLDATE_STR);
		nullEncodings.put(CmsDataType.DF_DOUBLE, "0x0.0p0");
		NULL_ENCODING = Collections.unmodifiableMap(nullEncodings);

		Map<CmsDataType, IDfValue> nullValues = new EnumMap<CmsDataType, IDfValue>(CmsDataType.class);
		nullValues.put(CmsDataType.DF_BOOLEAN, DfValueFactory.newBooleanValue(false));
		nullValues.put(CmsDataType.DF_INTEGER, DfValueFactory.newIntValue(0));
		nullValues.put(CmsDataType.DF_STRING, DfValueFactory.newStringValue(""));
		nullValues.put(CmsDataType.DF_ID, DfValueFactory.newIdValue(DfId.DF_NULLID));
		nullValues.put(CmsDataType.DF_TIME, DfValueFactory.newTimeValue(DfTime.DF_NULLDATE));
		nullValues.put(CmsDataType.DF_DOUBLE, DfValueFactory.newDoubleValue(0.0));
		NULL_VALUE = Collections.unmodifiableMap(nullValues);

		Map<CmsDataType, List<IDfValue>> valueLists = new EnumMap<CmsDataType, List<IDfValue>>(CmsDataType.class);
		Map<CmsDataType, List<String>> encodingLists = new EnumMap<CmsDataType, List<String>>(CmsDataType.class);

		List<IDfValue> values = null;
		List<String> encodings = null;

		values = Arrays.asList(DfValueFactory.newBooleanValue(false), DfValueFactory.newBooleanValue(true));
		valueLists.put(CmsDataType.DF_BOOLEAN, Collections.unmodifiableList(values));
		encodings = Arrays.asList("false", "true");
		encodingLists.put(CmsDataType.DF_BOOLEAN, Collections.unmodifiableList(encodings));

		values = new ArrayList<IDfValue>();
		encodings = new ArrayList<String>();
		for (int i = -100; i < 100; i++) {
			values.add(DfValueFactory.newIntValue(i));
			encodings.add(String.valueOf(i));
			int I = (i < 0 ? Integer.MIN_VALUE : Integer.MAX_VALUE) - i;
			values.add(DfValueFactory.newIntValue(I));
			encodings.add(String.valueOf(I));
		}
		valueLists.put(CmsDataType.DF_INTEGER, Collections.unmodifiableList(values));
		encodingLists.put(CmsDataType.DF_INTEGER, Collections.unmodifiableList(encodings));

		values = new ArrayList<IDfValue>();
		encodings = new ArrayList<String>();
		for (int i = 0; i < 100; i++) {
			String s = UUID.randomUUID().toString();
			values.add(DfValueFactory.newStringValue(s));
			encodings.add(s);
		}
		valueLists.put(CmsDataType.DF_STRING, Collections.unmodifiableList(values));
		encodingLists.put(CmsDataType.DF_STRING, Collections.unmodifiableList(encodings));

		values = new ArrayList<IDfValue>();
		encodings = new ArrayList<String>();
		for (int i = -100; i < 100; i++) {
			String s = String.format("%016x", i);
			values.add(DfValueFactory.newIdValue(s));
			encodings.add(s);
			long L = (i < 0 ? Long.MIN_VALUE : Long.MAX_VALUE) - i;
			s = String.format("%016x", L);
			values.add(DfValueFactory.newIdValue(s));
			encodings.add(s);
		}
		valueLists.put(CmsDataType.DF_ID, Collections.unmodifiableList(values));
		encodingLists.put(CmsDataType.DF_ID, Collections.unmodifiableList(encodings));

		values = new ArrayList<IDfValue>();
		encodings = new ArrayList<String>();
		DateFormat fmt = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		for (int i = 0; i < 100; i++) {
			Date d = new Date(i);
			values.add(DfValueFactory.newTimeValue(d));
			encodings.add(fmt.format(d));
			d = new Date(Long.MAX_VALUE - i);
			values.add(DfValueFactory.newTimeValue(d));
			encodings.add(fmt.format(d));
		}
		valueLists.put(CmsDataType.DF_TIME, Collections.unmodifiableList(values));
		encodingLists.put(CmsDataType.DF_TIME, Collections.unmodifiableList(encodings));

		values = new ArrayList<IDfValue>();
		encodings = new ArrayList<String>();
		double[] DOUBLES = {
			0.0, 1.0, 0.1, Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.MIN_VALUE,
			Double.MAX_VALUE
		};
		for (double d : DOUBLES) {
			values.add(DfValueFactory.newDoubleValue(d));
			encodings.add(Double.toHexString(d));
		}
		valueLists.put(CmsDataType.DF_DOUBLE, Collections.unmodifiableList(values));
		encodingLists.put(CmsDataType.DF_DOUBLE, Collections.unmodifiableList(encodings));

		VALUE_LIST = Collections.unmodifiableMap(valueLists);
		ENCODING_LIST = Collections.unmodifiableMap(encodingLists);
	}

	@Test
	public void testGetDfConstant() {
		Assert.assertEquals(IDfValue.DF_BOOLEAN, CmsDataType.DF_BOOLEAN.getDfConstant());
		Assert.assertEquals(IDfValue.DF_INTEGER, CmsDataType.DF_INTEGER.getDfConstant());
		Assert.assertEquals(IDfValue.DF_STRING, CmsDataType.DF_STRING.getDfConstant());
		Assert.assertEquals(IDfValue.DF_ID, CmsDataType.DF_ID.getDfConstant());
		Assert.assertEquals(IDfValue.DF_TIME, CmsDataType.DF_TIME.getDfConstant());
		Assert.assertEquals(IDfValue.DF_DOUBLE, CmsDataType.DF_DOUBLE.getDfConstant());
		Assert.assertEquals(IDfValue.DF_UNDEFINED, CmsDataType.DF_UNDEFINED.getDfConstant());
	}

	@Test
	public void testGetNullEncoding() {
		for (CmsDataType type : CmsDataTypeTest.NULL_ENCODING.keySet()) {
			Assert.assertEquals(CmsDataTypeTest.NULL_ENCODING.get(type), type.getNullEncoding());
		}
		try {
			Assert.assertEquals(null, CmsDataType.DF_UNDEFINED.getNullEncoding());
			Assert.fail("Undefined didn't fail an unsupported operation");
		} catch (UnsupportedOperationException e) {
			// All is well
		}
	}

	@Test
	public void testGetNullValue() {
		for (CmsDataType type : CmsDataTypeTest.NULL_VALUE.keySet()) {
			Assert.assertEquals(CmsDataTypeTest.NULL_VALUE.get(type), type.getNullValue());
		}
		try {
			Assert.assertEquals(null, CmsDataType.DF_UNDEFINED.getNullValue());
			Assert.fail("Undefined didn't fail an unsupported operation");
		} catch (UnsupportedOperationException e) {
			// All is well
		}
	}

	@Test
	public void testGetValue() {
		for (CmsDataType type : CmsDataTypeTest.NULL_VALUE.keySet()) {
			IDfValue n = CmsDataTypeTest.NULL_VALUE.get(type);
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
			Assert.assertEquals(String.format("Comparing %s null values", type), N, type.getValue(n));
			Assert.assertEquals(String.format("Comparing %s null", type), N, type.getValue(null));

			for (IDfValue v : CmsDataTypeTest.VALUE_LIST.get(type)) {
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
	public void testFromAttribute() {
		for (CmsDataType a : CmsDataType.values()) {
			Attribute A = new Attribute(a.name(), false, a.getDfConstant());
			CmsDataType tA = CmsDataType.fromAttribute(A);
			Assert.assertNotNull(tA);
			Assert.assertEquals(a, tA);
			for (CmsDataType b : CmsDataType.values()) {
				Attribute B = new Attribute(a.name(), false, b.getDfConstant());
				CmsDataType tB = CmsDataType.fromAttribute(B);
				Assert.assertNotNull(tB);
				if (a == b) {
					// Make sure there are no collisions
					Assert.assertEquals(a, tB);
				} else {
					Assert.assertEquals(b, tB);
				}
			}
		}
		try {
			CmsDataType.fromAttribute(null);
			Assert.fail("Did not fail with a null attribute");
		} catch (IllegalArgumentException e) {
			// All is well
		}
	}

	@Test
	public void testEncode() {
		for (CmsDataType type : CmsDataTypeTest.NULL_VALUE.keySet()) {
			Assert.assertEquals(CmsDataTypeTest.NULL_ENCODING.get(type), type.encode(null));
			List<IDfValue> values = CmsDataTypeTest.VALUE_LIST.get(type);
			List<String> encodings = CmsDataTypeTest.ENCODING_LIST.get(type);
			for (int i = 0; i < values.size(); i++) {
				final IDfValue v = values.get(i);
				final String e = encodings.get(i);
				Assert.assertEquals(String.format("Comparing %s encodings", type), e, type.encode(v));
			}
		}
	}

	@Test
	public void testDecode() {
		for (CmsDataType type : CmsDataTypeTest.NULL_VALUE.keySet()) {
			Assert.assertEquals(CmsDataTypeTest.NULL_ENCODING.get(type),
				type.encode(CmsDataTypeTest.NULL_VALUE.get(type)));
			Assert.assertEquals(CmsDataTypeTest.NULL_ENCODING.get(type), type.encode(null));
			List<IDfValue> values = CmsDataTypeTest.VALUE_LIST.get(type);
			List<String> encodings = CmsDataTypeTest.ENCODING_LIST.get(type);
			for (int i = 0; i < values.size(); i++) {
				final IDfValue v = values.get(i);
				final String e = encodings.get(i);
				Assert.assertEquals(String.format("Comparing %s decodings", type), type.getValue(v),
					type.getValue(type.decode(e)));
			}
		}
	}

	@Test
	public void testEncodeDecode() {
		for (CmsDataType type : CmsDataTypeTest.NULL_VALUE.keySet()) {
			Assert.assertEquals(type.getValue(CmsDataTypeTest.NULL_VALUE.get(type)),
				type.getValue(type.decode(CmsDataTypeTest.NULL_ENCODING.get(type))));
			Assert.assertEquals(type.getValue(CmsDataTypeTest.NULL_VALUE.get(type)), type.getValue(type.decode(null)));
			List<IDfValue> values = CmsDataTypeTest.VALUE_LIST.get(type);
			List<String> encodings = CmsDataTypeTest.ENCODING_LIST.get(type);
			for (int i = 0; i < values.size(); i++) {
				final IDfValue v = values.get(i);
				final IDfValue V = type.decode(type.encode(v));
				Assert.assertEquals(String.format("Comparing %s looped encoding", type), type.getValue(v),
					type.getValue(V));

				final String e = encodings.get(i);
				final String E = type.encode(type.decode(e));
				Assert.assertEquals(String.format("Comparing %s looped decoding", type), e, E);
			}
		}
	}

	@Test
	public void testEncodeDF_UNDEFINED() {
		final CmsDataType type = CmsDataType.DF_UNDEFINED;
		final IDfValue NULL = CmsDataTypeTest.NULL_VALUE.get(CmsDataType.DF_STRING);
		try {
			type.encode(null);
			Assert.fail("DF_UNDEFINED did not fail when encoding null");
		} catch (UnsupportedOperationException e) {
			// All is well
		}
		try {
			type.encode(NULL);
			Assert.fail("DF_UNDEFINED did not fail when encoding a non-null");
		} catch (UnsupportedOperationException e) {
			// All is well
		}
		try {
			type.encode(null);
			Assert.fail("DF_UNDEFINED did not fail when encoding null");
		} catch (UnsupportedOperationException e) {
			// All is well
		}
		try {
			type.getNullEncoding();
			Assert.fail("DF_UNDEFINED did not fail when retrieving the null encoding");
		} catch (UnsupportedOperationException e) {
			// All is well
		}
		try {
			type.getNullValue();
			Assert.fail("DF_UNDEFINED did not fail when retrieving the null value");
		} catch (UnsupportedOperationException e) {
			// All is well
		}
		try {
			type.getValue(null);
			Assert.fail("DF_UNDEFINED did not fail when calling getValue with null");
		} catch (UnsupportedOperationException e) {
			// All is well
		}
		try {
			type.getValue(NULL);
			Assert.fail("DF_UNDEFINED did not fail when calling getValue with non-null");
		} catch (UnsupportedOperationException e) {
			// All is well
		}
	}
}