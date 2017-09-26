
package com.armedia.caliente.engine.transform;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "cardinality.t")
@XmlEnum
public enum CardinalityT {

	@XmlEnumValue("first")
	FIRST("first"), //
	@XmlEnumValue("any")
	ANY("any"), //
	@XmlEnumValue("all")
	ALL("all"), //
	@XmlEnumValue("last")
	LAST("last"), //
	//
	;
	private final String value;

	CardinalityT(String v) {
		this.value = v;
	}

	public String value() {
		return this.value;
	}

	public static CardinalityT fromValue(String v) {
		for (CardinalityT c : CardinalityT.values()) {
			if (c.value.equals(v)) { return c; }
		}
		throw new IllegalArgumentException(v);
	}

}