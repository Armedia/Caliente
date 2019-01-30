package com.armedia.caliente.engine.xml.importer.jaxb;

import java.util.EnumMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.store.CmfValue;

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

	public static DataTypeT fromValue(String v) {
		return DataTypeT.valueOf(v);
	}

	private static volatile Map<DataTypeT, CmfValue.Type> TO_CMF = null;
	private static volatile Map<CmfValue.Type, DataTypeT> FROM_CMF = null;

	private static void initMaps() {
		if (DataTypeT.TO_CMF == null) {
			synchronized (DataTypeT.class) {
				if (DataTypeT.TO_CMF == null) {
					DataTypeT.TO_CMF = new EnumMap<>(DataTypeT.class);
					DataTypeT.FROM_CMF = new EnumMap<>(CmfValue.Type.class);

					for (DataTypeT t : DataTypeT.values()) {
						DataTypeT.TO_CMF.put(t, t.equiv);
						DataTypeT t2 = DataTypeT.FROM_CMF.put(t.equiv, t);
						if (t2 != null) {
							throw new RuntimeException(String.format(
								"Duplicate mapping: DataTypeT.%s and DataTypeT.%s both map to CmfValue.Type.%s", t, t2,
								t.equiv));
						}
					}
				}
			}
		}
	}

	public static DataTypeT convert(CmfValue.Type t) {
		DataTypeT.initMaps();
		return DataTypeT.FROM_CMF.get(t);
	}

	public static CmfValue.Type convert(DataTypeT t) {
		DataTypeT.initMaps();
		return DataTypeT.TO_CMF.get(t);
	}
}