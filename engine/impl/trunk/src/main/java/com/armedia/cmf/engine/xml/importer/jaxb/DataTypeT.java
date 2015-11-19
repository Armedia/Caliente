package com.armedia.cmf.engine.xml.importer.jaxb;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "dataType.t")
@XmlEnum
public enum DataTypeT {

	//
	BOOLEAN,
	INTEGER,
	DOUBLE,
	STRING,
	ID,
	DATETIME,
	URI,
	HTML,
	OTHER,
	//
	;

	public String value() {
		return name();
	}

	public static DataTypeT fromValue(String v) {
		return DataTypeT.valueOf(v);
	}
}