package com.armedia.cmf.engine.alfresco.bulk.importer.jaxb;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "attribute.t", propOrder = {
	"value"
})
public class AttributeT extends AttributeBaseT {

	@XmlElement(name = "value", required = false)
	protected List<String> value;

	public List<String> getValue() {
		if (this.value == null) {
			this.value = new ArrayList<String>();
		}
		return this.value;
	}

	@Override
	public String toString() {
		return String.format("AttributeT [name=%s, dataType=%s, value=%s]", this.name, this.dataType, this.value);
	}

}