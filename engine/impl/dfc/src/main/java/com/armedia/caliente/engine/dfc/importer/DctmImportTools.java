package com.armedia.caliente.engine.dfc.importer;

import com.armedia.caliente.store.CmfProperty;
import com.documentum.fc.common.IDfValue;

public class DctmImportTools {
	public static String concatenateStrings(CmfProperty<IDfValue> p, char sep) {
		if (p == null) { return null; }
		if (p.getValueCount() < 1) { return ""; }
		if (!p.isMultivalued() || (p.getValueCount() == 1)) { return p.getValue().asString(); }
		StringBuilder sb = new StringBuilder();
		for (IDfValue v : p) {
			if (sb.length() > 0) {
				sb.append(',');
			}
			sb.append(v.asString());
		}
		return sb.toString();
	}
}