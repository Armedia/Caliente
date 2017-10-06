package com.armedia.caliente.engine.xml.extmeta;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "parameterizedSqlParameter.t", propOrder = {
	"name", "value"
})
public class QueryParameter {

	@XmlElement(name = "name", required = true)
	protected String name;

	@XmlElement(name = "value", required = true)
	protected String value;

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getValue() {
		return this.value;
	}

	public void setValue(String value) {
		this.value = value;
	}

}