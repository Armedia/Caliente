package com.delta.cmsmf.datastore;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfTime;
import com.documentum.fc.common.DfValue;
import com.documentum.fc.common.IDfAttr;
import com.documentum.fc.common.IDfId;
import com.documentum.fc.common.IDfTime;
import com.documentum.fc.common.IDfValue;

public final class DfValueFactory {
	private DfValueFactory() {
	}

	public static IDfValue newBooleanValue(boolean v) {
		DataType type = DataType.DF_BOOLEAN;
		return new DfValue(String.valueOf(v), type.getDfConstant());
	}

	public static IDfValue newIntValue(int v) {
		DataType type = DataType.DF_INTEGER;
		return new DfValue(String.valueOf(v), type.getDfConstant());
	}

	public static IDfValue newIntValue(long v) {
		DataType type = DataType.DF_INTEGER;
		return new DfValue(String.valueOf((int) v), type.getDfConstant());
	}

	public static IDfValue newStringValue(String v) {
		if (v == null) { return null; }
		DataType type = DataType.DF_STRING;
		return new DfValue(v, type.getDfConstant());
	}

	public static IDfValue newIdValue(IDfId v) {
		if (v == null) { return null; }
		DataType type = DataType.DF_ID;
		return new DfValue(v.toString(), type.getDfConstant());
	}

	public static IDfValue newIdValue(String v) {
		if (v == null) { return null; }
		DataType type = DataType.DF_ID;
		return new DfValue(v, type.getDfConstant());
	}

	public static IDfValue newTimeValue(IDfTime v) {
		if (v == null) { return null; }
		return new DfValue(v);
	}

	public static IDfValue newTimeValue(long v) {
		return new DfValue(new DfTime(new Date(v)));
	}

	public static IDfValue newTimeValue(Date v) {
		return new DfValue(new DfTime(v));
	}

	public static IDfValue newDoubleValue(float v) {
		DataType type = DataType.DF_DOUBLE;
		return new DfValue(Double.toHexString(v), type.getDfConstant());
	}

	public static IDfValue newDoubleValue(double v) {
		DataType type = DataType.DF_DOUBLE;
		return new DfValue(Double.toHexString(v), type.getDfConstant());
	}

	public static List<IDfValue> getAllRepeatingValues(IDfAttr attr, IDfPersistentObject object) throws DfException {
		return DfValueFactory.getAllRepeatingValues(attr.getName(), object);
	}

	public static List<IDfValue> getAllRepeatingValues(DataAttribute attr, IDfPersistentObject object)
		throws DfException {
		return DfValueFactory.getAllRepeatingValues(attr.getName(), object);
	}

	public static List<IDfValue> getAllRepeatingValues(String attr, IDfPersistentObject object) throws DfException {
		int valueCount = object.getValueCount(attr);
		List<IDfValue> ret = new ArrayList<IDfValue>(valueCount);
		for (int i = 0; i < valueCount; i++) {
			ret.add(object.getRepeatingValue(attr, i));
		}
		return ret;
	}
}