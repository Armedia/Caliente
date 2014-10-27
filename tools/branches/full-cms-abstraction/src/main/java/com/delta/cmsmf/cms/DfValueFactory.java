package com.delta.cmsmf.cms;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.armedia.cmf.storage.StoredAttribute;
import com.documentum.fc.client.IDfTypedObject;
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
		DctmDataType type = DctmDataType.DF_BOOLEAN;
		return new DfValue(String.valueOf(v), type.getDfConstant());
	}

	public static IDfValue newIntValue(int v) {
		DctmDataType type = DctmDataType.DF_INTEGER;
		return new DfValue(String.valueOf(v), type.getDfConstant());
	}

	public static IDfValue newIntValue(long v) {
		DctmDataType type = DctmDataType.DF_INTEGER;
		return new DfValue(String.valueOf((int) v), type.getDfConstant());
	}

	public static IDfValue newStringValue(String v) {
		if (v == null) { return null; }
		DctmDataType type = DctmDataType.DF_STRING;
		return new DfValue(v, type.getDfConstant());
	}

	public static IDfValue newIdValue(IDfId v) {
		if (v == null) { return null; }
		DctmDataType type = DctmDataType.DF_ID;
		return new DfValue(v.toString(), type.getDfConstant());
	}

	public static IDfValue newIdValue(String v) {
		if (v == null) { return null; }
		DctmDataType type = DctmDataType.DF_ID;
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
		DctmDataType type = DctmDataType.DF_DOUBLE;
		return new DfValue(Double.toHexString(v), type.getDfConstant());
	}

	public static IDfValue newDoubleValue(double v) {
		DctmDataType type = DctmDataType.DF_DOUBLE;
		return new DfValue(Double.toHexString(v), type.getDfConstant());
	}

	public static List<IDfValue> getAllRepeatingValues(IDfAttr attr, IDfTypedObject object) throws DfException {
		return DfValueFactory.getAllRepeatingValues(attr.getName(), object);
	}

	public static List<IDfValue> getAllRepeatingValues(StoredAttribute<IDfValue> attr,
		IDfTypedObject object) throws DfException {
		return DfValueFactory.getAllRepeatingValues(attr.getName(), object);
	}

	public static List<IDfValue> getAllRepeatingValues(String attr, IDfTypedObject object) throws DfException {
		int valueCount = object.getValueCount(attr);
		List<IDfValue> ret = new ArrayList<IDfValue>(valueCount);
		for (int i = 0; i < valueCount; i++) {
			ret.add(object.getRepeatingValue(attr, i));
		}
		return ret;
	}
}