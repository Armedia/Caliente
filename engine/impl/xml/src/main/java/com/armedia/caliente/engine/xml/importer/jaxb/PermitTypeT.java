package com.armedia.caliente.engine.xml.importer.jaxb;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "permitType.t")
@XmlEnum
public enum PermitTypeT {

	//
	ACCESS,
	EXTENDED,
	APPLICATION,
	ACCESS_RESTRICTION,
	EXTENDED_RESTRICTION,
	APPLICATION_RESTRICTION,
	REQUIRED_GROUP,
	REQUIRED_GROUP_SET
	//
	;

	public String value() {
		return name();
	}

	public static PermitTypeT fromValue(String v) {
		return PermitTypeT.valueOf(v);
	}

}
