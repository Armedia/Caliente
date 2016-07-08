package com.armedia.cmf.engine.alfresco.bulk.importer.jaxb;

import java.util.EnumMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

import com.armedia.cmf.storage.CmfDataType;

@XmlType(name = "dataType.t")
@XmlEnum
public enum DataTypeT {

	//
	BOOLEAN(CmfDataType.BOOLEAN),
	INTEGER(CmfDataType.INTEGER),
	DOUBLE(CmfDataType.DOUBLE),
	STRING(CmfDataType.STRING),
	ID(CmfDataType.ID),
	DATETIME(CmfDataType.DATETIME),
	URI(CmfDataType.URI),
	HTML(CmfDataType.HTML),
	OTHER(CmfDataType.OTHER),
	//
	;

	private final CmfDataType equiv;

	private DataTypeT(CmfDataType equiv) {
		this.equiv = equiv;
	}

	public String value() {
		return name();
	}

	public static DataTypeT fromValue(String v) {
		return DataTypeT.valueOf(v);
	}

	private static volatile Map<DataTypeT, CmfDataType> TO_CMF = null;
	private static volatile Map<CmfDataType, DataTypeT> FROM_CMF = null;

	private static void initMaps() {
		if (DataTypeT.TO_CMF == null) {
			synchronized (DataTypeT.class) {
				if (DataTypeT.TO_CMF == null) {
					DataTypeT.TO_CMF = new EnumMap<DataTypeT, CmfDataType>(DataTypeT.class);
					DataTypeT.FROM_CMF = new EnumMap<CmfDataType, DataTypeT>(CmfDataType.class);

					for (DataTypeT t : DataTypeT.values()) {
						DataTypeT.TO_CMF.put(t, t.equiv);
						DataTypeT t2 = DataTypeT.FROM_CMF.put(t.equiv, t);
						if (t2 != null) { throw new RuntimeException(String.format(
							"Duplicate mapping: DataTypeT.%s and DataTypeT.%s both map to CmfDataType.%s", t, t2,
							t.equiv)); }
					}
				}
			}
		}
	}

	public static DataTypeT convert(CmfDataType t) {
		DataTypeT.initMaps();
		return DataTypeT.FROM_CMF.get(t);
	}

	public static CmfDataType convert(DataTypeT t) {
		DataTypeT.initMaps();
		return DataTypeT.TO_CMF.get(t);
	}
}