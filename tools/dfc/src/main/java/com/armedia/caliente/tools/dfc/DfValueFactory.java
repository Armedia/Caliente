package com.armedia.caliente.tools.dfc;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.armedia.commons.utilities.Tools;
import com.documentum.fc.client.IDfTypedObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;
import com.documentum.fc.common.DfTime;
import com.documentum.fc.common.DfValue;
import com.documentum.fc.common.IDfAttr;
import com.documentum.fc.common.IDfId;
import com.documentum.fc.common.IDfTime;
import com.documentum.fc.common.IDfValue;

public final class DfValueFactory {
	private DfValueFactory() {
	}

	public static IDfValue newValue(int type, Object v) {
		switch (type) {
			case IDfValue.DF_BOOLEAN:
				if (Boolean.class.isInstance(v)) { return DfValueFactory.newBooleanValue(Boolean.class.cast(v)); }
				break;

			case IDfValue.DF_DOUBLE:
				if (Number.class
					.isInstance(v)) { return DfValueFactory.newDoubleValue(Number.class.cast(v).doubleValue()); }
				break;

			case IDfValue.DF_INTEGER:
				if (Number.class.isInstance(v)) { return DfValueFactory.newIntValue(Number.class.cast(v).longValue()); }
				break;

			case IDfValue.DF_TIME:
				if (Date.class.isInstance(v)) { return DfValueFactory.newTimeValue(Date.class.cast(v)); }
				break;

			case IDfValue.DF_ID:
				if (IDfId.class.isInstance(v)) { return DfValueFactory.newIdValue(IDfId.class.cast(v)); }
				break;

			case IDfValue.DF_STRING:
				break;

			default:
				throw new RuntimeException(String.format("Unsupported type [%s] for value [%s]", type, v));
		}
		if (v == null) { return null; }
		return DfValueFactory.newStringValue(Tools.toString(v));
	}

	public static IDfValue newBooleanValue(boolean v) {
		return new DfValue(String.valueOf(v), IDfValue.DF_BOOLEAN);
	}

	public static IDfValue newIntValue(int v) {
		return new DfValue(String.valueOf(v), IDfValue.DF_INTEGER);
	}

	public static IDfValue newIntValue(long v) {
		return new DfValue(String.valueOf((int) v), IDfValue.DF_INTEGER);
	}

	public static IDfValue newStringValue(String v) {
		if (v == null) { return null; }
		return new DfValue(v, IDfValue.DF_STRING);
	}

	public static IDfValue newIdValue(IDfId v) {
		if (v == null) {
			v = DfId.DF_NULLID;
		}
		return new DfValue(v.toString(), IDfValue.DF_ID);
	}

	public static IDfValue newIdValue(String v) {
		if (v == null) {
			v = DfId.DF_NULLID_STR;
		}
		return new DfValue(v, IDfValue.DF_ID);
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
		return new DfValue(Double.toHexString(v), IDfValue.DF_DOUBLE);
	}

	public static IDfValue newDoubleValue(double v) {
		return new DfValue(Double.toHexString(v), IDfValue.DF_DOUBLE);
	}

	public static List<IDfValue> getAllRepeatingValues(IDfAttr attr, IDfTypedObject object) throws DfException {
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