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