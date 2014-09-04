package com.delta.cmsmf.datastore;

import java.util.Date;

import com.documentum.fc.common.DfTime;
import com.documentum.fc.common.DfValue;
import com.documentum.fc.common.IDfId;
import com.documentum.fc.common.IDfTime;

public final class DfValueFactory {
	private DfValueFactory() {
	}

	public static DfValue newBooleanValue(boolean v) {
		DataType type = DataType.DF_BOOLEAN;
		return new DfValue(String.valueOf(v), type.getDfConstant());
	}

	public static DfValue newIntValue(int v) {
		DataType type = DataType.DF_INTEGER;
		return new DfValue(String.valueOf(v), type.getDfConstant());
	}

	public static DfValue newIntValue(long v) {
		DataType type = DataType.DF_INTEGER;
		return new DfValue(String.valueOf((int) v), type.getDfConstant());
	}

	public static DfValue newStringValue(String v) {
		if (v == null) { return null; }
		DataType type = DataType.DF_STRING;
		return new DfValue(v, type.getDfConstant());
	}

	public static DfValue newIdValue(IDfId v) {
		if (v == null) { return null; }
		DataType type = DataType.DF_ID;
		return new DfValue(v.toString(), type.getDfConstant());
	}

	public static DfValue newIdValue(String v) {
		if (v == null) { return null; }
		DataType type = DataType.DF_ID;
		return new DfValue(v, type.getDfConstant());
	}

	public static DfValue newTimeValue(IDfTime v) {
		if (v == null) { return null; }
		return new DfValue(v);
	}

	public static DfValue newTimeValue(long v) {
		return new DfValue(new DfTime(new Date(v)));
	}

	public static DfValue newTimeValue(Date v) {
		return new DfValue(new DfTime(v));
	}

	public static DfValue newDoubleValue(float v) {
		DataType type = DataType.DF_DOUBLE;
		return new DfValue(Double.toHexString(v), type.getDfConstant());
	}

	public static DfValue newDoubleValue(double v) {
		DataType type = DataType.DF_DOUBLE;
		return new DfValue(Double.toHexString(v), type.getDfConstant());
	}
}