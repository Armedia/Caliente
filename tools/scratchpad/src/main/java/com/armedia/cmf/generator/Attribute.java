package com.armedia.cmf.generator;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "attribute.t", propOrder = {
	"value"
})
public class Attribute {

	@XmlElement(name = "value", required = false)
	protected List<Value> value;

	@XmlAttribute(name = "name", required = true)
	protected String name;

	public List<Value> getValue() {
		if (this.value == null) {
			this.value = new ArrayList<>();
		}
		return this.value;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String value) {
		this.name = value;
	}
}