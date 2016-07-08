package com.armedia.cmf.engine.alfresco.bulk.importer.jaxb;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "property.t", propOrder = {
	"value"
})
public class PropertyT extends AttributeBaseT {

	@XmlElement(name = "value", required = false)
	protected List<String> value;

	@XmlAttribute(name = "repeating", required = true)
	protected boolean repeating;

	public boolean isRepeating() {
		return this.repeating;
	}

	public void setRepeating(boolean repeating) {
		this.repeating = repeating;
	}

	public List<String> getValue() {
		if (this.value == null) {
			this.value = new ArrayList<String>();
		}
		return this.value;
	}

	@Override
	public String toString() {
		return String.format("AttributeT [name=%s, dataType=%s, repeating=%s value=%s]", this.name, this.dataType,
			this.repeating, this.value);
	}

}