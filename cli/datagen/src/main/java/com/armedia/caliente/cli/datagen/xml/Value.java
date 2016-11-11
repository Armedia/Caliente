package com.armedia.caliente.cli.datagen.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "value.t", propOrder = {
	"value"
})
public class Value {

	@XmlValue
	protected String value;

	public String getValue() {
		return this.value;
	}

	public void setValue(String value) {
		this.value = value;
	}
}