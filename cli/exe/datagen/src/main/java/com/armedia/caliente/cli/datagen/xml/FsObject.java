package com.armedia.caliente.cli.datagen.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "fs_object.t", propOrder = {
	"attributes"
})
public class FsObject {

	@XmlElementWrapper(name = "attributes", required = false)
	@XmlElement(name = "attribute", required = false)
	protected List<Attribute> attributes;

	@XmlAttribute(name = "type", required = false)
	protected String type;

	@XmlAttribute(name = "name", required = false)
	protected String name;

	public List<Attribute> getAttributes() {
		if (this.attributes == null) {
			this.attributes = new ArrayList<>();
		}
		return this.attributes;
	}

	public String getType() {
		return this.type;
	}

	public void setType(String value) {
		this.type = value;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String value) {
		this.name = value;
	}
}