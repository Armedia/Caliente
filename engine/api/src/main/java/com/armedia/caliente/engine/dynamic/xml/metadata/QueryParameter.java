package com.armedia.caliente.engine.dynamic.xml.metadata;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.dynamic.xml.Expression;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "parameterizedSQLParameter.t", propOrder = {
	"name", "value"
})
public class QueryParameter {

	@XmlElement(name = "name", required = true)
	protected String name;

	@XmlElement(name = "value", required = true)
	protected Expression value;

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Expression getValue() {
		return this.value;
	}

	public void setValue(Expression value) {
		this.value = value;
	}

}