package com.armedia.caliente.engine.xml.importer.jaxb;

import java.util.EnumMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.store.CmfValueType;

@XmlType(name = "dataType.t")
@XmlEnum
public enum DataTypeT {

	//
	BOOLEAN(CmfValueType.BOOLEAN),
	INTEGER(CmfValueType.INTEGER),
	DOUBLE(CmfValueType.DOUBLE),
	STRING(CmfValueType.STRING),
	ID(CmfValueType.ID),
	DATETIME(CmfValueType.DATETIME),
	URI(CmfValueType.URI),
	HTML(CmfValueType.HTML),
	OTHER(CmfValueType.OTHER),
	//
	;

	private final CmfValueType equiv;

	private DataTypeT(CmfValueType equiv) {
		this.equiv = equiv;
	}

	public String value() {
		return name();
	}

	public static DataTypeT fromValue(String v) {
		return DataTypeT.valueOf(v);
	}

	private static volatile Map<DataTypeT, CmfValueType> TO_CMF = null;
	private static volatile Map<CmfValueType, DataTypeT> FROM_CMF = null;

	private static void initMaps() {
		if (DataTypeT.TO_CMF == null) {
			synchronized (DataTypeT.class) {
				if (DataTypeT.TO_CMF == null) {
					DataTypeT.TO_CMF = new EnumMap<>(DataTypeT.class);
					DataTypeT.FROM_CMF = new EnumMap<>(CmfValueType.class);

					for (DataTypeT t : DataTypeT.values()) {
						DataTypeT.TO_CMF.put(t, t.equiv);
						DataTypeT t2 = DataTypeT.FROM_CMF.put(t.equiv, t);
						if (t2 != null) { throw new RuntimeException(
							String.format("Duplicate mapping: DataTypeT.%s and DataTypeT.%s both map to CmfDataType.%s",
								t, t2, t.equiv)); }
					}
				}
			}
		}
	}

	public static DataTypeT convert(CmfValueType t) {
		DataTypeT.initMaps();
		return DataTypeT.FROM_CMF.get(t);
	}

	public static CmfValueType convert(DataTypeT t) {
		DataTypeT.initMaps();
		return DataTypeT.TO_CMF.get(t);
	}
}