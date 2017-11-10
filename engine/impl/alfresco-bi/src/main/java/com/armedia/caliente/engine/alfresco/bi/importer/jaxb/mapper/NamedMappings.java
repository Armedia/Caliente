package com.armedia.caliente.engine.alfresco.bi.importer.jaxb.mapper;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "namedMappings.t")
public class NamedMappings extends MappingSet {

	@XmlAttribute(name = "name", required = true)
	protected String name;

	public String getName() {
		return this.name;
	}

	public void setName(String value) {
		this.name = value;
	}

}