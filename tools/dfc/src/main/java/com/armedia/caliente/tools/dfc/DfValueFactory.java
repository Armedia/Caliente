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

	public static IDfValue of(int type, Object v) {
		switch (type) {
			case IDfValue.DF_BOOLEAN:
				if (Boolean.class.isInstance(v)) { return DfValueFactory.of(Boolean.class.cast(v)); }
				break;

			case IDfValue.DF_DOUBLE:
				if (Number.class.isInstance(v)) { return DfValueFactory.of(Number.class.cast(v).doubleValue()); }
				break;

			case IDfValue.DF_INTEGER:
				if (Number.class.isInstance(v)) { return DfValueFactory.of(Number.class.cast(v).longValue()); }
				break;

			case IDfValue.DF_TIME:
				if (Date.class.isInstance(v)) { return DfValueFactory.of(Date.class.cast(v)); }
				break;

			case IDfValue.DF_ID:
				if (IDfId.class.isInstance(v)) { return DfValueFactory.of(IDfId.class.cast(v)); }
				break;

			case IDfValue.DF_STRING:
				break;

			default:
				throw new RuntimeException(String.format("Unsupported type [%s] for value [%s]", type, v));
		}
		if (v == null) { return null; }
		return DfValueFactory.of(Tools.toString(v));
	}

	public static IDfValue of(boolean v) {
		return new DfValue(String.valueOf(v), IDfValue.DF_BOOLEAN);
	}

	public static IDfValue of(int v) {
		return new DfValue(String.valueOf(v), IDfValue.DF_INTEGER);
	}

	public static IDfValue of(long v) {
		return new DfValue(String.valueOf((int) v), IDfValue.DF_INTEGER);
	}

	public static IDfValue of(String v) {
		if (v == null) { return null; }
		return new DfValue(v, IDfValue.DF_STRING);
	}

	public static IDfValue of(IDfId v) {
		if (v == null) {
			v = DfId.DF_NULLID;
		}
		return new DfValue(v.toString(), IDfValue.DF_ID);
	}

	public static IDfValue ofId(String v) {
		if (v == null) {
			v = DfId.DF_NULLID_STR;
		}
		return new DfValue(v, IDfValue.DF_ID);
	}

	public static IDfValue of(IDfTime v) {
		if (v == null) { return null; }
		return new DfValue(v);
	}

	public static IDfValue ofTime(long v) {
		return new DfValue(new DfTime(new Date(v)));
	}

	public static IDfValue of(Date v) {
		return new DfValue(new DfTime(v));
	}

	public static IDfValue of(float v) {
		return new DfValue(Double.toHexString(v), IDfValue.DF_DOUBLE);
	}

	public static IDfValue of(double v) {
		return new DfValue(Double.toHexString(v), IDfValue.DF_DOUBLE);
	}

	public static List<IDfValue> getAllValues(IDfAttr attr, IDfTypedObject object) throws DfException {
		return DfValueFactory.getAllValues(attr.getName(), object);
	}

	public static List<IDfValue> getAllValues(String attr, IDfTypedObject object) throws DfException {
		int valueCount = object.getValueCount(attr);
		List<IDfValue> ret = new ArrayList<>(valueCount);
		for (int i = 0; i < valueCount; i++) {
			ret.add(object.getRepeatingValue(attr, i));
		}
		return ret;
	}
}