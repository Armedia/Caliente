/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
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
package com.armedia.caliente.engine.xml.importer.jaxb;

import java.util.EnumMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.Tools;

@XmlType(name = "dataType.t")
@XmlEnum
public enum DataTypeT {

	//
	BOOLEAN(CmfValue.Type.BOOLEAN),
	INTEGER(CmfValue.Type.INTEGER),
	DOUBLE(CmfValue.Type.DOUBLE),
	STRING(CmfValue.Type.STRING),
	ID(CmfValue.Type.ID),
	DATETIME(CmfValue.Type.DATETIME),
	URI(CmfValue.Type.URI),
	HTML(CmfValue.Type.HTML),
	OTHER(CmfValue.Type.OTHER),
	//
	;

	private final CmfValue.Type equiv;

	private DataTypeT(CmfValue.Type equiv) {
		this.equiv = equiv;
	}

	public String value() {
		return name();
	}

	private static final Map<DataTypeT, CmfValue.Type> TO_CMF;
	private static final Map<CmfValue.Type, DataTypeT> FROM_CMF;

	static {
		Map<DataTypeT, CmfValue.Type> toCmf = new EnumMap<>(DataTypeT.class);
		Map<CmfValue.Type, DataTypeT> fromCmf = new EnumMap<>(CmfValue.Type.class);

		for (DataTypeT t : DataTypeT.values()) {
			toCmf.put(t, t.equiv);
			DataTypeT t2 = fromCmf.put(t.equiv, t);
			if (t2 != null) {
				throw new RuntimeException(String.format(
					"Duplicate mapping: DataTypeT.%s and DataTypeT.%s both map to CmfValue.Type.%s", t, t2, t.equiv));
			}
		}
		TO_CMF = Tools.freezeMap(toCmf);
		FROM_CMF = Tools.freezeMap(fromCmf);
	}

	public static DataTypeT fromValue(String v) {
		return DataTypeT.valueOf(v);
	}

	public static DataTypeT convert(CmfValue.Type t) {
		return DataTypeT.FROM_CMF.get(t);
	}

	public static CmfValue.Type convert(DataTypeT t) {
		return DataTypeT.TO_CMF.get(t);
	}
}