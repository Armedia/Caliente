package com.armedia.caliente.engine.xml.importer.jaxb;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "attributeBase.t")
@XmlSeeAlso({
	AttributeT.class, AttributeDefT.class
})
public class AttributeBaseT implements Comparable<AttributeBaseT> {

	@XmlAttribute(name = "name", required = true)
	protected String name;

	@XmlAttribute(name = "dataType", required = true)
	protected DataTypeT dataType;

	public String getName() {
		return this.name;
	}

	public void setName(String value) {
		this.name = value;
	}

	public DataTypeT getDataType() {
		return this.dataType;
	}

	public void setDataType(DataTypeT value) {
		this.dataType = value;
	}

	@Override
	public int compareTo(AttributeBaseT o) {
		if (this == o) { return 0; }
		if (o == null) { return 1; }
		return Tools.compare(this.name, o.name);
	}

	@Override
	public String toString() {
		return String.format("AttributeBaseT [name=%s, dataType=%s]", this.name, this.dataType);
	}
}