package com.armedia.cmf.engine.xml.importer.jaxb;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "typeDef.t", propOrder = {
	"name", "superType", "attributes"
})
public class TypeDefT {

	@XmlElement(name = "name", required = true)
	protected String name;

	@XmlElement(name = "superType", required = false)
	protected String superType;

	@XmlElementWrapper(name = "attributes", required = false)
	@XmlElement(name = "attribute", required = false)
	protected List<AttributeDefT> attributes;

	public List<AttributeDefT> getAttributes() {
		if (this.attributes == null) {
			this.attributes = new ArrayList<AttributeDefT>();
		}
		return this.attributes;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String value) {
		this.name = value;
	}

	public String getSuperType() {
		return this.superType;
	}

	public void setSuperType(String value) {
		this.superType = value;
	}

	@Override
	public String toString() {
		return String.format("TypeDefT [name=%s, superType=%s, attributes=%s]", this.name, this.superType,
			this.attributes);
	}
}