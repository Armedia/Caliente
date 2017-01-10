package com.armedia.caliente.engine.alfresco.bulk.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "entry.t", propOrder = {
	"value"
})
@XmlRootElement(name = "entry")
public class PropertiesEntry {

	@XmlAttribute(name = "key", required = true)
	protected String key;

	@XmlValue
	protected String value;

	public String getKey() {
		return this.key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getValue() {
		return this.value;
	}

	public void setValue(String value) {
		this.value = value;
	}
}